
#### Atlas management


To manage search indexes in the Atlas UI, what Atlas user role must you have? (Select all that apply.)

Correct Answer

a.
Project Search Index Editor
Correct. 

The Project Search Index Editor Atlas user role allows you to manage search indexes in the Atlas UI.

b.
Project Read Only
Incorrect. 

The Project Read Only Atlas user role does not allow you to manage search indexes in the Atlas UI.

c.
Project Data Access Read Write
Incorrect.

The Project Data Access Read Write Atlas user role does not allow you to manage search indexes in the Atlas UI.

d.
Project Data Access Admin or higher
Correct. 

Project Data Access Admin or a higher Atlas user role allows you to manage search indexes in the Atlas UI.


Which of the following search index editors are available to use in the Atlas UI? (Select all that apply.)

Correct Answer

a.
VS Code
Incorrect. 

VS Code is not available in the Atlas UI for creating and editing search indexes.

b.
JSON Editor
Correct.

The JSON Editor is available in the Atlas UI for creating and editing search indexes. This editor is usually preferred by seasoned experts.

c.
Visual Editor
Correct. 

The Visual Editor is available in the Atlas UI for creating and editing search indexes. This editor is a great option if you’re a beginner when it comes to creating search indexes in Atlas.

/// Atlas CLI

Code Summary: Managing Atlas Search Indexes via the Atlas CLI
Create an Atlas Search Index Using the Atlas CLI
To create an Atlas Search index using the Atlas CLI, create a JSON file with the search index definition.

{
    "name": "genreIndex",
    "clusterName": "Cluster0",
    "collectionName": "movies",
    "database": "sample_mflix",
    "mappings": {
      "dynamic": true
    }
}
Next, run the following command in the Atlas CLI to create an Atlas Search index:

atlas clusters search index create \
    --clusterName Cluster0 \
    --file ./index.json \
    --projectId 000000000000000000
Update an Existing Atlas Search Index Using the Atlas CLI
To update an existing Atlas Search index using the Atlas CLI, create a JSON file with the updated search index definition.

{
    "name": "genreIndex",
    "clusterName": "Cluster0",
    "collectionName": "movies",
    "database": "sample_mflix",
    "mappings": {
      "fields": {
        "genres": {
            "type": "string"
          }
      }
    }
 }
Next, run the following command in the Atlas CLI to update an existing Atlas Search index:

atlas clusters search indexes update \
    <Search Index ID> \
    --clusterName Cluster0 \
    --file ./index.json \
    --projectId 000000000000000000
List Atlas Search Indexes Using the Atlas CLI
To list all Atlas Search indexes in the Atlas CLI, run the following command:

atlas clusters search indexes list \
    --projectId 000000000000000000 \
    --clusterName Cluster0 \
    --collection movies \
    --db sample_mflix
View an Atlas Search Index Using the Atlas CLI
To view an Atlas Search index using the Atlas CLI, run the following command:

atlas clusters search indexes describe \
    <Search Index ID> \
    --projectId 000000000000000000 \
    --clusterName Cluster0 \
    --output json
Delete an Atlas Search Index Using the Atlas CLI
To delete an Atlas Search index using the Atlas CLI, run the following command:

atlas clusters search indexes delete \
    <Search Index ID> \
    --clusterName Cluster0 \
    --projectId 000000000000000000 
	
	
//// Quiz

To manage search indexes in the Atlas CLI, what Atlas user role must you have? (Select all that apply.)

Incorrect Answer

a.
Project Search Index Editor
Incorrect.

The Project Search Index Editor Atlas user role does not allow you to manage search indexes in the Atlas CLI. This role only lets you create, view, edit, and delete Atlas Search indexes by using the Atlas UI or API.

b.
Project Owner
Correct. 

The Project Owner Atlas user role allows you to manage search indexes in the Atlas CLI.

c.
Organization Owner
Correct. 

The Organization Owner Atlas user role allows you to manage search indexes in the Atlas CLI.

d.
Project Data Access Admin or higher
Correct. 

Project Data Access Admin or higher Atlas user role allows you to manage search indexes in the Atlas CLI.

///////////////////
Which command can you use in the Atlas CLI to retrieve a list of your Atlas Search index IDs? (Select one.)

Correct Answer

a.
atlas search indexes list
Incorrect. 

atlas search indexes list isn't supported by the Atlas CLI.

b.
atlas clusters search indexes list
Correct. 

atlas clusters search indexes list retrieves a list of your Atlas Search index IDs in the Atlas CLI.

c.
atlas clusters search indexes
Incorrect.

atlas clusters search indexes does not retrieve a list of your Atlas Search index IDs in the Atlas CLI.

