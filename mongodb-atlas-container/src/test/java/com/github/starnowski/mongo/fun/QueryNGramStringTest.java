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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class QueryNGramStringTest extends AbstractItTest {

  private static final String INDEX_NAME = "QueryNGramStringTest_ngram_idx";
  private static final String DATABASE_NAME = "testdb";
  private static final String COLLECTION_NAME = "filter_phrase_items";

  private static final String NGRAM_INDEX_DEF =
      """
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

  private static final String PHRASE_OPERATOR_FIELD1_10_BOOST_FIELD2_1 =
      """
            {
              "$search": {
                "index": "%1$s",
                "compund": {
                  "should": [
                    {
                      "phrase": {
                        "query": "%2$s",
                        "path": "field1",
                        "score": { "boost": 10 }
                      }
                    },
                    {
                      "phrase": {
                        "query": "%2$s",
                        "path": "field2",
                        "score": { "boost": 1 }
                      }
                    }
                  ]
                }
              }
            }
            """;

  private static final String PHRASE_OPERATOR_FIELD1 =
      """
                {
                  "$search": {
                    "index": "%1$s",
                    "compound": {
                      "should": [
                        {
                          "phrase": {
                            "query": "%2$s",
                            "path": "field1"
                          }
                        }
                      ],
                      "minimumShouldMatch": 1
                    }
                  }
                }
                """;

  private static java.util.stream.Stream<Arguments>
      provideShouldReturnExpectedDocumentsWithCorrectOrder() {
    return java.util.stream.Stream.of(
        Arguments.of(
            PHRASE_OPERATOR_FIELD1.formatted(INDEX_NAME, "123"),
            List.of("QueryNGramStringTest_1", "QueryNGramStringTest_2", "QueryNGramStringTest_3")));
  }

  @ParameterizedTest
  @MethodSource("provideShouldReturnExpectedDocumentsWithCorrectOrder")
  @MongoSetup(
      mongoDocuments = {
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/QueryNGramStringTest_exact_match.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/QueryNGramStringTest_startsWith_match.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/QueryNGramStringTest_contains_match.json")
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
            Document.parse(searchQuery),
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
          System.out.println("Results ->" + results + "<-");
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
    waitForSearchIndexSync(collection, indexName, "field1");
  }

  private void ensureSearchIndex(MongoCollection<Document> collection) {
    ensureSearchIndexReady(INDEX_NAME, NGRAM_INDEX_DEF, collection);
  }
}
