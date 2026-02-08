package com.github.starnowski.mongo.fun.mongodb.container.controller;

import static com.github.starnowski.mongo.fun.mongodb.container.AbstractITTest.TEST_DATABASE;
import static io.restassured.RestAssured.given;

import com.github.starnowski.mongo.fun.mongodb.container.test.MongoDatabaseSetupExtension;
import com.github.starnowski.mongo.fun.mongodb.container.test.MongoDocument;
import com.github.starnowski.mongo.fun.mongodb.container.test.MongoSetup;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.*;
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
class Example2ControllerWithAddedIndexesTest {

  @Inject private MongoClient mongoClient;

  public static Stream<Arguments> provideShouldReturnResponseBasedOnFilters() {
    return Stream.of(
        Arguments.of(
            List.of("plainString eq 'eOMtThyhVNLWUZNRcBaQKxI'"),
            "examples/query/responses/example2_1.json",
            "FETCH + IXSCAN"),
        Arguments.of(
            List.of("tolower(plainString) eq 'eomtthyhvnlwuznrcbaqkxi'"),
            "examples/query/responses/example2_1.json",
            "COLLSCAN"),
        Arguments.of(
            List.of("tolower(plainString) eq tolower('eOMtThyhVNLWUZNRcBaQKxI')"),
            "examples/query/responses/example2_1.json",
            "COLLSCAN"),
        Arguments.of(
            List.of("plainString eq 'Some text'"),
            "examples/query/responses/example2_2.json",
            "FETCH + IXSCAN"),
        Arguments.of(
            List.of("startswith(plainString,'So')"),
            "examples/query/responses/example2_2.json",
            "FETCH + IXSCAN"),
        Arguments.of(
            Arrays.asList("startswith(plainString,'So')", "plainString eq 'Some text'"),
            "examples/query/responses/example2_2.json",
            "FETCH + IXSCAN"),
        Arguments.of(
            Arrays.asList("startswith(plainString,'Some t')", "smallInteger eq -1188957731"),
            "examples/query/responses/example2_2.json",
            "FETCH + IXSCAN"),
        Arguments.of(
            List.of("startswith(plainString,'Po') or smallInteger eq -113"),
            "examples/query/responses/example2_3.json",
            "COLLSCAN"),
        Arguments.of(
            Arrays.asList(
                "timestamp ge 2024-07-20T10:00:00.00Z", "timestamp le 2024-07-20T20:00:00.00Z"),
            "examples/query/responses/example2_1.json",
            "COLLSCAN"),
        Arguments.of(
            Arrays.asList(
                "plainString eq 'eOMtThyhVNLWUZNRcBaQKxI'",
                "uuidProp eq b921f1dd-3cbc-0495-fdab-8cd14d33f0aa"),
            "examples/query/responses/example2_1.json",
            "FETCH + IXSCAN"),
        Arguments.of(
            Arrays.asList("plainString eq 'eOMtThyhVNLWUZNRcBaQKxI'", "password eq 'password1'"),
            "examples/query/responses/example2_1.json",
            "FETCH + IXSCAN"),
        Arguments.of(
            List.of("plainString eq 'eOMtThyhVNLWUZNRcBaQKxI' or password eq 'password1'"),
            "examples/query/responses/example2_1.json",
            "FETCH + IXSCAN"),
        Arguments.of(
            List.of("tags/any(t:t eq 'developer') or tags/any(t:t eq 'LLM')"),
            "examples/query/responses/example2_3.json",
            "COLLSCAN"));
  }

  @PostConstruct
  public void init() {}

  @BeforeEach
  public void createIndexes() {
    MongoCollection<Document> col =
        mongoClient.getDatabase(TEST_DATABASE).getCollection("examples");
    col.createIndex(new Document("plainString", 1));
    col.createIndex(new Document("password", 1));
  }

  @AfterEach
  public void removeIndexes() {
    MongoCollection<Document> col =
        mongoClient.getDatabase(TEST_DATABASE).getCollection("examples");
    col.dropIndex(new Document("plainString", 1));
    col.dropIndex(new Document("password", 1));
  }

  @ParameterizedTest
  @MethodSource({"provideShouldReturnResponseBasedOnFilters"})
  @MongoSetup(
      mongoDocuments = {
        @MongoDocument(bsonFilePath = "examples/query/example2_1.json", collection = "examples"),
        @MongoDocument(bsonFilePath = "examples/query/example2_2.json", collection = "examples"),
        @MongoDocument(bsonFilePath = "examples/query/example2_3.json", collection = "examples")
      })
  public void shouldReturnResponseWithFirstStageQueryPlannerWinningPlanBasedOnFilters(
      List<String> filters, String expectedResponseFilePath, String firstStageWinningPlan)
      throws IOException, JSONException {
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