d.
atlas clusters search getIndexes
Incorrect. 

atlas clusters search getIndexes isn't supported by the Atlas CLI.

///////////// LABS //////////////////////////

{
    "name": "mySearchIndex",
    "collectionName": "movies",
    "database": "sample_mflix",
    "definition": {
        "mappings": {
            "dynamic": true
        }
    }
}

/// create index based on json file
atlas deployments search indexes create \
  --deploymentName myLocalRs1 \
  -f /lab/search_index.json --watch
  
/// checking search index status
atlas deployments search indexes list \
  --deploymentName myLocalRs1 \
  --db sample_mflix \
  --collection movies \
  --output=json \
  --type LOCAL \
  | jq --arg INDEX_NAME "mySearchIndex" '.[] | select(.Name == $INDEX_NAME) .Status'
  
/// running the aggregation pipeline
db.movies.aggregate([
  {
    $search: {
      index: "mySearchIndex",
      text: { query: "Hockey", path: "plot" },
    },
  },
  { $project: { title: 1, year: 1, plot: 1 } },
  { $limit: 5 },
]);

atlas deployments search indexes list \
  --deploymentName myLocalRs1 \
  --db sample_mflix \
  --collection movies \
  --output=json \
  --type LOCAL
 
  
  atlas clusters search indexes describe \
    <Search Index ID> \
    --projectId 000000000000000000 \
    --clusterName Cluster0 \
    --output json
  
#### Describe single index
atlas deployments search indexes describe \
	69b709570195723fe1ecf7ee \
  --deploymentName myLocalRs1 \
  --output=json \
  --type LOCAL

{
  "IndexID": "69b709570195723fe1ecf7ee",
  "Database": "sample_mflix",
  "CollectionName": "movies",
  "Name": "mySearchIndex",
  "Type": "search",
  "Status": "READY",
  "Queryable": true,
  "LatestDefinition": [
    {
      "Key": "mappings",
      "Value": [
        {
          "Key": "dynamic",
          "Value": true
        },
        {
          "Key": "fields",
          "Value": []
        }
      ]
    }
  ],
  "LatestVersion": 0
}

atlas deployments search indexes describe \
	69b709bd0195723fe1ecf7f0 \
  --deploymentName myLocalRs1 \
  --output=json \
  --type LOCAL

{
  "IndexID": "69b709bd0195723fe1ecf7f0",
  "Database": "sample_mflix",
  "CollectionName": "movies",
  "Name": "titleIndex",
  "Type": "search",
  "Status": "READY",
  "Queryable": true,
  "LatestDefinition": [
    {
      "Key": "mappings",
      "Value": [
        {
          "Key": "dynamic",
          "Value": false
        },
        {
          "Key": "fields",
          "Value": [
            {
              "Key": "title",
              "Value": [
                {
                  "Key": "type",
                  "Value": "string"
                },
                {
                  "Key": "indexOptions",
                  "Value": "offsets"
                },
                {
                  "Key": "store",
                  "Value": true
                },
                {
                  "Key": "norms",
                  "Value": "include"
                }
              ]
            }
          ]
        }
      ]
    }
  ],
  "LatestVersion": 0
}

//////////////////////////

atlas deployments search indexes list   --deploymentName myLocalRs1   --db sample_mflix   --collection movies   --output=json   --type LOCAL
[
  {
    "IndexID": "69b709570195723fe1ecf7ee",
    "Database": "sample_mflix",
    "CollectionName": "movies",
    "Name": "mySearchIndex",
    "Type": "search",
    "Status": "READY",
    "Queryable": true,
    "LatestDefinition": [
      {
        "Key": "mappings",
        "Value": [
          {
            "Key": "dynamic",
            "Value": true
          },
          {
            "Key": "fields",
            "Value": []
          }
        ]
      }
    ],
    "LatestVersion": 0
  },
  {
    "IndexID": "69b709bd0195723fe1ecf7f0",
    "Database": "sample_mflix",
    "CollectionName": "movies",
    "Name": "titleIndex",
    "Type": "search",
    "Status": "READY",
    "Queryable": true,
    "LatestDefinition": [
      {
        "Key": "mappings",
        "Value": [
          {
            "Key": "dynamic",
            "Value": false
          },
          {
            "Key": "fields",
            "Value": [
              {
                "Key": "title",
                "Value": [
                  {
                    "Key": "type",
                    "Value": "string"
                  },
                  {
                    "Key": "indexOptions",
                    "Value": "offsets"
                  },
                  {
                    "Key": "store",
                    "Value": true
                  },
                  {
                    "Key": "norms",
                    "Value": "include"
                  }
                ]
              }
            ]
          }
        ]
      }
    ],
    "LatestVersion": 0
  }
]


