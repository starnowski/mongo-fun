package com.github.starnowski.mongo.fun.mongodb.container.controller;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;
import org.json.JSONException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;

@QuarkusTest
class ExampleControllerTest {

  public static Stream<Arguments> provideShouldSaveExampleDocument() {
    return Stream.of(
        Arguments.of("examples/example1.json", "examples/example1.json"),
        Arguments.of("examples/example2.json", "examples/example2.json"));
  }

  public static Stream<Arguments> provideShouldReturnBadRequestForInvalidPayload() {
    return Stream.of(Arguments.of("examples/invalid_request_example1.json"));
  }

  public static Stream<Arguments> provideShouldSaveExampleDocumentWithParameters() {
    return Stream.of(
        Arguments.of(
            "examples/example2_withQueryParams.json",
            Map.of("names", Arrays.asList("Simon", "Doug")),
            "examples/example2_withQueryParams.json"));
  }

  @ParameterizedTest
  @MethodSource({"provideShouldSaveExampleDocument"})
  public void shouldSaveExampleDocument(String requestFile, String expectedResponse)
      throws IOException, JSONException {
    ExtractableResponse<Response> response =
        given()
            .body(
                Files.readString(
                    Paths.get(
                        new File(getClass().getClassLoader().getResource(requestFile).getFile())
                            .getPath())))
            .contentType(ContentType.JSON)
            .when()
            .post("/examples/")
            .then()
            .statusCode(200)
            .extract();

    String responsePayload = response.asString();
    JSONAssert.assertEquals(
        Files.readString(
            Paths.get(
                new File(getClass().getClassLoader().getResource(expectedResponse).getFile())
                    .getPath())),
        responsePayload,
        false);
  }

  // provideShouldSaveExampleDocumentWithParameters

  @ParameterizedTest
  @MethodSource({"provideShouldSaveExampleDocumentWithParameters"})
  public void shouldSaveExampleDocumentWithQueryParams(
      String requestFile, Map queryParams, String expectedResponse)
      throws IOException, JSONException {
    ExtractableResponse<Response> response =
        given()
            .body(
                Files.readString(
                    Paths.get(
                        new File(getClass().getClassLoader().getResource(requestFile).getFile())
                            .getPath())))
            .contentType(ContentType.JSON)
            .queryParams(queryParams)
            .when()
            .post("/examples/withParams")
            .then()
            .statusCode(200)
            .extract();

    String responsePayload = response.asString();
    System.out.println("Response payload: " + responsePayload);
    JSONAssert.assertEquals(
        Files.readString(
            Paths.get(
                new File(getClass().getClassLoader().getResource(expectedResponse).getFile())
                    .getPath())),
        responsePayload,
        false);
  }

  @ParameterizedTest
  @MethodSource({"provideShouldReturnBadRequestForInvalidPayload"})
  public void shouldReturnBadRequestForInvalidPayload(String requestFile)
      throws IOException, JSONException {
    ExtractableResponse<Response> response =
        given()
            .body(
                Files.readString(
                    Paths.get(
                        new File(getClass().getClassLoader().getResource(requestFile).getFile())
                            .getPath())))
            .contentType(ContentType.JSON)
            .when()
            .post("/examples/")
            .then()
            .statusCode(400)
            .extract();
  }
}
