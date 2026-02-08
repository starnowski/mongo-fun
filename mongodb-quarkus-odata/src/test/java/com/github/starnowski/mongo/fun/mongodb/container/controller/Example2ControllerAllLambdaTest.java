package com.github.starnowski.mongo.fun.mongodb.container.controller;

import static io.restassured.RestAssured.given;

import com.github.starnowski.mongo.fun.mongodb.container.test.MongoDatabaseSetupExtension;
import com.github.starnowski.mongo.fun.mongodb.container.test.MongoDocument;
import com.github.starnowski.mongo.fun.mongodb.container.test.MongoSetup;
import com.mongodb.client.MongoClient;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.json.JSONException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;

@QuarkusTest
@ExtendWith(MongoDatabaseSetupExtension.class)
class Example2ControllerAllLambdaTest extends AbstractExample2ControllerTest {

  private static final String ALL_EXAMPLES_IN_RESPONSE =
      prepareResponseForQueryWithPlainStringProperties(
          "eOMtThyhVNLWUZNRcBaQKxI", "Some text", "Poem", "Mario", "Oleksa");
  @Inject private MongoClient mongoClient;

  public static Stream<Arguments> provideShouldReturnBadRequestForInvalidPayload() {
    return Stream.of(
        Arguments.of(
            "examples/invalid_request_example2.json", "examples/oas_response_example2.json"));
  }

  private static String prepareResponseForQueryWithPlainStringProperties(String... properties) {
    return """
                {
                  "results": [
                    %s
                  ]
                }
                """
        .formatted(
            Stream.of(properties)
                .map(
                    """
                                        {
                                              "plainString": "%s"
                                        }
                                """
                        ::formatted)
                .collect(Collectors.joining("\n,")));
  }

  public static Stream<Arguments> provideShouldReturnResponseStringBasedOnFilters() {
    return Stream.of(
        Arguments.of(
            List.of("tags/all(t:t ne 'no such text' and t ne 'no such word')"),
            ALL_EXAMPLES_IN_RESPONSE),
        Arguments.of(
            List.of("tags/all(t:startswith(t,'star') and t ne 'starlord')"),
            prepareResponseForQueryWithPlainStringProperties("Mario")),
        Arguments.of(
            List.of("tags/all(t:startswith(t,'star') or t ne 'starlord')"),
            ALL_EXAMPLES_IN_RESPONSE),
        Arguments.of(
            List.of("tags/all(t:startswith(t,'star ') or t eq 'starlord')"),
            prepareResponseForQueryWithPlainStringProperties("Mario", "Oleksa")),
        Arguments.of(
            List.of("tags/all(t:startswith(t,'starlord') or t in ('star trek', 'star wars'))"),
            prepareResponseForQueryWithPlainStringProperties("Mario", "Oleksa")),
        Arguments.of(
            List.of(
                "tags/all(t:contains(t,'starlord') or contains(t,'trek') or contains(t,'wars'))"),
            prepareResponseForQueryWithPlainStringProperties("Mario", "Oleksa")),
        Arguments.of(
            List.of("tags/all(t:contains(t,'starlord'))"),
            prepareResponseForQueryWithPlainStringProperties()),
        // New test cases
        Arguments.of(
            List.of("tags/all(t:endswith(t,'web') or endswith(t,'trap'))"),
            prepareResponseForQueryWithPlainStringProperties("eOMtThyhVNLWUZNRcBaQKxI")),
        Arguments.of(
            List.of("tags/all(t:length(t) eq 9)"),
            prepareResponseForQueryWithPlainStringProperties("Mario")),
        Arguments.of(
            List.of("tags/all(t:contains(tolower(t),'star'))"),
            prepareResponseForQueryWithPlainStringProperties("Mario", "Oleksa")),
        Arguments.of(
            List.of("tags/all(t:contains(tolower(t),tolower('star')))"),
            prepareResponseForQueryWithPlainStringProperties("Mario", "Oleksa")),
        Arguments.of(
            List.of("tags/all(t:startswith(toupper(t),toupper('star')))"),
            prepareResponseForQueryWithPlainStringProperties("Mario", "Oleksa")),
        Arguments.of( // all documents matches
            List.of("tags/all(t:endswith(tolower(t),tolower(t)))"), ALL_EXAMPLES_IN_RESPONSE),
        Arguments.of(
            List.of("tags/all(t:contains(toupper(t),'STAR'))"),
            prepareResponseForQueryWithPlainStringProperties("Mario", "Oleksa")),
        Arguments.of(
            List.of("numericArray/all(n:n gt 5)"),
            prepareResponseForQueryWithPlainStringProperties(
                "eOMtThyhVNLWUZNRcBaQKxI", "Mario", "Oleksa")),
        // TODO numericArray/all(n:round(n) gt floor(5.05))
        Arguments.of(
            List.of("numericArray/all(n:n gt floor(5.05))"),
            prepareResponseForQueryWithPlainStringProperties(
                "eOMtThyhVNLWUZNRcBaQKxI", "Mario", "Oleksa")),
        Arguments.of(List.of("numericArray/all(n:n add 2 gt round(n))"), ALL_EXAMPLES_IN_RESPONSE),
        Arguments.of(
            List.of("numericArray/all(n:n eq 10 or n eq 20 or n eq 30)"),
            prepareResponseForQueryWithPlainStringProperties("eOMtThyhVNLWUZNRcBaQKxI")));
  }

