package com.github.starnowski.mongo.fun;

import static com.mongodb.ExplainVerbosity.QUERY_PLANNER;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.impl.async.H2AsyncMainClientExec;
import com.github.starnowski.jamolingo.junit5.MongoDocument;
import com.github.starnowski.jamolingo.junit5.MongoSetup;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class QueryNGramStringTest extends AbstractItTest {

  private static final String INDEX_NAME = "QueryNGramStringTest_ngram_idx";
  private static final String STANDARD_WITH_NGRAM_TOKEN_FILTER_NAME =
      "QueryNGramStringTest_standard_with_ngram_token_filter_idx";
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
                            ],
                            "field2": [
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

  private static final String STANDARD_WITH_NGRAM_TOKEN_FILTER_INDEX_DEF =
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
                                  ],
                                  "field2": [
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
                                                "charFilters": []
                                                "tokenizer": {
                                                  "type": "standard"
                                                },
                                                "tokenFilters": [
                                                    {
                                                        "type": "lowercase"
                                                    },
                                                    {
                                                        "type": "nGram",
                                                        "minGram": 3,
                                                        "maxGram": 10
                                                     }
                                                ]
                                              }
                                            ]
                            }
                """;

  private static final String PHRASE_OPERATOR_FIELD1_10_BOOST_FIELD2_1 =
      """
            {
              "$search": {
                "index": "%1$s",
                "compound": {
                  "should": [
                    {
                      "phrase": {
                        "query": "%2$s",
                        "path": "field1",
                        "score": { "boost": { "value" : 10 }  }
                      }
                    },
                    {
                      "phrase": {
                        "query": "%2$s",
                        "path": "field2",
                        "score": { "boost": { "value" : 1 }  }
                      }
                    }
                  ]
                  ,
                  "minimumShouldMatch": 1
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
            Map.of("QueryNGramStringTest_1", 0, "QueryNGramStringTest_2", 1, "QueryNGramStringTest_3", 2)),
        Arguments.of(
            PHRASE_OPERATOR_FIELD1.formatted(INDEX_NAME, "start123"),
                Map.of("QueryNGramStringTest_2", 0)),
        Arguments.of(
            PHRASE_OPERATOR_FIELD1.formatted(INDEX_NAME, "sta"), Map.of("QueryNGramStringTest_2", 0)),
        Arguments.of(PHRASE_OPERATOR_FIELD1.formatted(INDEX_NAME, "start"), Map.of()),
        Arguments.of(
            PHRASE_OPERATOR_FIELD1_10_BOOST_FIELD2_1.formatted(INDEX_NAME, "123"),
                Map.of("QueryNGramStringTest_1", 0, "QueryNGramStringTest_2", 0, "QueryNGramStringTest_3", 0)),
        Arguments.of(
            PHRASE_OPERATOR_FIELD1_10_BOOST_FIELD2_1.formatted(INDEX_NAME, "start"), Map.of()));
  }

  private static java.util.stream.Stream<Arguments>
      provideShouldReturnExpectedDocumentsWithCorrectOrderForSecondIndex() {
    return java.util.stream.Stream.of(
        Arguments.of(
            PHRASE_OPERATOR_FIELD1.formatted(STANDARD_WITH_NGRAM_TOKEN_FILTER_NAME, "123"),
            Map.of("QueryNGramStringTest_1", 0, "QueryNGramStringTest_2", 0, "QueryNGramStringTest_3", 0)),
        Arguments.of(
            PHRASE_OPERATOR_FIELD1.formatted(STANDARD_WITH_NGRAM_TOKEN_FILTER_NAME, "start123"),
            Map.of("QueryNGramStringTest_2", 0, "QueryNGramStringTest_1", 0, "QueryNGramStringTest_3", 0)),
        Arguments.of(
            PHRASE_OPERATOR_FIELD1.formatted(STANDARD_WITH_NGRAM_TOKEN_FILTER_NAME, "sta"),
            Map.of("QueryNGramStringTest_2", 0)),
        Arguments.of(
            PHRASE_OPERATOR_FIELD1.formatted(STANDARD_WITH_NGRAM_TOKEN_FILTER_NAME, "start"),
            Map.of("QueryNGramStringTest_2", 0)),
        Arguments.of(
            PHRASE_OPERATOR_FIELD1_10_BOOST_FIELD2_1.formatted(
                STANDARD_WITH_NGRAM_TOKEN_FILTER_NAME, "123"),
            Map.of("QueryNGramStringTest_1", 0, "QueryNGramStringTest_2", 0, "QueryNGramStringTest_3", 0)),
        Arguments.of(
            PHRASE_OPERATOR_FIELD1_10_BOOST_FIELD2_1.formatted(
                STANDARD_WITH_NGRAM_TOKEN_FILTER_NAME, "start"),
            Map.of("QueryNGramStringTest_2", 0)));
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
      String searchQuery, Map<String, Integer> expectedIdsWithScoreIndex) throws InterruptedException {
    // GIVEN
    MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
    MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
    ensureSearchIndex(collection);
    waitForSearchIndexSync(collection, INDEX_NAME);

    runTest(searchQuery, expectedIdsWithScoreIndex, collection);
  }

  @ParameterizedTest
  @MethodSource("provideShouldReturnExpectedDocumentsWithCorrectOrderForSecondIndex")
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
  public void shouldReturnExpectedDocumentsWithCorrectOrderForStandardIndex(
      String searchQuery, Map<String, Integer> expectedIdsWithScoreIndex) throws InterruptedException {
    // GIVEN
    MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
    MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
    ensureSearchStandardWithNGramTokenFiltersIndex(collection);
    waitForSearchIndexSync(collection, STANDARD_WITH_NGRAM_TOKEN_FILTER_NAME);

    runTest(searchQuery, expectedIdsWithScoreIndex, collection);
  }

  private void runTest(
      String searchQuery, Map<String, Integer> expectedIdsWithScoreIndex, MongoCollection<Document> collection) {

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
          System.out.println("Query explain QUERY_PLANNER ->");
          System.out.println(collection.aggregate(pipeline).explain(QUERY_PLANNER).toJson());
          System.out.println("<-");
          // THEN
          Assertions.assertEquals(
                  expectedIdsWithScoreIndex,
                  convertResultsToDocumentAndScoreIndex(results),
              "Expected to find documents with expected order for query: " + searchQuery);
        });
  }

  private Map<String, Integer> convertResultsToDocumentAndScoreIndex(List<Document> results) {
      List<Double> scores = results.stream().map(d -> d.getDouble("score")).distinct().sorted().toList();
      return results.stream().collect(Collectors.toMap(d -> d.getString("_id"), d -> scores.indexOf(d.getDouble("score")) ));
  }

  private void waitForSearchIndexSync(MongoCollection<Document> collection, String indexName)
      throws InterruptedException {
    waitForSearchIndexSync(collection, indexName, "field1");
  }

  private void ensureSearchIndex(MongoCollection<Document> collection) {
    ensureSearchIndexReady(INDEX_NAME, NGRAM_INDEX_DEF, collection);
  }

  private void ensureSearchStandardWithNGramTokenFiltersIndex(
      MongoCollection<Document> collection) {
    ensureSearchIndexReady(
        STANDARD_WITH_NGRAM_TOKEN_FILTER_NAME,
        STANDARD_WITH_NGRAM_TOKEN_FILTER_INDEX_DEF,
        collection);
  }
}
