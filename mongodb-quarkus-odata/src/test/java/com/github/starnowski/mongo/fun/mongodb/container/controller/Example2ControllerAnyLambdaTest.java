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
class Example2ControllerAnyLambdaTest extends AbstractExample2ControllerTest {

  @Inject private MongoClient mongoClient;

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
            List.of("tags/any(t:t eq 'word wide web')"),
            prepareResponseForQueryWithPlainStringProperties("eOMtThyhVNLWUZNRcBaQKxI")),
        Arguments.of(
            List.of("tags/any(t:startswith(t,'star'))"),
            prepareResponseForQueryWithPlainStringProperties("Mario", "Oleksa")),
        Arguments.of(
            List.of("tags/any(t:contains(t,'spider'))"),
            prepareResponseForQueryWithPlainStringProperties(
                "eOMtThyhVNLWUZNRcBaQKxI", "Some text")),
        Arguments.of(
            List.of("numericArray/any(n:n gt 25)"),
            prepareResponseForQueryWithPlainStringProperties(
                "eOMtThyhVNLWUZNRcBaQKxI", "Mario", "Oleksa")),
        Arguments.of(
            List.of("numericArray/any(n:n lt 10)"),
            prepareResponseForQueryWithPlainStringProperties("Some text", "Poem")),
        Arguments.of(
            List.of("tags/any(t:contains(tolower(t),'star'))"),
            prepareResponseForQueryWithPlainStringProperties("Mario", "Oleksa")),
        Arguments.of(
            List.of("tags/any(t:endswith(toupper(t),'TRAP'))"),
            prepareResponseForQueryWithPlainStringProperties("eOMtThyhVNLWUZNRcBaQKxI")),
        Arguments.of(
            List.of("tags/any(t:length(t) eq 8)"),
            prepareResponseForQueryWithPlainStringProperties("Some text", "Poem", "Oleksa")),
        Arguments.of(
            List.of("numericArray/any(n:n add 2 gt 100)"),
            prepareResponseForQueryWithPlainStringProperties("Mario")),
        Arguments.of(
            List.of("tags/any(t:t ne 'no such text' and t ne 'no such word')"),
            prepareResponseForQueryWithPlainStringProperties(
                "eOMtThyhVNLWUZNRcBaQKxI", "Some text", "Poem", "Mario", "Oleksa")),
        Arguments.of(
            List.of("tags/any(t:startswith(t,'star') and t ne 'starlord')"),
            prepareResponseForQueryWithPlainStringProperties("Mario", "Oleksa")),
        Arguments.of(
            List.of("tags/any(t:startswith(t,'star') or t ne 'starlord')"),
            prepareResponseForQueryWithPlainStringProperties(
                "eOMtThyhVNLWUZNRcBaQKxI", "Some text", "Poem", "Mario", "Oleksa")),
        Arguments.of(
            List.of("tags/any(t:startswith(t,'star ') or t eq 'starlord')"),
            prepareResponseForQueryWithPlainStringProperties("Mario", "Oleksa")),
        Arguments.of(
            List.of("tags/any(t:startswith(t,'starlord') or t in ('star trek', 'star wars'))"),
            prepareResponseForQueryWithPlainStringProperties("Mario", "Oleksa")),
        Arguments.of(
            List.of(
                "tags/any(t:contains(t,'starlord') or contains(t,'trek') or contains(t,'wars'))"),
            prepareResponseForQueryWithPlainStringProperties("Mario", "Oleksa")),
        Arguments.of(
            List.of("tags/any(t:contains(t,'starlord'))"),
            prepareResponseForQueryWithPlainStringProperties("Oleksa")),
        Arguments.of(
            List.of("tags/any(t:endswith(t,'web') or endswith(t,'trap'))"),
            prepareResponseForQueryWithPlainStringProperties(
                "eOMtThyhVNLWUZNRcBaQKxI", "Some text")),
        Arguments.of(
            List.of("tags/any(t:length(t) eq 9)"),
            prepareResponseForQueryWithPlainStringProperties("Some text", "Poem", "Mario", "Oleksa")),
        Arguments.of(
            List.of("numericArray/any(n:n gt 5)"),
            prepareResponseForQueryWithPlainStringProperties(
                "eOMtThyhVNLWUZNRcBaQKxI", "Some text", "Mario", "Oleksa")),
        Arguments.of(
            List.of("numericArray/any(n:n gt floor(5.05))"),
            prepareResponseForQueryWithPlainStringProperties(
                "eOMtThyhVNLWUZNRcBaQKxI", "Some text", "Mario", "Oleksa")),
        Arguments.of(
            List.of("numericArray/any(n:n add 2 gt round(n))"),
            prepareResponseForQueryWithPlainStringProperties(
                "eOMtThyhVNLWUZNRcBaQKxI", "Some text", "Poem", "Mario", "Oleksa")),
        Arguments.of(
            List.of("numericArray/any(n:n eq 10 or n eq 20 or n eq 30)"),
            prepareResponseForQueryWithPlainStringProperties("eOMtThyhVNLWUZNRcBaQKxI")));
  }

  public static Stream<Arguments> provideShouldReturnResponseStringBasedOnComplexListFilters() {
    return Stream.of(
        Arguments.of(
            List.of("complexList/any(c:c/someString eq 'Apple')"),
            prepareResponseForQueryWithPlainStringProperties("Doc1", "Doc4")),
        Arguments.of(
            List.of("complexList/any(c:c/someNumber gt 35)"),
            prepareResponseForQueryWithPlainStringProperties("Doc2", "Doc3", "Doc4", "Doc6")),
        Arguments.of(
            List.of("complexList/any(c:c/someString eq 'Banana' or c/someString eq 'Cherry')"),
            prepareResponseForQueryWithPlainStringProperties("Doc2", "Doc3")),
        Arguments.of(
            List.of("complexList/any(c:c/nestedComplexArray/any(n:n/stringVal eq 'val1'))"),
            prepareResponseForQueryWithPlainStringProperties("Doc1", "Doc2", "Doc4")),
        Arguments.of(
            List.of("complexList/any(c:startswith(c/someString,'Ap'))"),
            prepareResponseForQueryWithPlainStringProperties("Doc1", "Doc4", "Doc5")),
        Arguments.of(
            List.of("complexList/any(c:contains(c/someString,'ana'))"),
            prepareResponseForQueryWithPlainStringProperties("Doc2")),
        Arguments.of(
            List.of("complexList/any(c:endswith(c/someString,'erry'))"),
            prepareResponseForQueryWithPlainStringProperties("Doc3", "Doc4")),
        Arguments.of(
            List.of("complexList/any(c:contains(c/someString,'e'))"),
            prepareResponseForQueryWithPlainStringProperties("Doc1", "Doc3", "Doc4", "Doc6")),
        Arguments.of(
            List.of("complexList/any(c:c/someString eq 'Application')"),
            prepareResponseForQueryWithPlainStringProperties("Doc1", "Doc5")));
  }

  @ParameterizedTest
  @MethodSource({"provideShouldReturnResponseStringBasedOnFilters"})
  @MongoSetup(
      mongoDocuments = {
        @MongoDocument(bsonFilePath = "examples/query/example2_1.json", collection = "examples"),
        @MongoDocument(bsonFilePath = "examples/query/example2_2.json", collection = "examples"),
        @MongoDocument(bsonFilePath = "examples/query/example2_3.json", collection = "examples"),
        @MongoDocument(bsonFilePath = "examples/query/example2_4.json", collection = "examples"),
        @MongoDocument(bsonFilePath = "examples/query/example2_5.json", collection = "examples"),
        @MongoDocument(
            bsonFilePath = "examples/query/example2_only_id.json",
            collection = "examples")
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
            collection = "examples"),
        @MongoDocument(
            bsonFilePath = "examples/query/example2_complex_6.json",
            collection = "examples"),
        @MongoDocument(
            bsonFilePath = "examples/query/example2_only_id.json",
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
            List.of("complexList/any(c:c/someNumber gt 5)"),
            prepareResponseForQueryWithPlainStringProperties(
                "Doc1", "Doc2", "Doc3", "Doc4", "Doc5", "Doc6")),
        Arguments.of(
            List.of("complexList/any(c:c/someNumber gt 25)"),
            prepareResponseForQueryWithPlainStringProperties("Doc2", "Doc3", "Doc4", "Doc6")),
        Arguments.of(
            List.of("complexList/any(c:c/someNumber lt 25)"),
            prepareResponseForQueryWithPlainStringProperties("Doc1", "Doc4", "Doc5")),
        Arguments.of(
            List.of("complexList/any(c:c/someNumber eq 10 or c/someNumber eq 20)"),
            prepareResponseForQueryWithPlainStringProperties("Doc1", "Doc4", "Doc5")),
        Arguments.of(
            List.of("complexList/any(c:c/someNumber add 5 gt 20)"),
            prepareResponseForQueryWithPlainStringProperties(
                "Doc1", "Doc2", "Doc3", "Doc4", "Doc5", "Doc6")),
        Arguments.of(
            List.of("complexList/any(c:c/someNumber gt floor(5.05))"),
            prepareResponseForQueryWithPlainStringProperties(
                "Doc1", "Doc2", "Doc3", "Doc4", "Doc5", "Doc6")),
        Arguments.of(
            List.of("complexList/any(c:c/someNumber add 2 gt round(c/someNumber))"),
            prepareResponseForQueryWithPlainStringProperties(
                "Doc1", "Doc2", "Doc3", "Doc4", "Doc5", "Doc6")),
        Arguments.of(
            List.of("complexList/any(c:c/someNumber eq 20)"),
            prepareResponseForQueryWithPlainStringProperties("Doc1", "Doc5")));
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
            collection = "examples"),
        @MongoDocument(
            bsonFilePath = "examples/query/example2_complex_6.json",
            collection = "examples"),
        @MongoDocument(
            bsonFilePath = "examples/query/example2_only_id.json",
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
            List.of("complexList/any(c:c/nestedComplexArray/any(n:n/stringVal eq 'val1'))"),
            prepareResponseForQueryWithPlainStringProperties("Doc1", "Doc2", "Doc4")),
        Arguments.of(
            List.of("complexList/any(c:c/nestedComplexArray/any(n:startswith(n/stringVal,'val')))"),
            prepareResponseForQueryWithPlainStringProperties("Doc1", "Doc2", "Doc4")),
        Arguments.of(
            List.of("complexList/any(c:c/nestedComplexArray/any(n:contains(n/stringVal,'match')))"),
            prepareResponseForQueryWithPlainStringProperties("Doc5", "Doc6")),
        Arguments.of(
            List.of(
                "complexList/any(c:c/nestedComplexArray/any(n:n/stringVal eq 'val1' or n/stringVal eq 'test1'))"),
            prepareResponseForQueryWithPlainStringProperties("Doc1", "Doc2", "Doc3", "Doc4")),
        Arguments.of(
            List.of(
                "complexList/any(c:c/nestedComplexArray/any(n:n/stringVal eq 'val1' or n/stringVal eq 'test1') and c/someNumber ge 20)"),
            prepareResponseForQueryWithPlainStringProperties("Doc2", "Doc3", "Doc4")),
        Arguments.of(
            List.of("complexList/any(c:c/nestedComplexArray/any(n:n/numberVal gt 70))"),
            prepareResponseForQueryWithPlainStringProperties("Doc6")),
        Arguments.of(
            List.of(
                "complexList/any(c:c/nestedComplexArray/any(n:n/numberVal eq 71) and c/nestedComplexArray/any(n:n/numberVal eq 72))"),
            prepareResponseForQueryWithPlainStringProperties("Doc6")),
        Arguments.of(
            List.of("complexList/any(c:c/nestedComplexArray/$count ge 2)"),
            prepareResponseForQueryWithPlainStringProperties("Doc6")),
        Arguments.of(
            List.of("complexList/any(c:c/primitiveStringList/any(n:startswith(n,'item11')))"),
            prepareResponseForQueryWithPlainStringProperties("Doc6")),
        Arguments.of(
            List.of("complexList/any(c:c/primitiveNumberList/any(n:n gt 10))"),
            prepareResponseForQueryWithPlainStringProperties("Doc6")));
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
            collection = "examples"),
        @MongoDocument(
            bsonFilePath = "examples/query/example2_complex_6.json",
            collection = "examples"),
        @MongoDocument(
            bsonFilePath = "examples/query/example2_only_id.json",
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
}