  public static Stream<Arguments> provideShouldReturnResponseStringBasedOnComplexListFilters() {
    return Stream.of(
        Arguments.of(
            List.of("complexList/all(c:startswith(c/someString,'Ap'))"),
            prepareResponseForQueryWithPlainStringProperties("Doc1", "Doc5")),
        Arguments.of(
            List.of("complexList/all(c:contains(c/someString,'ana'))"),
            prepareResponseForQueryWithPlainStringProperties("Doc2")),
        Arguments.of(
            List.of("complexList/all(c:endswith(c/someString,'erry'))"),
            prepareResponseForQueryWithPlainStringProperties("Doc3")),
        Arguments.of(
            List.of("complexList/all(c:contains(c/someString,'e'))"),
            prepareResponseForQueryWithPlainStringProperties("Doc3", "Doc4")),
        Arguments.of(
            List.of("complexList/all(c:c/someString eq 'Application')"),
            prepareResponseForQueryWithPlainStringProperties("Doc5")));
  }

  public static Stream<Arguments> provideShouldReturnResponseStringBasedOnPipelines() {
    return Stream.of(
        //                Arguments.of("""
        //                            {
        //                                "pipeline": [
        //                                    {
        //                                    "$match": {
        //                                      "tags": {
        //                                        "$not": {
        //                                          "$elemMatch": {
        //                                            "$and": [
        //                                              { "$not": { "$regex": "/^star/" } },
        //                                              { "$eq": "starlord" }
        //                                            ]
        //                                          }
        //                                        }
        //                                      }
        //                                    }
        //                                  }
        //                                ]
        //                            }
        //                        """, ALL_EXAMPLES_IN_RESPONSE)
        //                ,
        // com.mongodb.MongoCommandException: Command failed with error 2 (BadValue): 'unknown
        // operator: $nor' on server localhost:27018. The full response is {"ok": 0.0, "errmsg":
        // "unknown operator: $nor", "code": 2, "codeName": "BadValue"}
        //                Arguments.of("""
        //                            {
        //                                "pipeline": [
        //                                    {
        //                                    "$match": {
        //                                      "tags": {
        //                                        "$nor": {
        //                                          "$elemMatch": {
        //                                            "$and": [
        //                                              { "$not": { "$regex": "/^star/" } },
        //                                              { "$eq": "starlord" }
        //                                            ]
        //                                          }
        //                                        }
        //                                      }
        //                                    }
        //                                  }
        //                                ]
        //                            }
        //                        """, ALL_EXAMPLES_IN_RESPONSE)
        // Caused by: com.mongodb.MongoCommandException: Command failed with error 2 (BadValue):
        // 'unknown top level operator: $not. If you are trying to negate an entire expression, use
        // $nor.' on server localhost:27018. The full response is {"ok": 0.0, "errmsg": "unknown top
        // level operator: $not. If you are trying to negate an entire expression, use $nor.",
        // "code": 2, "codeName": "BadValue"}
        //                Arguments.of("""
        //                            {
        //                            	"pipeline": [
        //                            		{
        //                            			"$match": {
        //                            				"$not": {
        //                            					"tags": {
        //                            						"$elemMatch": {
        //                            							"$and": [
        //                            								{
        //                            									"$not": {
        //                            										"$regex": "/^star/"
        //                            									}
        //                            								},
        //                            								{
        //                            									"$eq": "starlord"
        //                            								}
        //                            							]
        //                            						}
        //                            					}
        //                            				}
        //                            			}
        //                            		}
        //                            	]
        //                            }
        //                        """, ALL_EXAMPLES_IN_RESPONSE)
        // 2026-01-30 21:17:19,154 DEBUG [org.mon.dri.pro.command] (executor-thread-1) Command
        // "aggregate" failed on database "test" in 2.5556 ms using a connection with
        // driver-generated ID 3 and server-generated ID 3 to localhost:27018. The request ID is 11
        // and the operation ID is 11.: com.mongodb.MongoCommandException: Command failed with error
        // 2 (BadValue): 'unknown top level operator: $not. If you are trying to negate an entire
        // expression, use $nor.' on server localhost:27018. The full response is {"ok": 0.0,
        // "errmsg": "unknown top level operator: $not. If you are trying to negate an entire
        // expression, use $nor.", "code": 2, "codeName": "BadValue"}
        //                Arguments.of("""
        //                            {
        //                            	"pipeline": [
        //                            		{
        //                            			"$match": {
        //                            					"tags": {
        //                            						"$elemMatch": {
        //                            							"$and": [
        //                            								{
        //                            									"$not": {
        //                            										"$regex": "/^star/"
        //                            									}
        //                            								},
        //                            								{
        //                            									"$eq": "starlord"
        //                            								}
        //                            							]
        //                            						}
        //                            					}
        //                            				}
        //                            		}
        //                            	]
        //                            }
        //                        """, ALL_EXAMPLES_IN_RESPONSE)

        //                Arguments.of("""
        //                            {
        //                            	"pipeline": [
        //                            		{
        //                            			"$match": {
        //                            					"tags": {
        //                            						"$elemMatch": {
        //                            							"$and": [
        //                            								{
        //                            									"$regex": "/^star/"
        //                            								},
        //                            								{
        //                            									"$eq": "starlord"
        //                            								}
        //                            							]
        //                            						}
        //                            					}
        //                            				}
        //                            		}
        //                            	]
        //                            }
        //                        """, ALL_EXAMPLES_IN_RESPONSE)
        // Compile but invalid record
        //                Arguments.of("""
        //                            {
        //                            	"pipeline": [
        //                            		{
        //                            			"$match": {
        //                            					"tags": {
        //                            						"$elemMatch": {
        //                            							"or": [
        //                            								{
        //                            									"$regex": "/^star/"
        //                            								},
        //                            								{
        //                            									"$eq": "starlord"
        //                            								}
        //                            							]
        //                            						}
        //                            					}
        //                            				}
        //                            		}
        //                            	]
        //                            }
        //                        """, ALL_EXAMPLES_IN_RESPONSE)
        //                Arguments.of("""
        //                            {
        //                            	"pipeline": [
        //                            		{
        //                            			"$match": {
        //                            					"tags": {
        //                            						"$elemMatch": {
        //                            							"or": [
        //                            								{
        //                            									"$regex": "/^star/"
        //                            								},
        //                            								{
        //                            									"$ne": "starlord"
        //                            								}
        //                            							]
        //                            						}
        //                            					}
        //                            				}
        //                            		}
        //                            	]
        //                            }
        //                        """, ALL_EXAMPLES_IN_RESPONSE)

        Arguments.of(
            """
                            {
                            	"pipeline": [
                                           {
                                             "$match": {
                                               "$expr": {
                                                 "$allElementsTrue": {
                                                   "$map": {
                                                     "input": "$tags",
                                                     "as": "t",
                                                     "in": {
                                                       "$or": [
                                                         { "$regexMatch": { "input": "$$t", "regex": "/^star/" } },
                                                         { "$ne": ["$$t", "starlord"] }
                                                       ]
                                                     }
                                                   }
                                                 }
                                               }
                                             }
                                           }
                                         ]
                            }
                        """,
            prepareResponseForQueryWithPlainStringProperties(
                "eOMtThyhVNLWUZNRcBaQKxI", "Some text", "Poem", "Mario"))
        //                ,
        //
        //                Arguments.of(List.of("tags/all(t:startswith(t,'star') or t ne
        // 'starlord')"), ALL_EXAMPLES_IN_RESPONSE),
        //                Arguments.of(List.of("tags/all(t:startswith(t,'star ') or t eq
        // 'starlord')"), prepareResponseForQueryWithPlainStringProperties("Mario", "Oleksa")),
        //                Arguments.of(List.of("tags/all(t:startswith(t,'starlord') or t in ('star
        // trek', 'star wars'))"), prepareResponseForQueryWithPlainStringProperties("Mario",
        // "Oleksa")),
        //                Arguments.of(List.of("tags/all(t:contains(t,'starlord') or
        // contains(t,'trek') or contains(t,'wars'))"),
        // prepareResponseForQueryWithPlainStringProperties("Mario", "Oleksa")),
        //                Arguments.of(List.of("tags/all(t:contains(t,'starlord'))"),
        // prepareResponseForQueryWithPlainStringProperties())
        );
  }

