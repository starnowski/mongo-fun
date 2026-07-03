package com.github.starnowski.mongo.fun;

import com.github.starnowski.jamolingo.junit5.MongoDocument;
import com.github.starnowski.jamolingo.junit5.MongoSetup;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class QueryNGramStringTest extends AbstractItTest {

  private static final String INDEX_NAME = "QueryNGramStringTest_ngram_idx";
  private static final String DATABASE_NAME = "testdb";
  private static final String COLLECTION_NAME = "filter_phrase_items";

  private static final String NGRAM_INDEX_DEF = """
                      {
                        "mappings": {
                          "dynamic": false,
                          "fields": {
                            "field1": [
                              {
                                "type": "string",
                                "analyzer": "custom_ngram"
                              }
                            ]
                          }
                        },
                        "analyzers": [
                                        {
                                          "name": "custom_ngram",
                                          "tokenizer": {
                                            "type": "nGram",
                                            "minGram": 3,
                                            "maxGram": 10
                                          }
                                        }
                                      ]
                      }
          """;

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
  public void shouldReturnExpectedDocumentsWithCorrectOrder(
      String searchQuery, List<String> expectedIds) throws InterruptedException {
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
              "$set": {
                "score": { "$meta": "searchScore" }
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
              expectedIds,
              results.stream().map(d -> d.getString("_id")).collect(Collectors.toList()),
              "Expected to find documents with expected order for query: " + searchQuery);
        });
  }

  // TODO
  // Parameters
  // query, expected results in correct order

  // TODO

  private void waitForSearchIndexSync(MongoCollection<Document> collection, String indexName)
      throws InterruptedException {
    waitForSearchIndexSync(collection, indexName, "type");
  }

  private void ensureSearchIndex(MongoCollection<Document> collection) {
    ensureSearchIndexReady(
        INDEX_NAME,
        """
          {
            "mappings": {
              "dynamic": false,
              "fields": {
                "type": { "type": "string" }
              }
            }
          }
          """,
        collection);
  }
}
