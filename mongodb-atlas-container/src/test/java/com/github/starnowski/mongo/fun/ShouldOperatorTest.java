package com.github.starnowski.mongo.fun;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import com.github.starnowski.jamolingo.junit5.MongoDocument;
import com.github.starnowski.jamolingo.junit5.MongoSetup;
import com.github.starnowski.jamolingo.junit5.SpringMongoDataLoaderExtension;
import com.mongodb.MongoCommandException;
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
public class ShouldOperatorTest {

  @Autowired protected MongoClient mongoClient;

  private static final String DATABASE_NAME = "testdb_should";
  private static final String COLLECTION_NAME = "movies";
  private static final String INDEX_NAME = "should_plot_idx";

  @Test
  @MongoSetup(
      mongoDocuments = {
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/should_movie_poet.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/should_movie_elizabeth.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/should_movie_both.json")
      })
  public void shouldReturnMoviesWithDefaultMinimumShouldMatch() throws InterruptedException {
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
                    {"text":{ "query":"poet", "path":"plot" }},
                    {"text":{ "query":"Elizabeth", "path":"plot" }}
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
          // Should return at least 3 movies: "The Poet", "Elizabeth", "Elizabeth the Poet"
          Assertions.assertTrue(
              results.size() >= 3, "Expected at least 3 movies, but found " + results.size());
        });
  }

  @Test
  @MongoSetup(
      mongoDocuments = {
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/should_movie_poet.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/should_movie_elizabeth.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/should_movie_both.json")
      })
  public void shouldReturnMoviesWithExplicitMinimumShouldMatchTwo() throws InterruptedException {
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
                    {"text":{ "query":"poet", "path":"plot" }},
                    {"text":{ "query":"Elizabeth", "path":"plot" }}
                  ],
                  "minimumShouldMatch": 2
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
          // Should return only 1 movie: "Elizabeth the Poet"
          Assertions.assertEquals(
              1, results.size(), "Expected exactly 1 movie, but found " + results.size());
          Assertions.assertEquals("Elizabeth the Poet", results.get(0).getString("title"));
          Assertions.assertTrue(results.get(0).getDouble("score") > 0);
        });
  }

  @Test
  @MongoSetup(
      mongoDocuments = {
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/should_movie_poet.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/should_movie_elizabeth.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/should_movie_both.json")
      })
  public void shouldReturnMoviesWithExplicitMinimumShouldMatchTwoWithZeroScore()
      throws InterruptedException {
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
                                    {"text":{ "query":"poet", "path":"plot" , "score": { "constant": { "value": 0 } } }},
                                    {"text":{ "query":"Elizabeth", "path":"plot" , "score": { "constant": { "value": 0 } } }}
                                  ],
                                  "minimumShouldMatch": 2
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
          // Should return only 1 movie: "Elizabeth the Poet"
          Assertions.assertEquals(
              1, results.size(), "Expected exactly 1 movie, but found " + results.size());
          Assertions.assertEquals("Elizabeth the Poet", results.get(0).getString("title"));
          Assertions.assertEquals(
              0, Double.compare(Double.valueOf(0.0), results.get(0).getDouble("score")));
        });
  }

  @Test
  @MongoSetup(
      mongoDocuments = {
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/should_movie_poet.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/should_movie_elizabeth.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/should_movie_both.json")
      })
  public void shouldThrowErrorWhenMinimumShouldMatchExceedsConditions()
      throws InterruptedException {
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
                    {"text":{ "query":"poet", "path":"plot" }},
                    {"text":{ "query":"Elizabeth", "path":"plot" }}
                  ],
                  "minimumShouldMatch": 3
                }
              }
            }
            """
                    .formatted(INDEX_NAME)));

    // WHEN & THEN
    TestHelper.runAssertion(
        20,
        1,
        () -> {
          Assertions.assertThrows(
              MongoCommandException.class,
              () -> {
                collection.aggregate(pipeline).into(new ArrayList<>());
              },
              "Expected MongoCommandException because minimumShouldMatch (3) exceeds number of conditions (2)");
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