  @ParameterizedTest
  @MethodSource({"provideShouldReturnResponseStringBasedOnFilters"})
  @MongoSetup(
      mongoDocuments = {
        @MongoDocument(bsonFilePath = "examples/query/example2_1.json", collection = "examples"),
        @MongoDocument(bsonFilePath = "examples/query/example2_2.json", collection = "examples"),
        @MongoDocument(bsonFilePath = "examples/query/example2_3.json", collection = "examples"),
        @MongoDocument(bsonFilePath = "examples/query/example2_4.json", collection = "examples"),
        @MongoDocument(bsonFilePath = "examples/query/example2_5.json", collection = "examples")
      })
  public void provideShouldReturnResponseStringBasedOnFilters(
      List<String> filters, String expectedResponse) throws IOException, JSONException {
    // WHEN
    ExtractableResponse<Response> getResponse =
        given()
            .when()
            .queryParams(Map.of("$filter", filters))
            .get("/examples2/simple-query")
            .then()
            .statusCode(200)
            .extract();

    // THEN
    JSONAssert.assertEquals(expectedResponse, getResponse.asString(), false);
  }

  @ParameterizedTest
  @MethodSource({"provideShouldReturnResponseStringBasedOnComplexListFilters"})
  @MongoSetup(
      mongoDocuments = {
        @MongoDocument(
            bsonFilePath = "examples/query/example2_complex_1.json",
            collection = "examples"),
        @MongoDocument(
            bsonFilePath = "examples/query/example2_complex_2.json",
            collection = "examples"),
        @MongoDocument(
            bsonFilePath = "examples/query/example2_complex_3.json",
            collection = "examples"),
        @MongoDocument(
            bsonFilePath = "examples/query/example2_complex_4.json",
            collection = "examples"),
        @MongoDocument(
            bsonFilePath = "examples/query/example2_complex_5.json",
            collection = "examples")
      })
  public void shouldReturnResponseStringBasedOnComplexListFilters(
      List<String> filters, String expectedResponse) throws IOException, JSONException {
    // WHEN
    ExtractableResponse<Response> getResponse =
        given()
            .when()
            .queryParams(Map.of("$filter", filters))
            .get("/examples2/simple-query")
            .then()
            .statusCode(200)
            .extract();

    // THEN
    JSONAssert.assertEquals(expectedResponse, getResponse.asString(), false);
  }

