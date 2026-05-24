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
public class FuzzyOperatorTest {

  @Autowired protected MongoClient mongoClient;

  private static final String DATABASE_NAME = "testdb_fuzzy";
  private static final String COLLECTION_NAME = "movies";
  private static final String INDEX_NAME = "fuzzy_idx";

  @Test
  @MongoSetup(
      mongoDocuments = {
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/fuzzy_movie1.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/fuzzy_movie2.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/fuzzy_movie3.json")
      })
  public void shouldTestMaxEdits() throws InterruptedException {
    // GIVEN
    MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
    MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
    ensureSearchIndex(collection);
    waitForSearchIndexSync(collection, INDEX_NAME);

    // Search for "P0et" with maxEdits: 1 should match "poet"
    List<Bson> pipeline =
        List.of(
            Document.parse(
                """
            {
              "$search": {
                "index": "%s",
                "text": {
                  "query": "P0et",
                  "path": "plot",
                  "fuzzy": {"maxEdits": 1}
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
          Assertions.assertFalse(results.isEmpty(), "Expected to find results");
          boolean foundPoet =
              results.stream().anyMatch(doc -> "The Poet".equals(doc.getString("title")));
          Assertions.assertTrue(foundPoet, "Should find 'The Poet' for query 'P0et'");
        });
  }

  @Test
  @MongoSetup(
      mongoDocuments = {
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/fuzzy_movie1.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/fuzzy_movie2.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/fuzzy_movie3.json")
      })
  public void shouldTestPrefixLength() throws InterruptedException {
    // GIVEN
    MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
    MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
    ensureSearchIndex(collection);
    waitForSearchIndexSync(collection, INDEX_NAME);

    // Search for "Poetry" with prefixLength: 4. Only words starting with "Poet" should be matched
    // "Poe" + "try" -> prefix "Poet" (4 chars) must be exact.
    // Query "Poxtry" with prefixLength: 2 would match "Poetry", but with prefixLength: 3 it
    // wouldn't.
    // Let's use a query that has an error after the prefix.
    List<Bson> pipeline =
        List.of(
            Document.parse(
                """
            {
              "$search": {
                "index": "%s",
                "text": {
                  "query": "Poetxy",
                  "path": "plot",
                  "fuzzy": {"maxEdits": 1, "prefixLength": 4}
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
          Assertions.assertFalse(results.isEmpty(), "Expected to find results");
          boolean foundPoetry =
              results.stream().anyMatch(doc -> "The Poetry".equals(doc.getString("title")));
          Assertions.assertTrue(
              foundPoetry, "Should find 'The Poetry' for query 'Poetxy' with prefixLength 4");
        });
  }

  @Test
  @MongoSetup(
      mongoDocuments = {
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/fuzzy_movie1.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/fuzzy_movie2.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/fuzzy_movie3.json")
      })
  public void shouldTestMaxExpansions() throws InterruptedException {
    // GIVEN
    MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
    MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
    ensureSearchIndex(collection);
    waitForSearchIndexSync(collection, INDEX_NAME);

    // maxExpansions: The maximum number of variations to generate and search for.
    // This is harder to test definitively without knowing the internal dictionary,
    // but we can verify that the query still works with a small maxExpansions.
    List<Bson> pipeline =
        List.of(
            Document.parse(
                """
            {
              "$search": {
                "index": "%s",
                "text": {
                  "query": "Poe",
                  "path": "plot",
                  "fuzzy": {"maxEdits": 2, "maxExpansions": 10}
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
          Assertions.assertFalse(results.isEmpty(), "Expected to find results");
          // "Poe" with maxEdits 2 should match "poet", "poetry", "poems"
          boolean foundPoet =
              results.stream().anyMatch(doc -> "The Poet".equals(doc.getString("title")));
          boolean foundPoetry =
              results.stream().anyMatch(doc -> "The Poetry".equals(doc.getString("title")));
          boolean foundPoem =
              results.stream().anyMatch(doc -> "The Poem".equals(doc.getString("title")));
          Assertions.assertTrue(
              foundPoet || foundPoetry || foundPoem, "Should find at least one match");
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
                "title": { "type": "string" }
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
