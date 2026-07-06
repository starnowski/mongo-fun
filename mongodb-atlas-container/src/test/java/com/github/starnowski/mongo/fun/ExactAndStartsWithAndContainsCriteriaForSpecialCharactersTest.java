package com.github.starnowski.mongo.fun;

import com.github.starnowski.jamolingo.junit5.MongoDocument;
import com.github.starnowski.jamolingo.junit5.MongoSetup;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.util.Map;
import org.bson.Document;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ExactAndStartsWithAndContainsCriteriaForSpecialCharactersTest extends AbstractItTest {

  private static final String DEFAULT_INDEX_NAME =
      "ExactAndStartsWithAndContainsCriteriaForSpecialCharactersTest_default_idx";
  private static final String DATABASE_NAME = "testdb";
  private static final String COLLECTION_NAME = "filter_phrase_items";

  private static final String DEFAULT_INDEX_DEF =
      """
          {
              "mappings": {
                  "dynamic": false,
                  "fields": {
                      "field1": [
                          {
                              "type": "string",
                              "analyzer": "custom_ngram",
                              "multi": {
                                  "exact_match": {
                                      "type": "string",
                                      "analyzer": "keyword_lowercase"
                                  }
                              }
                          },
                          {
                                "type": "autocomplete",
                                "minGrams": 3,
                                "maxGrams": 10,
                                "tokenization": "edgeGram",
                                "analyzer": "lucene.whitespace",
                                "foldDiacritics": true
                              }
                      ],
                      "field2": [
                          {
                              "type": "string",
                              "analyzer": "custom_ngram",
                              "multi": {
                                  "exact_match": {
                                      "type": "string",
                                      "analyzer": "keyword_lowercase"
                                  }
                              }
                          },
                          {
                                "type": "autocomplete",
                                "minGrams": 3,
                                "maxGrams": 10,
                                "tokenization": "edgeGram",
                                "analyzer": "lucene.whitespace",
                                "foldDiacritics": true
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
                  },
                  {
                      "name": "keyword_lowercase",
                      "tokenizer": {
                          "type": "keyword"
                      },
                      "tokenFilters": [
                          {
                              "type": "lowercase"
                          }
                      ]
                  }
              ]
          }
          """;

  private static final String DEFAULT_QUERY_FIELD1 =
      """
                {
                  "$search": {
                    "index": "%1$s",
                    "compound": {
                      "should": [
                        {
                          "phrase": {
                            "query": "%2$s",
                            "path": { "value": "field1", "multi": "exact_match" }
                          }
                        },
                        {
                              "autocomplete": {
                                "query": "%2$s",
                                "path": "field1",
                                "tokenOrder": "sequential"
                              }
                        },
                        {
                                      "text": {
                                        "query": "%2$s",
                                        "path": "field1",
                                        "matchCriteria": "all"
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
            DEFAULT_QUERY_FIELD1.formatted(DEFAULT_INDEX_NAME, "123"),
            Map.of(
                "QueryNGramStringTest_1",
                0,
                "QueryNGramStringTest_2",
                1,
                "QueryNGramStringTest_3",
                2)),
        Arguments.of(
            DEFAULT_QUERY_FIELD1.formatted(DEFAULT_INDEX_NAME, "4-5"),
            Map.of(
                "QueryNGramStingWithSpecialCharactersTest_1",
                0,
                "QueryNGramStingWithSpecialCharactersTest_2",
                1,
                "QueryNGramStingWithSpecialCharactersTest_3",
                2)),
        Arguments.of(DEFAULT_QUERY_FIELD1.formatted(DEFAULT_INDEX_NAME, "4_5"), Map.of()),
        Arguments.of(
            DEFAULT_QUERY_FIELD1.formatted(DEFAULT_INDEX_NAME, "start123"),
            Map.of("QueryNGramStringTest_2", 0)),
        Arguments.of(
            DEFAULT_QUERY_FIELD1.formatted(DEFAULT_INDEX_NAME, "START123"),
            // Incorrect case-sensitive
            Map.of("QueryNGramStringTest_2", 0)),
        Arguments.of(
            DEFAULT_QUERY_FIELD1.formatted(DEFAULT_INDEX_NAME, "stART123"),
            // Incorrect case-sensitive
            Map.of("QueryNGramStringTest_2", 0)),
        Arguments.of(
            DEFAULT_QUERY_FIELD1.formatted(DEFAULT_INDEX_NAME, "stART123"),
            // Correct case for CONTAINS !!!!!
            Map.of("QueryNGramStringTest_2", 0)),
        Arguments.of(
            DEFAULT_QUERY_FIELD1.formatted(DEFAULT_INDEX_NAME, "sta"),
            Map.of("QueryNGramStringTest_2", 0, "QueryNGramStingWithSpecialCharactersTest_2", 1)),
        Arguments.of(
            DEFAULT_QUERY_FIELD1.formatted(DEFAULT_INDEX_NAME, "start"),
            Map.of("QueryNGramStringTest_2", 0, "QueryNGramStingWithSpecialCharactersTest_2", 1)),
        Arguments.of(
            DEFAULT_QUERY_FIELD1.formatted(DEFAULT_INDEX_NAME, "contains"),
            Map.of("QueryNGramStringTest_3", 0, "QueryNGramStingWithSpecialCharactersTest_3", 0)),
        Arguments.of(
            DEFAULT_QUERY_FIELD1.formatted(DEFAULT_INDEX_NAME, "s4-5c"),
            Map.of("QueryNGramStingWithSpecialCharactersTest_3", 0)));
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
            bsonFilePath = "bson/search/QueryNGramStringTest_contains_match.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/QueryNGramStingWithSpecialCharactersTest_exact_match.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath =
                "bson/search/QueryNGramStingWithSpecialCharactersTest_startsWith_match.json"),
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath =
                "bson/search/QueryNGramStingWithSpecialCharactersTest_contains_match.json")
      })
  public void shouldReturnExpectedDocumentsWithCorrectOrder(
      String searchQuery, Map<String, Integer> expectedIdsWithScoreIndex)
      throws InterruptedException {
    // GIVEN
    MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
    MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
    ensureSearchIndex(collection);
    waitForSearchIndexSync(collection, DEFAULT_INDEX_NAME);

    runTest(searchQuery, expectedIdsWithScoreIndex, collection);
  }

  private void waitForSearchIndexSync(MongoCollection<Document> collection, String indexName)
      throws InterruptedException {
    waitForSearchIndexSync(collection, indexName, "field1");
  }

  private void ensureSearchIndex(MongoCollection<Document> collection) {
    ensureSearchIndexReady(DEFAULT_INDEX_NAME, DEFAULT_INDEX_DEF, collection);
  }
}
