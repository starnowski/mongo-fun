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

public class QueryNGramStingWithSpecialCharactersTest extends AbstractItTest {

  private static final String KEYWORD_INDEX_NAME =
      "QueryNGramStingWithSpecialCharactersTest_keyword_idx";
  private static final String AUTOCOMPLETE_INDEX_NAME =
      "QueryNGramStingWithSpecialCharactersTest_autocomplete_idx";
  private static final String SINGLE_NGRAM_LOWERCASE_INDEX_NAME =
      "QueryNGramStingWithSpecialCharactersTest_single_ngram_lowercase_idx";
  private static final String DATABASE_NAME = "testdb";
  private static final String COLLECTION_NAME = "filter_phrase_items";

  private static final String KEYWORD_INDEX_DEF =
      """
          {
          	"mappings": {
          		"dynamic": false,
          		"fields": {
          			"field1": [
          				{
          					"type": "string",
          					"analyzer": "keyword_lowercase"
          				}
          			],
          			"field2": [
          				{
          					"type": "string",
          					"analyzer": "keyword_lowercase"
          				}
          			]
          		}
          	},
          	"analyzers": [
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

  private static final String AUTOCOMPLETE_INDEX_DEF =
      """
                      {
                        "mappings": {
                          "dynamic": false,
                          "fields": {
                            "field1": [
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
                                "type": "autocomplete",
                                "minGrams": 3,
                                "maxGrams": 10,
                                "tokenization": "edgeGram",
                                "analyzer": "lucene.whitespace",
                                "foldDiacritics": true
                              }
                            ]
                          }
                        }
                      }
          """;

  private static final String SINGLE_NGRAM_LOWERCASE_INDEX_DEF =
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

  private static final String AUTOCOMPLETE_OPERATOR_FIELD1 =
      """
                    {
                      "$search": {
                        "index": "%1$s",
                        "compound": {
                          "should": [
                            {
                              "autocomplete": {
                                "query": "%2$s",
                                "path": "field1",
                                "tokenOrder": "sequential"
                              }
                            }
                          ],
                          "minimumShouldMatch": 1
                        }
                      }
                    }
                    """;

  private static final String TEXT_OPERATOR_FIELD1 =
      """
                        {
                          "$search": {
                            "index": "%1$s",
                            "compound": {
                              "should": [
                                {
                                  "text": {
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

  /** Correct case for CONTAINS query !!!!! */
  private static final String TEXT_OPERATOR_ALL_CRITERIA_MATCH_FIELD1 =
      """
                            {
                              "$search": {
                                "index": "%1$s",
                                "compound": {
                                  "should": [
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
      provideShouldReturnExpectedDocumentsWithCorrectOrderForKeywordIndex() {
    return java.util.stream.Stream.of(
        Arguments.of(
            PHRASE_OPERATOR_FIELD1.formatted(KEYWORD_INDEX_NAME, "123"),
            Map.of("QueryNGramStringTest_1", 0)),
        Arguments.of(
            PHRASE_OPERATOR_FIELD1.formatted(KEYWORD_INDEX_NAME, "start123"),
            Map.of("QueryNGramStringTest_2", 0)),
        Arguments.of(
            PHRASE_OPERATOR_FIELD1.formatted(KEYWORD_INDEX_NAME, "START123"),
            Map.of("QueryNGramStringTest_2", 0)),
        Arguments.of(
            PHRASE_OPERATOR_FIELD1.formatted(KEYWORD_INDEX_NAME, "STart123"),
            Map.of("QueryNGramStringTest_2", 0)),
        Arguments.of(PHRASE_OPERATOR_FIELD1.formatted(KEYWORD_INDEX_NAME, "sta"), Map.of()),
        Arguments.of(PHRASE_OPERATOR_FIELD1.formatted(KEYWORD_INDEX_NAME, "start"), Map.of()),
        Arguments.of(
            PHRASE_OPERATOR_FIELD1.formatted(KEYWORD_INDEX_NAME, "4-5"),
            Map.of("QueryNGramStingWithSpecialCharactersTest_1", 0)));
  }

  private static java.util.stream.Stream<Arguments>
      provideShouldReturnExpectedDocumentsWithCorrectOrderForAutocompleteIndex() {
    return java.util.stream.Stream.of(
        Arguments.of(
            AUTOCOMPLETE_OPERATOR_FIELD1.formatted(AUTOCOMPLETE_INDEX_NAME, "123"),
            Map.of("QueryNGramStringTest_1", 0)),
        Arguments.of(
            AUTOCOMPLETE_OPERATOR_FIELD1.formatted(AUTOCOMPLETE_INDEX_NAME, "12"), Map.of()),
        Arguments.of(
            AUTOCOMPLETE_OPERATOR_FIELD1.formatted(AUTOCOMPLETE_INDEX_NAME, "start123"),
            Map.of("QueryNGramStringTest_2", 0)),
        Arguments.of(
            AUTOCOMPLETE_OPERATOR_FIELD1.formatted(AUTOCOMPLETE_INDEX_NAME, "START123"),
            Map.of("QueryNGramStringTest_2", 0)),
        Arguments.of(
            AUTOCOMPLETE_OPERATOR_FIELD1.formatted(AUTOCOMPLETE_INDEX_NAME, "stART123"),
            Map.of("QueryNGramStringTest_2", 0)),
        Arguments.of(
            AUTOCOMPLETE_OPERATOR_FIELD1.formatted(AUTOCOMPLETE_INDEX_NAME, "sta"),
            Map.of("QueryNGramStringTest_2", 0, "QueryNGramStingWithSpecialCharactersTest_2", 0)),
        Arguments.of(
            AUTOCOMPLETE_OPERATOR_FIELD1.formatted(AUTOCOMPLETE_INDEX_NAME, "star"),
            Map.of("QueryNGramStringTest_2", 0, "QueryNGramStingWithSpecialCharactersTest_2", 0)),
        Arguments.of(
            AUTOCOMPLETE_OPERATOR_FIELD1.formatted(AUTOCOMPLETE_INDEX_NAME, "start"),
            Map.of("QueryNGramStringTest_2", 0, "QueryNGramStingWithSpecialCharactersTest_2", 0)),
        Arguments.of(
            AUTOCOMPLETE_OPERATOR_FIELD1.formatted(AUTOCOMPLETE_INDEX_NAME, "sTARt"),
            Map.of("QueryNGramStringTest_2", 0, "QueryNGramStingWithSpecialCharactersTest_2", 0)),
        Arguments.of(
            AUTOCOMPLETE_OPERATOR_FIELD1.formatted(AUTOCOMPLETE_INDEX_NAME, "start_"),
            Map.of("QueryNGramStingWithSpecialCharactersTest_2", 0)),
        Arguments.of(
            AUTOCOMPLETE_OPERATOR_FIELD1.formatted(AUTOCOMPLETE_INDEX_NAME, "start-"), Map.of()),
        Arguments.of(
            AUTOCOMPLETE_OPERATOR_FIELD1.formatted(AUTOCOMPLETE_INDEX_NAME, "start-4"), Map.of()),
        Arguments.of(
            AUTOCOMPLETE_OPERATOR_FIELD1.formatted(AUTOCOMPLETE_INDEX_NAME, "contains"),
            Map.of("QueryNGramStringTest_3", 0, "QueryNGramStingWithSpecialCharactersTest_3", 0)));
  }

  private static java.util.stream.Stream<Arguments>
      provideShouldReturnExpectedDocumentsWithCorrectOrderForSingleNgramLowercaseIndex() {
    return java.util.stream.Stream.of(
        Arguments.of(
            TEXT_OPERATOR_FIELD1.formatted(SINGLE_NGRAM_LOWERCASE_INDEX_NAME, "123"),
            Map.of(
                "QueryNGramStringTest_1",
                0,
                "QueryNGramStringTest_2",
                1,
                "QueryNGramStringTest_3",
                2)),
        Arguments.of(
            TEXT_OPERATOR_FIELD1.formatted(SINGLE_NGRAM_LOWERCASE_INDEX_NAME, "4-5"),
            Map.of(
                "QueryNGramStingWithSpecialCharactersTest_1",
                0,
                "QueryNGramStingWithSpecialCharactersTest_2",
                1,
                "QueryNGramStingWithSpecialCharactersTest_3",
                2)),
        Arguments.of(
            TEXT_OPERATOR_FIELD1.formatted(SINGLE_NGRAM_LOWERCASE_INDEX_NAME, "4_5"), Map.of()),
        Arguments.of(
            TEXT_OPERATOR_ALL_CRITERIA_MATCH_FIELD1.formatted(
                SINGLE_NGRAM_LOWERCASE_INDEX_NAME, "4_5"),
            Map.of()),
        Arguments.of(
            TEXT_OPERATOR_ALL_CRITERIA_MATCH_FIELD1.formatted(
                SINGLE_NGRAM_LOWERCASE_INDEX_NAME, "123"),
            Map.of(
                "QueryNGramStringTest_1",
                0,
                "QueryNGramStringTest_2",
                1,
                "QueryNGramStringTest_3",
                2)),
        Arguments.of(
            TEXT_OPERATOR_ALL_CRITERIA_MATCH_FIELD1.formatted(
                SINGLE_NGRAM_LOWERCASE_INDEX_NAME, "s123c"),
            Map.of("QueryNGramStringTest_3", 0)),
        Arguments.of(
            TEXT_OPERATOR_ALL_CRITERIA_MATCH_FIELD1.formatted(
                SINGLE_NGRAM_LOWERCASE_INDEX_NAME, "4-5"),
            Map.of(
                "QueryNGramStingWithSpecialCharactersTest_1",
                0,
                "QueryNGramStingWithSpecialCharactersTest_2",
                1,
                "QueryNGramStingWithSpecialCharactersTest_3",
                2)),
        Arguments.of(
            TEXT_OPERATOR_ALL_CRITERIA_MATCH_FIELD1.formatted(
                SINGLE_NGRAM_LOWERCASE_INDEX_NAME, "s4-5c"),
            Map.of("QueryNGramStingWithSpecialCharactersTest_3", 0)),
        Arguments.of(
            TEXT_OPERATOR_FIELD1.formatted(SINGLE_NGRAM_LOWERCASE_INDEX_NAME, "start123"),
            Map.of(
                "QueryNGramStringTest_1",
                2,
                "QueryNGramStringTest_2",
                0,
                "QueryNGramStringTest_3",
                3,
                "QueryNGramStingWithSpecialCharactersTest_2",
                1)),
        Arguments.of(
            TEXT_OPERATOR_FIELD1.formatted(SINGLE_NGRAM_LOWERCASE_INDEX_NAME, "START123"),
            // Incorrect case-sensitive
            Map.of(
                "QueryNGramStringTest_1",
                2,
                "QueryNGramStringTest_2",
                0,
                "QueryNGramStringTest_3",
                3,
                "QueryNGramStingWithSpecialCharactersTest_2",
                1)),
        Arguments.of(
            TEXT_OPERATOR_FIELD1.formatted(SINGLE_NGRAM_LOWERCASE_INDEX_NAME, "stART123"),
            // Incorrect case-sensitive
            Map.of(
                "QueryNGramStringTest_1",
                2,
                "QueryNGramStringTest_2",
                0,
                "QueryNGramStringTest_3",
                3,
                "QueryNGramStingWithSpecialCharactersTest_2",
                1)),
        Arguments.of(
            TEXT_OPERATOR_ALL_CRITERIA_MATCH_FIELD1.formatted(
                SINGLE_NGRAM_LOWERCASE_INDEX_NAME, "stART123"),
            // Correct case for CONTAINS !!!!!
            Map.of("QueryNGramStringTest_2", 0)),
        Arguments.of(
            TEXT_OPERATOR_FIELD1.formatted(SINGLE_NGRAM_LOWERCASE_INDEX_NAME, "sta"),
            Map.of("QueryNGramStringTest_2", 0, "QueryNGramStingWithSpecialCharactersTest_2", 1)),
        Arguments.of(
            TEXT_OPERATOR_FIELD1.formatted(SINGLE_NGRAM_LOWERCASE_INDEX_NAME, "start"),
            Map.of("QueryNGramStringTest_2", 0, "QueryNGramStingWithSpecialCharactersTest_2", 1)),
        Arguments.of(
            TEXT_OPERATOR_FIELD1.formatted(SINGLE_NGRAM_LOWERCASE_INDEX_NAME, "contains"),
            // Both documents has the same number of characters
            // contains123contains and contains4-5contains, that is why both the same score
            Map.of("QueryNGramStringTest_3", 0, "QueryNGramStingWithSpecialCharactersTest_3", 0)),
        Arguments.of(
            TEXT_OPERATOR_FIELD1.formatted(SINGLE_NGRAM_LOWERCASE_INDEX_NAME, "12"), Map.of()),
        Arguments.of(
            TEXT_OPERATOR_ALL_CRITERIA_MATCH_FIELD1.formatted(
                SINGLE_NGRAM_LOWERCASE_INDEX_NAME, "12"),
            Map.of()));
  }

  @ParameterizedTest
  @MethodSource("provideShouldReturnExpectedDocumentsWithCorrectOrderForKeywordIndex")
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
  public void shouldReturnExpectedDocumentsWithCorrectOrderForKeywordIndex(
      String searchQuery, Map<String, Integer> expectedIdsWithScoreIndex)
      throws InterruptedException {
    // Correct exact strategy
    // GIVEN
    MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
    MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
    ensureSearchKeyWordIndex(collection);
    waitForSearchIndexSync(collection, KEYWORD_INDEX_NAME);

    runTest(searchQuery, expectedIdsWithScoreIndex, collection);
  }

  @ParameterizedTest
  @MethodSource("provideShouldReturnExpectedDocumentsWithCorrectOrderForAutocompleteIndex")
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
  public void shouldReturnExpectedDocumentsWithCorrectOrderForAutocompleteIndex(
      String searchQuery, Map<String, Integer> expectedIdsWithScoreIndex)
      throws InterruptedException {
    // GIVEN
    MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
    MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
    ensureSearchAutocompleteIndex(collection);
    waitForSearchIndexSync(collection, AUTOCOMPLETE_INDEX_NAME);

    runTest(searchQuery, expectedIdsWithScoreIndex, collection);
  }

  @ParameterizedTest
  @MethodSource("provideShouldReturnExpectedDocumentsWithCorrectOrderForSingleNgramLowercaseIndex")
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
  public void shouldReturnExpectedDocumentsWithCorrectOrderForTextNgramLowercaseIndex(
      String searchQuery, Map<String, Integer> expectedIdsWithScoreIndex)
      throws InterruptedException {
    // GIVEN
    MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
    MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
    ensureSearchSingleNgramLowercaseIndex(collection);
    waitForSearchIndexSync(collection, SINGLE_NGRAM_LOWERCASE_INDEX_NAME);

    runTest(searchQuery, expectedIdsWithScoreIndex, collection);
  }

  private void waitForSearchIndexSync(MongoCollection<Document> collection, String indexName)
      throws InterruptedException {
    waitForSearchIndexSync(collection, indexName, "field1");
  }

  private void ensureSearchKeyWordIndex(MongoCollection<Document> collection) {
    ensureSearchIndexReady(KEYWORD_INDEX_NAME, KEYWORD_INDEX_DEF, collection);
  }

  private void ensureSearchAutocompleteIndex(MongoCollection<Document> collection) {
    ensureSearchIndexReady(AUTOCOMPLETE_INDEX_NAME, AUTOCOMPLETE_INDEX_DEF, collection);
  }

  private void ensureSearchSingleNgramLowercaseIndex(MongoCollection<Document> collection) {
    ensureSearchIndexReady(
        SINGLE_NGRAM_LOWERCASE_INDEX_NAME, SINGLE_NGRAM_LOWERCASE_INDEX_DEF, collection);
  }
}
