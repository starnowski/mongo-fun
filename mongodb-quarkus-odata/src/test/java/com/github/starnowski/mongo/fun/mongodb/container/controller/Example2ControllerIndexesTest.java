package com.github.starnowski.mongo.fun.mongodb.container.controller;

import static com.github.starnowski.mongo.fun.mongodb.container.AbstractITTest.TEST_DATABASE;
import static com.github.starnowski.mongo.fun.mongodb.container.controller.Example2ControllerComplexTypesTest.TEST_CASE_NESTED_OBJECT_TOKENS_ANY_T_T_EQ_FIRST_EXAMPLE_AND_NESTED_OBJECT_NUMBERS_ANY_T_T_GT_5_AND_T_LT_27;
import static io.restassured.RestAssured.given;

import com.github.starnowski.mongo.fun.mongodb.container.test.MongoDatabaseSetupExtension;
import com.github.starnowski.mongo.fun.mongodb.container.test.MongoDocument;
import com.github.starnowski.mongo.fun.mongodb.container.test.MongoSetup;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bson.Document;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;

@QuarkusTest
@ExtendWith(MongoDatabaseSetupExtension.class)
class Example2ControllerIndexesTest {

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

  public static Stream<Arguments> provideShouldReturnResponseStringBasedOnFiltersExample2() {
    return Stream.of(
        Arguments.of(
            TEST_CASE_NESTED_OBJECT_TOKENS_ANY_T_T_EQ_FIRST_EXAMPLE_AND_NESTED_OBJECT_NUMBERS_ANY_T_T_GT_5_AND_T_LT_27,
            "FETCH + IXSCAN"));
  }

  @BeforeEach
  public void createIndexes() {
    MongoCollection<Document> col =
        mongoClient.getDatabase(TEST_DATABASE).getCollection("examples");
    col.createIndex(new Document("plainString", 1));
    col.createIndex(new Document("password", 1));
    col.createIndex(new Document("nestedObject.tokens", 1));
    col.createIndex(new Document("nestedObject.numbers", 1));
  }

  @AfterEach
  public void removeIndexes() {
    MongoCollection<Document> col =
        mongoClient.getDatabase(TEST_DATABASE).getCollection("examples");
    col.dropIndex(new Document("plainString", 1));
    col.dropIndex(new Document("password", 1));
    col.dropIndex(new Document("nestedObject.tokens", 1));
    col.dropIndex(new Document("nestedObject.numbers", 1));
  }

  @ParameterizedTest
  @MethodSource({"provideShouldReturnResponseStringBasedOnFiltersExample2"})
  @MongoSetup(
      mongoDocuments = {
        @MongoDocument(bsonFilePath = "examples/query/example2_6.json", collection = "examples"),
        @MongoDocument(bsonFilePath = "examples/query/example2_7.json", collection = "examples")
      })
  public void shouldReturnResponseStringBasedOnFiltersExample2(
      List<String> filters, String firstStageWinningPlan) throws IOException, JSONException {
    // GIVEN
    String jsonWithWinningPlanProperty =
        """
                {
                    "winningPlan": "%s"
                }
                """
            .formatted(firstStageWinningPlan);

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
    JSONAssert.assertEquals(jsonWithWinningPlanProperty, getResponse.asString(), false);
  }
}
