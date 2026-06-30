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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {SearchDemoApplication.class})
@AutoConfigureMockMvc
@ExtendWith(SpringMongoDataLoaderExtension.class)
public class FilterStringPhraseTest {

  @Autowired protected MongoClient mongoClient;

  private static final String INDEX_NAME = "filter_phrase_idx";
  private static final String KEYWORD_INDEX_NAME = "filter_phrase_keyword_idx";
  private static final String DATABASE_NAME = "testdb";
  private static final String COLLECTION_NAME = "filter_phrase_items";

  private static final String PHRASE_OPERATOR_TYPE_FIELD_QUERY =
      """
            {
              "$search": {
                "index": "%s",
                "phrase": {
                  "query": "%s",
                  "path": "type"
                }
              }
            }
            """;

  @ParameterizedTest
  @ValueSource(strings = {"Groccery", "GROCCERY", "groccery"})
  @MongoSetup(
      mongoDocuments = {
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/filter_phrase_1.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/filter_phrase_2.json")
      })
  public void shouldReturnBothDocumentsWhenSearchingByTypeUsingPhrase(String searchQuery)
      throws InterruptedException {
    // GIVEN
    MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
    MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
    ensureSearchIndex(collection);
    waitForSearchIndexSync(collection, INDEX_NAME);

    List<Bson> pipeline =
        List.of(
            Document.parse(PHRASE_OPERATOR_TYPE_FIELD_QUERY.formatted(INDEX_NAME, searchQuery)),
            Document.parse(
                """
            {
              "$project": {
                "_id": 1,
                "type": 1
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
          Assertions.assertEquals(
              2, results.size(), "Expected to find 2 documents for query: " + searchQuery);

          boolean found1 =
              results.stream()
                  .anyMatch(doc -> "filterStringPhraseTest_1".equals(doc.getString("_id")));
          boolean found2 =
              results.stream()
                  .anyMatch(doc -> "filterStringPhraseTest_2".equals(doc.getString("_id")));
          Assertions.assertTrue(
              found1, "Expected to find 'filterStringPhraseTest_1' in results: " + results);
          Assertions.assertTrue(
              found2, "Expected to find 'filterStringPhraseTest_2' in results: " + results);
        });
  }

  @ParameterizedTest
  @ValueSource(strings = {"Groccery", "GROCCERY", "groccery"})
  @MongoSetup(
      mongoDocuments = {
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/filter_phrase_1.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/filter_phrase_2.json")
      })
  public void shouldReturnBothDocumentsWhenSearchingByTypeUsingTextOperator(String searchQuery)
      throws InterruptedException {
    // GIVEN
    MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
    MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
    ensureSearchIndex(collection);
    waitForSearchIndexSync(collection, INDEX_NAME);

    List<Bson> pipeline =
        List.of(
            Document.parse(PHRASE_OPERATOR_TYPE_FIELD_QUERY.formatted(INDEX_NAME, searchQuery)),
            Document.parse(
                """
                            {
                              "$project": {
                                "_id": 1,
                                "type": 1
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
          Assertions.assertEquals(
              2, results.size(), "Expected to find 2 documents for query: " + searchQuery);

          boolean found1 =
              results.stream()
                  .anyMatch(doc -> "filterStringPhraseTest_1".equals(doc.getString("_id")));
          boolean found2 =
              results.stream()
                  .anyMatch(doc -> "filterStringPhraseTest_2".equals(doc.getString("_id")));
          Assertions.assertTrue(
              found1, "Expected to find 'filterStringPhraseTest_1' in results: " + results);
          Assertions.assertTrue(
              found2, "Expected to find 'filterStringPhraseTest_2' in results: " + results);
        });
  }

  @ParameterizedTest
  @ValueSource(strings = {"Groccery", "GROCCERY", "groccery"})
  @MongoSetup(
      mongoDocuments = {
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/filter_phrase_1.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/filter_phrase_2.json")
      })
  public void shouldReturnBothDocumentsWhenSearchingByTypeUsingTextOperatorForKeywordAnalyzer(
      String searchQuery) throws InterruptedException {
    // GIVEN
    // TODO
    MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
    MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
    ensureSearchIndexWithKeyword(collection);
    waitForSearchIndexSync(collection, KEYWORD_INDEX_NAME);

    List<Bson> pipeline =
        List.of(
            Document.parse(
                """
                                            {
                                              "$search": {
                                                "index": "%s",
                                                "text": {
                                                  "query": "%s",
                                                  "path": "type"
                                                }
                                              }
                                            }
                                            """
                    .formatted(KEYWORD_INDEX_NAME, searchQuery)),
            Document.parse(
                """
                                            {
                                              "$project": {
                                                "_id": 1,
                                                "type": 1
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
          Assertions.assertEquals(
              2, results.size(), "Expected to find 2 documents for query: " + searchQuery);

          boolean found1 =
              results.stream()
                  .anyMatch(doc -> "filterStringPhraseTest_1".equals(doc.getString("_id")));
          boolean found2 =
              results.stream()
                  .anyMatch(doc -> "filterStringPhraseTest_2".equals(doc.getString("_id")));
          Assertions.assertTrue(
              found1, "Expected to find 'filterStringPhraseTest_1' in results: " + results);
          Assertions.assertTrue(
              found2, "Expected to find 'filterStringPhraseTest_2' in results: " + results);
        });
  }

  @ParameterizedTest
  @ValueSource(strings = {"Groccery", "GROCCERY", "groccery"})
  @MongoSetup(
      mongoDocuments = {
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/filter_phrase_1.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/filter_phrase_2.json")
      })
  public void shouldReturnBothDocumentsWhenSearchingByTypeUsingCompoundFilter(String searchQuery)
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
                  "filter": [
                    {
                      "phrase": {
                        "query": "%s",
                        "path": "type"
                      }
                    }
                  ]
                }
              }
            }
            """
                    .formatted(INDEX_NAME, searchQuery)),
            Document.parse(
                """
            {
              "$project": {
                "_id": 1,
                "type": 1
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
          Assertions.assertEquals(
              2, results.size(), "Expected to find 2 documents for query: " + searchQuery);

          boolean found1 =
              results.stream()
                  .anyMatch(doc -> "filterStringPhraseTest_1".equals(doc.getString("_id")));
          boolean found2 =
              results.stream()
                  .anyMatch(doc -> "filterStringPhraseTest_2".equals(doc.getString("_id")));
          Assertions.assertTrue(
              found1, "Expected to find 'filterStringPhraseTest_1' in results: " + results);
          Assertions.assertTrue(
              found2, "Expected to find 'filterStringPhraseTest_2' in results: " + results);
        });
  }

  @ParameterizedTest
  @CsvSource({
    "x-groccery,filterStringPhraseTest_3",
    "X-GROCCERY,filterStringPhraseTest_3",
    "X-grocCERY,filterStringPhraseTest_3",
    "x-groccery-2,filterStringPhraseTest_4",
    "x-GROCCERY-2,filterStringPhraseTest_4"
  })
  @MongoSetup(
      mongoDocuments = {
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/filter_phrase_3.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/filter_phrase_4.json")
      })
  public void shouldReturnExpectedFirstDocumentWhenSearchingByTypeUsingPhrase(
      String searchQuery, String expectedDocumentId) throws InterruptedException {
    // GIVEN
    MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
    MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
    ensureSearchIndex(collection);
    waitForSearchIndexSync(collection, INDEX_NAME);

    List<Bson> pipeline =
        List.of(
            Document.parse(PHRASE_OPERATOR_TYPE_FIELD_QUERY.formatted(INDEX_NAME, searchQuery)),
            Document.parse(
                """
                            {
                              "$project": {
                                "_id": 1,
                                "type": 1
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
          Assertions.assertFalse(
              results.isEmpty(),
              "Expected to find at least one documents for query: " + searchQuery);

          Assertions.assertEquals(
              expectedDocumentId,
              results.getFirst().getString("_id"),
              "Expected to find as first document : " + expectedDocumentId);
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
                        "path": "type"
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
                "type": { "type": "string" }
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

  private void ensureSearchIndexWithKeyword(MongoCollection<Document> collection) {
    try {
      Document indexDefinition =
          Document.parse(
              """
                        {
                          "mappings": {
                            "dynamic": false,
                            "fields": {
                              "type": { "type": "string", "analyzer": "keyword_lowercase" }
                            }
                          }
                          ,
                          "analyzers": [
                                          {
                                            "name": "keyword_lowercase",
                                            "tokenizer": { "type": "keyword"},
                                            "tokenFilters": [
                                                {"type": "lowercase"}
                                            ]
                                          }
                                        ]
                        }
                        """);
      collection.createSearchIndex(KEYWORD_INDEX_NAME, indexDefinition);

      // Wait for index to be ready
      await()
          .atMost(30, SECONDS)
          .pollInterval(1, SECONDS)
          .until(
              () -> {
                for (Document index : collection.listSearchIndexes()) {
                  if (KEYWORD_INDEX_NAME.equals(index.getString("name"))
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
