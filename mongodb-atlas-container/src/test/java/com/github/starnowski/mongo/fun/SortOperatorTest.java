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
public class SortOperatorTest {

  @Autowired protected MongoClient mongoClient;

  private static final String INDEX_NAME = "sort_idx";

  @Test
  @MongoSetup(
      mongoDocuments = {
        @MongoDocument(
            database = "testdb",
            collection = "movies",
            bsonFilePath = "bson/search/sort_movie1.json"),
        @MongoDocument(
            database = "testdb",
            collection = "movies",
            bsonFilePath = "bson/search/sort_movie2.json"),
        @MongoDocument(
            database = "testdb",
            collection = "movies",
            bsonFilePath = "bson/search/sort_movie3.json"),
        @MongoDocument(
            database = "testdb",
            collection = "movies",
            bsonFilePath = "bson/search/sort_movie4.json")
      })
  public void shouldSortByScoreAscending() throws InterruptedException {
    // GIVEN
    MongoDatabase database = mongoClient.getDatabase("testdb");
    MongoCollection<Document> collection = database.getCollection("movies");
    ensureSearchIndex(collection);
    waitForSearchIndexSync(collection, INDEX_NAME);

    List<Bson> pipeline =
        List.of(
            Document.parse(
                """
            {
              "$search": {
                "index": "%s",
                "text": {"query": "poet","path":"plot" },
                "sort": { "unused":{"$meta":"searchScore","order": -1 }}
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
                "title": "$title"
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
          Assertions.assertEquals(4, results.size());
          // In this case, all movies have the same plot "A poet travels the world", so scores
          // should be identical.
          // Sorting by score ascending/descending might not show order change if scores are exactly
          // the same.
        });
  }

  @Test
  @MongoSetup(
      mongoDocuments = {
        @MongoDocument(
            database = "testdb",
            collection = "movies",
            bsonFilePath = "bson/search/sort_movie1.json"),
        @MongoDocument(
            database = "testdb",
            collection = "movies",
            bsonFilePath = "bson/search/sort_movie2.json"),
        @MongoDocument(
            database = "testdb",
            collection = "movies",
            bsonFilePath = "bson/search/sort_movie3.json"),
        @MongoDocument(
            database = "testdb",
            collection = "movies",
            bsonFilePath = "bson/search/sort_movie4.json")
      })
  public void shouldSortByScoreDescendingWithTieBreaker() throws InterruptedException {
    // GIVEN
    MongoDatabase database = mongoClient.getDatabase("testdb");
    MongoCollection<Document> collection = database.getCollection("movies");
    ensureSearchIndex(collection);
    waitForSearchIndexSync(collection, INDEX_NAME);

    List<Bson> pipeline =
        List.of(
            Document.parse(
                """
            {
              "$search": {
                "index": "%s",
                "text": {"query": "poet","path":"plot" },
                "sort": {
                    "unused":{"$meta":"searchScore","order": 1 },
                    "released": 1
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
                "released": "$released"
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
          Assertions.assertEquals(4, results.size());
          // Scores are identical, so it should be sorted by released date ascending
          // sort_movie2: 2010
          // sort_movie3: 2015
          // sort_movie1: 2020
          // sort_movie4: 2025
          Assertions.assertEquals("B Poet's Life", results.get(0).getString("title"));
          Assertions.assertEquals("C Poet's Life", results.get(1).getString("title"));
          Assertions.assertEquals("A Poet's Life", results.get(2).getString("title"));
          Assertions.assertEquals("D Poet's Life", results.get(3).getString("title"));
        });
  }

  @Test
  @MongoSetup(
      mongoDocuments = {
        @MongoDocument(
            database = "testdb",
            collection = "movies",
            bsonFilePath = "bson/search/sort_movie1.json"),
        @MongoDocument(
            database = "testdb",
            collection = "movies",
            bsonFilePath = "bson/search/sort_movie2.json"),
        @MongoDocument(
            database = "testdb",
            collection = "movies",
            bsonFilePath = "bson/search/sort_movie3.json"),
        @MongoDocument(
            database = "testdb",
            collection = "movies",
            bsonFilePath = "bson/search/sort_movie4.json")
      })
  public void shouldSortByStringField() throws InterruptedException {
    // GIVEN
    MongoDatabase database = mongoClient.getDatabase("testdb");
    MongoCollection<Document> collection = database.getCollection("movies");
    ensureSearchIndex(collection);
    waitForSearchIndexSync(collection, INDEX_NAME);

    List<Bson> pipeline =
        List.of(
            Document.parse(
                """
            {
              "$search": {
                "index": "%s",
                "text": {"query": "poet","path":"plot" },
                "sort": { "title": 1}
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
                "title": "$title"
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
          Assertions.assertEquals(4, results.size());
          // Sorted by title ascending
          Assertions.assertEquals("A Poet's Life", results.get(0).getString("title"));
          Assertions.assertEquals("B Poet's Life", results.get(1).getString("title"));
          Assertions.assertEquals("C Poet's Life", results.get(2).getString("title"));
          Assertions.assertEquals("D Poet's Life", results.get(3).getString("title"));
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
                        "path": "plot"
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
              "dynamic": true,
              "fields": {
                "title": [{ "type": "token" }],
                "released": [{ "type": "date" }],
                "plot": [{ "type": "string" }]
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