  public static Stream<Arguments>
      provideShouldReturnResponseStringBasedOnComplexListFiltersWithNumericProperties() {
    return Stream.of(
        Arguments.of(
            List.of("complexList/all(c:c/someNumber gt 5)"),
            prepareResponseForQueryWithPlainStringProperties(
                "Doc1", "Doc2", "Doc3", "Doc4", "Doc5")),
        Arguments.of(
            List.of("complexList/all(c:c/someNumber gt 25)"),
            prepareResponseForQueryWithPlainStringProperties("Doc2", "Doc3")),
        Arguments.of(
            List.of("complexList/all(c:c/someNumber lt 25)"),
            prepareResponseForQueryWithPlainStringProperties("Doc1", "Doc5")),
        Arguments.of(
            List.of("complexList/all(c:c/someNumber eq 10 or c/someNumber eq 20)"),
            prepareResponseForQueryWithPlainStringProperties("Doc1", "Doc5")),
        Arguments.of(
            List.of("complexList/all(c:c/someNumber add 5 gt 20)"),
            prepareResponseForQueryWithPlainStringProperties("Doc2", "Doc3", "Doc5")),
        Arguments.of(
            List.of("complexList/all(c:c/someNumber gt floor(5.05))"),
            prepareResponseForQueryWithPlainStringProperties(
                "Doc1", "Doc2", "Doc3", "Doc4", "Doc5")),
        Arguments.of(
            List.of("complexList/all(c:c/someNumber add 2 gt round(c/someNumber))"),
            prepareResponseForQueryWithPlainStringProperties(
                "Doc1", "Doc2", "Doc3", "Doc4", "Doc5")),
        Arguments.of(
            List.of("complexList/all(c:c/someNumber eq 20)"),
            prepareResponseForQueryWithPlainStringProperties("Doc5")));
  }

