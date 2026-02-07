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
import java.util.List;
import java.util.Map;
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

  private static final String ALL_EXAMPLES_IN_RESPONSE =
      prepareResponseForQueryWithPlainStringProperties(
          "eOMtThyhVNLWUZNRcBaQKxI", "Some text", "Poem", "Mario", "Oleksa");
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

  public static Stream<Arguments> provideShouldReturnResponseStringBasedOnComplexListFilters() {
    return Stream.of(
        Arguments.of(
            List.of("complexList/any(c:startswith(c/someString,'Ap'))"),
            prepareResponseForQueryWithPlainStringProperties("Doc1")),
        Arguments.of(
            List.of("complexList/any(c:contains(c/someString,'ana'))"),
            prepareResponseForQueryWithPlainStringProperties("Doc2")),
        Arguments.of(
            List.of("complexList/any(c:endswith(c/someString,'erry'))"),
            prepareResponseForQueryWithPlainStringProperties("Doc3")),
        Arguments.of(
            List.of("complexList/any(c:contains(c/someString,'e'))"),
            prepareResponseForQueryWithPlainStringProperties("Doc1", "Doc3")));
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
}
