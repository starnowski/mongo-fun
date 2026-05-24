package com.github.starnowski.mongo.fun;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import com.github.starnowski.jamolingo.junit5.MongoDocument;
import com.github.starnowski.jamolingo.junit5.MongoSetup;
import com.github.starnowski.jamolingo.junit5.SpringMongoDataLoaderExtension;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {SearchDemoApplication.class})
@AutoConfigureMockMvc
@ExtendWith(SpringMongoDataLoaderExtension.class)
public class CompoundOperatorTest {

  @Autowired protected MongoClient mongoClient;

  private static final String DATABASE_NAME = "testdb_compound";
  private static final String COLLECTION_NAME = "movies";
  private static final String INDEX_NAME = "compound_idx";

  @Test
  @MongoSetup(
      mongoDocuments = {
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/compound_movie1.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/compound_movie2.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/compound_movie3.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/compound_movie4.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/compound_movie5.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/compound_movie6.json")
      })
  public void shouldTestMustAndMustNotClauses() throws InterruptedException {
    // GIVEN
    MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
    MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
    ensureSearchIndex(collection);
    waitForSearchIndexSync(collection, INDEX_NAME);

    List<Bson> pipeline =
        List.of(
            Document.parse(
                """
            {
              "$search": {
                "index": "%s",
                "compound": {
                  "must": [
                    { "text": { "query": "poet", "path": "plot" } },
                    { "text": { "query": "Elizabeth", "path": "plot" } }
                  ],
                  "mustNot": [
                    { "text": { "query": "History", "path": "genres" } },
                    { "text": { "query": "Documentary", "path": "genres" } }
                  ]
                }
              }
            }
            """
                    .formatted(INDEX_NAME)),
            Document.parse(
                """
            {
              "$project": {
                "_id": 0,
                "score": { "$meta": "searchScore" },
                "title": "$title",
                "plot": "$plot",
                "genres": "$genres"
              }
            }
            """));

    // WHEN
    List<Document> results = new ArrayList<>();
    TestHelper.runAssertion(
        20,
        1,
        () -> {
          results.clear();
          collection.aggregate(pipeline).into(results);
          // THEN
          // "The Poet" matches must (poet, Elizabeth in plot) and is not "History" or
          // "Documentary".
          // "Elizabeth" matches must but is "History" genre, so it should be excluded.
          Assertions.assertFalse(results.isEmpty(), "Expected to find results");
          boolean foundPoet =
              results.stream().anyMatch(doc -> "The Poet".equals(doc.getString("title")));
          boolean foundElizabeth =
              results.stream().anyMatch(doc -> "Elizabeth".equals(doc.getString("title")));
          Assertions.assertTrue(foundPoet, "Should find 'The Poet'");
          Assertions.assertFalse(
              foundElizabeth, "Should NOT find 'Elizabeth' due to mustNot genre History");
        });
  }

  @Test
  @MongoSetup(
      mongoDocuments = {
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/compound_movie1.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/compound_movie2.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/compound_movie3.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/compound_movie4.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/compound_movie5.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/compound_movie6.json")
      })
  public void shouldTestMustAndShouldClauses() throws InterruptedException {
    // GIVEN
    MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
    MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
    ensureSearchIndex(collection);
    waitForSearchIndexSync(collection, INDEX_NAME);

    List<Bson> pipeline =
        List.of(
            Document.parse(
                """
            {
              "$search": {
                "index": "%s",
                "compound": {
                  "should": [
                    { "text": { "query": "poet", "path": "plot" } },
                    { "text": { "query": "Elizabeth", "path": "plot" } }
                  ],
                  "must": [
                    { "equals": { "value": 1934, "path": "year" } }
                  ]
                }
              }
            }
            """
                    .formatted(INDEX_NAME)),
            Document.parse(
                """
            {
              "$project": {
                "_id": 0,
                "score": { "$meta": "searchScore" },
                "year": "$year",
                "title": "$title",
                "plot": "$plot"
              }
            }
            """));

    // WHEN
    List<Document> results = new ArrayList<>();
    TestHelper.runAssertion(
        20,
        1,
        () -> {
          results.clear();
          collection.aggregate(pipeline).into(results);
          // THEN
          // Only "The Poet" (1934) matches must clause.
          Assertions.assertEquals(1, results.size(), "Expected exactly 1 movie");
          Assertions.assertEquals("The Poet", results.get(0).getString("title"));
          Assertions.assertEquals(1934, results.get(0).getInteger("year"));
        });
  }

  @Test
  @MongoSetup(
      mongoDocuments = {
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/compound_movie1.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/compound_movie2.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/compound_movie3.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/compound_movie4.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/compound_movie5.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/compound_movie6.json")
      })
  public void shouldTestMustMustNotShouldAndFilterClauses() throws InterruptedException {
    // GIVEN
    MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
    MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
    ensureSearchIndex(collection);
    waitForSearchIndexSync(collection, INDEX_NAME);

    List<Bson> pipeline =
        List.of(
            Document.parse(
                """
            {
              "$search": {
                "index": "%s",
                "compound": {
                  "filter": [{ "range": { "path": "year", "gte": 1992, "lte": 2000 } }],
                  "must": [
                    { "exists": { "path": "genres" } },
                    { "text": { "query": "earth", "path": "plot" } }
                  ],
                  "mustNot": [
                    { "text": { "query": "Documentary", "path": "genres" } },
                    { "text": { "query": "Drama", "path": "genres" } },
                    { "text": { "query": "Comedy", "path": "genres" } }
                  ],
                  "should": [
                    { "phrase": { "query": "John Leguizamo", "path": "cast" } },
                    { "phrase": { "query": "Gillian Anderson", "path": "cast" } },
                    { "phrase": { "query": "Paula Marshall", "path": "cast" } }
                  ],
                  "minimumShouldMatch": 1
                }
              }
            }
            """
                    .formatted(INDEX_NAME)),
            Document.parse(
                """
            {
              "$project": {
                "_id": 0,
                "score": { "$meta": "searchScore" },
                "year": "$year",
                "title": "$title",
                "plot": "$plot",
                "genres": "$genres",
                "cast": "$cast"
              }
            }
            """));

    // WHEN
    List<Document> results = new ArrayList<>();
    TestHelper.runAssertion(
        20,
        1,
        () -> {
          results.clear();
          collection.aggregate(pipeline).into(results);
          // THEN
          // "Action Movie 2000":
          // filter: 2000 (OK)
          // must: genres exist (OK), plot contains "earth" (OK)
          // mustNot: genre is Action (not Doc, Drama, Comedy) (OK)
          // should: cast is John Leguizamo (OK)
          // minShouldMatch: 1 (OK)
          Assertions.assertEquals(1, results.size(), "Expected exactly 1 movie");
          Assertions.assertEquals("Action Movie 2000", results.get(0).getString("title"));
        });
  }