//titleIndex 

atlas deployments search indexes describe \
	titleIndex \
  --deploymentName myLocalRs1 \
  --output=json \
  --type LOCAL

atlas deployments search indexes describe \
--deploymentName myLocalRs1 \
69b709bd0195723fe1ecf7f0

/////////////////////////

Delete Search Indexes Using the Atlas CLI
In this lab, you'll retrieve the ID of the indexes you wish to remove, delete an index with confirmation, and also use the --force flag to delete an index without confirmation.


atlas deployments search indexes list \
  --deploymentName myLocalRs1 \
  --db sample_mflix \
  --collection movies
ID                         NAME            DATABASE       COLLECTION   STATUS   TYPE
69b709570195723fe1ecf7ee   mySearchIndex   sample_mflix   movies       READY    search
69b709bd0195723fe1ecf7f0   titleIndex      sample_mflix   movies       READY    search

atlas deployments search indexes delete \
  --deploymentName myLocalRs1 \
  69b709570195723fe1ecf7ee
  
atlas deployments search indexes delete \
  --deploymentName myLocalRs1 \
  69b709bd0195723fe1ecf7f0 \
  --force
  
### Mongos handling
Code Summary: Managing Atlas Search Indexes via mongosh
Create an Atlas Search Index Using the MongoDB Shell
To create an Atlas Search index using the MongoDB Shell, use the createSearchIndex method on the desired collection. Pass in a name and the index definition as arguments.

db.movies.createSearchIndex(
    "titleIndex",
     {
        "mappings": {
           "fields": {
              "title": {
                 "type": "string"
              }
           }
        }
     }
  )
View an Atlas Search Index Using the MongoDB Shell
To view an Atlas Search index using the MongoDB Shell, use the getSearchIndexes method on the desired collection. Pass in the name of the search index as an argument.

db.movies.getSearchIndexes("titleIndex")
List all Atlas Search Indexes Using the MongoDB Shell
To list all Atlas Search indexes using the MongoDB Shell, use the getSearchIndexes method on the desired collection with no arguments.

db.movies.getSearchIndexes()
Update an Atlas Search Index Using the MongoDB Shell
To update an Atlas Search index using the MongoDB Shell, use the updateSearchIndexes method on the desired collection. Pass in the name of the search index and the updated index definition as arguments.

db.movies.updateSearchIndex(
    "titleIndex",
    {
       "mappings": { "dynamic": true },
    }
 )
Delete an Atlas Search Index Using the MongoDB Shell
To delete an Atlas Search index using the MongoDB Shell, use the dropSearchIndex method on the desired collection. Pass in the name of the search index as an argument.

db.movies.dropSearchIndex("titleIndex")

//////// Quiz

To manage search indexes in the MongoDB Shell, which database privilege must you have? (Select all that apply.)

Correct Answer

a.
createSearchIndexes
Correct. 

The createSearchIndexes database privilege allows you to create search indexes with the MongoDB Shell.

b.
dropSearchIndexes
Correct. 

The dropSearchIndexes database privilege allows you to delete search indexes with the MongoDB Shell.

c.
updateSearchIndexes
Correct. 

The updateSearchIndexes database privilege allows you to update search indexes with the MongoDB Shell.

d.
listSearchIndexes
Correct. 

The listSearchIndexes database privilege allows you to view search indexes with the MongoDB Shell.

e.
adminSearchIndexes
Incorrect. 

The adminSearchIndexes database privilege does not allow you to manage search indexes in the MongoDB Shell.

You have created a search index named genresIndex. How would you retrieve the details for only the genresIndex? (Select one.)

Correct Answer

a.
db.collection.getSearchIndexes()
Incorrect. 

getSearchIndexes() will not retrieve the details of just the genresIndex search index. Instead, it will retrieve the status of all search indexes in the collection.

b.
db.collection.getSearchIndexes(“genresIndex”)
Correct. 

getSearchIndexes(“genresIndex”) will retrieve the details of just the genresIndex search index.

c.
db.collection.retrieveSearchIndexes(“genresIndex”)
Incorrect.

retrieveSearchIndexes(“genresIndex”) will not retrieve the details of just the genresIndex search index. This is not a valid command in the MongoDB Shell.

d.
db.collection.findSearchIndexes(“genresIndex”)
Incorrect.

findSearchIndexes(“genresIndex”) will not retrieve the details of just the genresIndex search index. This is not a valid command in the MongoDB Shell.

  
https://learn.mongodb.com/learn/course/advanced-queries-with-atlas-search/lesson-1-using-the-must-mustnot-and-filter-clauses/learn?client=customer