  @ParameterizedTest
  @MethodSource({"provideShouldReturnResponseStringBasedOnComplexListFiltersWithNumericProperties"})
  @MongoSetup(
      mongoDocuments = {
        @MongoDocument(
            bsonFilePath = "examples/query/example2_complex_1.json",
            collection = "examples"),
        @MongoDocument(
            bsonFilePath = "examples/query/example2_complex_2.json",
            collection = "examples"),
        @MongoDocument(
            bsonFilePath = "examples/query/example2_complex_3.json",
            collection = "examples"),
        @MongoDocument(
            bsonFilePath = "examples/query/example2_complex_4.json",
            collection = "examples"),
        @MongoDocument(
            bsonFilePath = "examples/query/example2_complex_5.json",
            collection = "examples")
      })
  public void shouldReturnResponseStringBasedOnComplexListFiltersWithNumericProperties(
      List<String> filters, String expectedResponse) throws IOException, JSONException {
    // WHEN
    ExtractableResponse<Response> getResponse =
        given()
            .when()
            .queryParams(Map.of("$filter", filters))
            .get("/examples2/simple-query")
            .then()
            .statusCode(200)
            .extract();

    // THEN
    JSONAssert.assertEquals(expectedResponse, getResponse.asString(), false);
  }

  public static Stream<Arguments>
      provideShouldReturnResponseStringBasedOnNestedComplexArrayFilters() {
    return Stream.of(
        Arguments.of(
            List.of("complexList/all(c:c/nestedComplexArray/all(n:n/stringVal eq 'val1'))"),
            prepareResponseForQueryWithPlainStringProperties("Doc2")),
        Arguments.of(
            List.of("complexList/all(c:c/nestedComplexArray/all(n:startswith(n/stringVal,'val')))"),
            prepareResponseForQueryWithPlainStringProperties("Doc1", "Doc2")),
        Arguments.of(
            List.of("complexList/all(c:c/nestedComplexArray/all(n:contains(n/stringVal,'match')))"),
            prepareResponseForQueryWithPlainStringProperties("Doc5")),
        Arguments.of(
            List.of(
                "complexList/all(c:c/nestedComplexArray/all(n:n/stringVal eq 'val1' or n/stringVal eq 'test1'))"),
            prepareResponseForQueryWithPlainStringProperties("Doc2", "Doc4")));
  }

  @ParameterizedTest
  @MethodSource({"provideShouldReturnResponseStringBasedOnNestedComplexArrayFilters"})
  @MongoSetup(
      mongoDocuments = {
        @MongoDocument(
            bsonFilePath = "examples/query/example2_complex_1.json",
            collection = "examples"),
        @MongoDocument(
            bsonFilePath = "examples/query/example2_complex_2.json",
            collection = "examples"),
        @MongoDocument(
            bsonFilePath = "examples/query/example2_complex_3.json",
            collection = "examples"),
        @MongoDocument(
            bsonFilePath = "examples/query/example2_complex_4.json",
            collection = "examples"),
        @MongoDocument(
            bsonFilePath = "examples/query/example2_complex_5.json",
            collection = "examples")
      })
  public void shouldReturnResponseStringBasedOnNestedComplexArrayFilters(
      List<String> filters, String expectedResponse) throws IOException, JSONException {
    // WHEN
    ExtractableResponse<Response> getResponse =
        given()
            .when()
            .queryParams(Map.of("$filter", filters))
            .get("/examples2/simple-query")
            .then()
            .statusCode(200)
            .extract();

    // THEN
    JSONAssert.assertEquals(expectedResponse, getResponse.asString(), false);
  }

  @ParameterizedTest
  @MethodSource({"provideShouldReturnResponseStringBasedOnPipelines"})
  @MongoSetup(
      mongoDocuments = {
        @MongoDocument(bsonFilePath = "examples/query/example2_1.json", collection = "examples"),
        @MongoDocument(bsonFilePath = "examples/query/example2_2.json", collection = "examples"),
        @MongoDocument(bsonFilePath = "examples/query/example2_3.json", collection = "examples"),
        @MongoDocument(bsonFilePath = "examples/query/example2_4.json", collection = "examples"),
        @MongoDocument(bsonFilePath = "examples/query/example2_5.json", collection = "examples")
      })
  public void provideShouldReturnResponseStringBasedOnPipelines(
      String json, String expectedResponse) throws IOException, JSONException {
    // WHEN
    ExtractableResponse<Response> getResponse =
        given().when().body(json).post("/examples2/simple-query").then().statusCode(200).extract();

    // THEN
    JSONAssert.assertEquals(expectedResponse, getResponse.asString(), false);
  }
}