  @Test
  @MongoSetup(
      mongoDocuments = {
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/compound_movie1.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/compound_movie2.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/compound_movie3.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/compound_movie4.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/compound_movie5.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/compound_movie6.json")
      })
  public void shouldTestNestedCompoundQuery() throws InterruptedException {
    // GIVEN
    MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
    MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
    ensureSearchIndex(collection);
    waitForSearchIndexSync(collection, INDEX_NAME);

    List<Bson> pipeline =
        List.of(
            Document.parse(
                """
            {
              "$search": {
                "index": "%s",
                "compound": {
                  "should": [
                    {
                      "compound": {
                        "must": [
                          { "phrase": { "query": "Star Wars", "path": "title" } },
                          { "phrase": { "query": "Liam Neeson", "path": "cast" } }
                        ]
                      }
                    },
                    {
                      "compound": {
                        "must": [ { "text": { "query": "Action", "path": "genres" } } ],
                        "mustNot": [ { "phrase": { "query": "Liam Neeson", "path": "cast" } } ]
                      }
                    }
                  ],
                  "minimumShouldMatch": 1
                }
              }
            }
            """
                    .formatted(INDEX_NAME)),
            Document.parse(
                """
            {
              "$project": {
                "_id": 0,
                "score": { "$meta": "searchScore" },
                "year": "$year",
                "title": "$title",
                "genres": "$genres",
                "cast": "$cast"
              }
            }
            """));

    // WHEN
    List<Document> results = new ArrayList<>();
    TestHelper.runAssertion(
        20,
        1,
        () -> {
          results.clear();
          collection.aggregate(pipeline).into(results);
          // THEN
          // Results should include:
          // 1. "Star Wars: Episode I - The Phantom Menace" (matches first inner compound)
          // 2. "Action Movie 2000" (matches second inner compound: Action genre, NOT Liam Neeson)
          // 3. "Star Wars" (NOT Liam Neeson, NOT matches first. BUT it matches second? Genres:
          // ["Action", "Sci-Fi"]. Yes!)
          Assertions.assertTrue(results.size() >= 2, "Expected at least 2 movies");
          boolean foundEp1 =
              results.stream().anyMatch(doc -> doc.getString("title").contains("Episode I"));
          boolean foundAction2000 =
              results.stream().anyMatch(doc -> "Action Movie 2000".equals(doc.getString("title")));
          boolean foundStarWars77 =
              results.stream().anyMatch(doc -> "Star Wars".equals(doc.getString("title")));

          Assertions.assertTrue(foundEp1, "Should find Star Wars Episode I");
          Assertions.assertTrue(foundAction2000, "Should find Action Movie 2000");
          Assertions.assertTrue(
              foundStarWars77,
              "Should find Star Wars (1977) because it is Action genre and not Liam Neeson");
        });
  }

  private void waitForSearchIndexSync(MongoCollection<Document> collection, String indexName)
      throws InterruptedException {
    long collectionCount = collection.countDocuments();
    await()
        .atMost(30, SECONDS)
        .pollInterval(1, SECONDS)
        .until(
            () -> {
              List<Document> results = new ArrayList<>();
              collection
                  .aggregate(
                      List.of(
                          Document.parse(
                              """
                  {
                    "$searchMeta": {
                      "index": "%s",
                      "exists": {
                        "path": "title"
                      },
                      "count": {
                        "type": "total"
                      }
                    }
                  }
                  """
                                  .formatted(indexName))))
                  .into(results);

              if (!results.isEmpty()) {
                Document countDoc = results.get(0).get("count", Document.class);
                return countDoc != null && countDoc.getLong("total") == collectionCount;
              }
              return false;
            });
  }

  private void ensureSearchIndex(MongoCollection<Document> collection) {
    try {
      Document indexDefinition =
          Document.parse(
              """
          {
            "mappings": {
              "dynamic": false,
              "fields": {
                "plot": { "type": "string" },
                "title": { "type": "string" },
                "genres": { "type": "string" },
                "year": { "type": "number" },
                "cast": { "type": "string" }
              }
            }
          }
          """);
      collection.createSearchIndex(INDEX_NAME, indexDefinition);

      // Wait for index to be ready
      await()
          .atMost(30, SECONDS)
          .pollInterval(1, SECONDS)
          .until(
              () -> {
                for (Document index : collection.listSearchIndexes()) {
                  if (INDEX_NAME.equals(index.getString("name"))
                      && "READY".equals(index.getString("status"))) {
                    return true;
                  }
                }
                return false;
              });
    } catch (Exception e) {
      // Index might already exist
    }
  }
}
