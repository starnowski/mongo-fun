/// createIndexes
use("sample_mflix");
const indexName = "plot_title_idx";
const indexDefinition = {
  mappings: {
    dynamic: false,
    fields: {
      plot: { type: "string" },
      title: { type: "string" },
    },
  },
};
print(db.movies.createSearchIndex(indexName, indexDefinition));





load("/lab/createIndex.js")
db.movies.getSearchIndexes()
db.movies.aggregate([
  {
    $search: {
      index: "plot_title_idx",
      text: {
        query: "The Lion King",
        path: "title"
      }
    }
  },
  {
    $project: {
      title: 1,
      year: 1,
      plot: 1,
    }
  },
  {
    $limit: 1
  }
])


//// Lab 1 - 2



print(db.movies.createSearchIndex("movies_dynamic",  {
  mappings: {
    dynamic: true
  },
}));


db.movies.getSearchIndexes();

/// Both indexes

use("sample_mflix");

const indexName = "title_tomatoes_index";
const indexDefinition = {
  mappings: {
    // TODO: Set dynamic to false and add fields object with title (type "string") and tomatoes (dynamic: true, type: "document")
    dynamic: false,
    fields: {
        title: {
            type: "string"
        },
        tomatoes : {
            dynamic: true,
            type: "document"
        }

    },
  },
};

const coll = db.getCollection("movies");
print(coll.createSearchIndex(indexName, indexDefinition));

db.movies.aggregate([
  {
    $search: {
      index: 'title_tomatoes_index',
      range: {
        path: 'tomatoes.viewer.rating',
        gte: 4,
      },
    },
  },
  {
    $project: {
      title: 1,
      'tomatoes.viewer.rating': 1,
      _id: 0,
    },
  },
  { $limit: 5 },
])


#### 3

If you want to perform a search query on a field that has an array of subdocuments, which data type should you use in your search index? (Select one.)

Correct Answer

a.
embeddedDocuments
Correct. 

The embeddedDocuments data type will let you index fields that contain an array of documents. This allows you to then query individual string fields inside the documents in the array.

https://www.mongodb.com/docs/atlas/atlas-search/operators-and-collectors/


###### Indexes that support multiple data types for field


### Index
{
    "name": "released_multi_type",
    "collectionName": "movies",
    "database": "sample_mflix",
    "definition": {
        "mappings": {
            "dynamic": false,
            "fields": {
                "released": [
                    {
                    "type": "date"
                },
                {
                    "type": "string"
                }
                ]
            }
        }
    }
}

### Query
const query = db.movies.aggregate([
    {
        $search: {
            index: "released_multi_type",
            compound: {
                should: [
                    { text: { query: "1999-03-31", path: "released" } },
                    { equals: { value: new Date("1999-03-31"), path: "released" } },
                ],
            },
        },
    },
    { $project: { _id: 0, title: 1, released: 1 } },
]);
printjson(query.toArray());



#### Operators examples:

Code Summary: $search operators: text and equals
Use the text Operator
The text operator performs a full-text search of data in our search index. In the following example, plotReleasedIndex is used with the text operator to search for documents that contain the word “nature” in the plot field.

db.movies.aggregate([
    {
      "$search": {
        "index": "plotReleasedIndex",
        "text": {
          "query": "nature",
          "path": "plot"
        }
      }
    },
   { "$project": {"_id": 0, "title": 1, "plot": 1 }}
  ])
Use the equals Operator
The equals operator returns documents with a field that matches a specified value. In the following example, plotReleasedIndex is used with the equals operator to search for movies that were released on March 31st, 1999.

db.movies.aggregate([
   {
      "$search": {
         "index": "plotReleasedIndex",
         "equals": {
            "path": "released",
            "value": ISODate("1999-03-31T00:00:00.000Z")
         }
      }
   },
   { "$project": {"_id": 0, "title": 1, "released": 1 }}
])

#### Near or range operators

Code Summary: $search operators: near and range
Use the near Operator
The near operator performs a search for dates, numbers, or geographic locations nearest to a given value or point. In the following example, the near operator is used with plotReleasedIndex to search for movies released around May 17, 1999.

db.movies.aggregate([
   {
      "$search": {
         "index": "plotReleasedIndex",
         "near": {
            "path": "released",
            "origin": ISODate("1999-05-17T00:00:00.000+00:00"),
            "pivot": 2629746000
         }
      }
   },
{ "$project": { "_id": 0, "title": 1, "released": 1, "score": { "$meta": "searchScore" }}}
])
Use the range Operator
The range operator performs a search based on a range of numbers or dates. The following example uses the range operator to return all documents with movies released between January 1st, 1994 and January 1st, 1999.

db.movies.aggregate([
  {
    "$search": {
      "index": "plotReleasedIndex",
      "range": {
        "path": "released",
        "gt": ISODate("1994-01-01T00:00:00.000Z"),
        "lt": ISODate("1999-01-01T00:00:00.000Z")
      }
    }
  },
  { "$project": { "_id": 0, "title": 1, "released": 1 }}
])

////
The near operator returns ___ documents in the collection, ordered by how close the document is to the given point. (Select one.)

Correct Answer

a.
all
Correct. 

The near operator returns all documents in the collection ordered by how close the document is to the given point.


If a returned document has a score of 0.3 when using the near search operator, what is its distance from the origin? (Select one.)

Correct Answer

a.
The document is beyond the given pivot.
Correct. 

The document is beyond the pivot. The pivot is the distance from the origin we are searching for using the near operator.

b.
The document is an exact match to the pivot.
Incorrect. 

The document would have a score of 0.5 if it was an exact match to the pivot. The pivot is the distance from the origin we are searching for using the near operator.

c.
The document is an exact match to the origin.
Incorrect. 

The document would have a score of 1 if it was an exact match to the origin. The origin is the starting point of our query.



#### Lab 

30 days in milliseconds is equal to 2592000000.


const searchStage = {
    $search: {
        "index": "released_index",
         "near": {
            "path": "released",
            "origin": new Date("1984-06-08"),
            "pivot": 2592000000
         }
    }
};

const projectStage = {
    $project: {
        _id: 0,
        title: 1,
        released: 1,
        score: { $meta: "searchScore" },
    },
};

const limitStage = { $limit: 10 };

Use the near Operator to Find Movies
In this lab, you'll use the near operator to search for movies in the movies collection that were released around a particular date.

Important
An Atlas Search index named released_index has been created for you.

Note
Please note that this lab is using a Local Atlas Deployment. Visit our documentation page to learn more!

Lab Instructions
In the editor tab, open the query.js file, which contains a query to search for movies released near a specific date. Update the searchStage variable by doing the following:

Specify the index to use for this query, which is released_index

Use the near operator to find movies released within 30 days of the target date, 1984-06-08



In this lab, you will use the range operator to search for movies released within a specific date range.


Fine-Tune Search Results with the range Operator
In this lab, you will use the range operator to search for movies released during a specific date range in our movies collection.

For this lab, you will continue to use the released_index, a static index on the released field with a type of date.



const searchStage = {
    $search: {
        "index": "released_index",
		 "range": {
			"path": "released",
			"gt": new Date("1987-01-01"),
			"lt": new Date("1990-01-01")
		}
    }
};

const projectStage = {
    $project: {
        _id: 0,
        title: 1,
        released: 1,
        score: { $meta: "searchScore" },
    },
};

const limitStage = { $limit: 10 };


#### Facets

Code Summary: Creating Search Facets
Define a Search Index for Facets
The following example creates a search index named genresFacetedIndex, which uses the stringFacet field type on the genres field:

db.movies.createSearchIndex(
    "genresFacetedIndex",
    {
      "mappings": {
        "dynamic": false,
        "fields": {
          "genres": {
            "type": "stringFacet"
          },
          "released": {
            "type": "date"
          }
        }
      }
    }
  )
Create a Facet Using $searchMeta
In the following example, the facet operator is used inside the $searchMeta stage to find movies based on their release date and bucket them by genre.

db.movies.aggregate([
    {
      "$searchMeta": {
        "index": "genresFacetedIndex",
        "facet": {
          "operator": {
            "range": {
                "path": "released",
                "gte": ISODate("2000-01-01T00:00:00.000Z"),
                "lte": ISODate("2000-01-31T00:00:00.000Z")
              },
            },
          "facets": {
            "genresFacet": {
              "type": "string",
              "path": "genres"
            }
          }
        }
      }
    }
  ])
Optionally, we can limit the number of buckets in our search query by using the numBuckets option. In the example below, numBuckets is set to 2:

db.movies.aggregate([
    {
      "$searchMeta": {
        "index": "genresFacetedIndex",
        "facet": {
          "operator": {
            "range": {
                "path": "released",
                "gte": ISODate("2000-01-01T00:00:00.000Z"),
                "lte": ISODate("2000-01-31T00:00:00.000Z")
              },
            },
          "facets": {
            "genresFacet": {
              "type": "string",
              "path": "genres",
              "numBuckets":  2
            }
          }
        }
      }
    }
  ])
  
  
You have a collection of documents pertaining to books in a library. Each document has a field for the author and you want to create a search query that buckets the results by author. What data type does the author field need to be in your search index? (Select one.)

Correct Answer

a.
bucket
Incorrect. 

You would not use the bucket data type in your search index since bucket is not a valid data type for Atlas Search indexes.

b.
string
Incorrect. 

You would not use the string data type in your search index since it’s not a supported data type for faceting search results.

c.
stringFacet
Correct. 

You would use the stringFacet data type on the authors field in order to bucket your results with facets.

d.
facet
Incorrect. 

You would not use the facet data type in your search index since facet is not a valid data type for Atlas Search indexes.

Which field types in Atlas Search can be used for faceting when you create a search index? (Select all that apply.)

Incorrect Answer

a.
dateFacet
Correct. 

Atlas Search provides the dateFacet type for indexing date fields for faceting.

b.
geoFacet
Incorrect. 

The geoFacet data type is not supported by facets when creating a search index.

c.
stringFacet
Correct. 

Atlas Search provides the stringFacet type for indexing string fields for faceting.

d.
numberFacet
Correct. 

Atlas Search provides the numberFacet type for indexing numeric fields for faceting.



Create an Atlas Search Index with a Faceted Field Type
In this lab, you'll use the Atlas CLI command atlas deployments search indexes create to create an Atlas Search index that includes a faceted field type.

Your goal is to allow users to find movies released during a certain period of time grouped by language in the `` collection. To do this, you will need to configure the index mappings to include the languages and released fields with the required types.

{
    "name": "language_facet_idx",
    "collectionName": "movies",
    "database": "sample_mflix",
    "definition": {
      "mappings": {
        "dynamic": false,
        "fields": {
          "languages": {
            "type": "stringFacet"
          },
          "released": {
            "type": "date"
          }
        }
      }
    }
}

atlas deployments search indexes create --deploymentName myLocalRs1 --type LOCAL -f /lab/faceted_search_index.json --watch

atlas deployments search indexes list \
  --deploymentName myLocalRs1 \
  --db sample_mflix \
  --collection movies \
  --output=json \
  --type LOCAL \
  | jq --arg INDEX_NAME "language_facet_idx" '.[] | select(.Name == $INDEX_NAME) .Status'


Create a Search Query to Group Results Using Facets
In this lab, you'll use an aggregation pipeline with the $searchMeta stage and the facet collector to find movies released within a certain timeframe, group them by language, and then get a count for each group.

To do this, you will use the movies collection and the language_facet_idx search index that you created in the previous lab. Let's get started!



const facetedSearchPipeline = [{
    "$searchMeta": {
        "index": "language_facet_idx",
        "facet": {
            "operator": {
                "range": {
                    "path": "released",
                    "gte": ISODate("2000-01-01T00:00:00.000Z"),
                    "lte": ISODate("2000-01-31T00:00:00.000Z")
                },
            },
            "facets": {
                "languageFacet": {
                    "type": "string",
                    "path": "languages"
                }
            }
        }
    }
}]


### Results:
const facetedSearchPipeline = [{
    "$searchMeta": {
        "index": "language_facet_idx",
        "facet": {
            "operator": {
                "range": {
                    "path": "released",
                    "gte": ISODate("2000-01-01T00:00:00.000Z"),
                    "lte": ISODate("2000-01-31T00:00:00.000Z")
                },
            },
            "facets": {
                "languageFacet": {
                    "type": "string",
                    "path": "languages"
                }
            }
        }
    }
}]


docker run -p 27019:27017 -e MONGOT_LOG_FILE=/dev/stdout mongodb/mongodb-atlas-local

-m=4g

docker run -p 27019:27017 -m=4g -e MONGOT_LOG_FILE=/dev/stdout mongodb/mongodb-atlas-local
