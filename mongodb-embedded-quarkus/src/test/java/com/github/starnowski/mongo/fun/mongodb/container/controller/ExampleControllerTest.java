package com.github.starnowski.mongo.fun.mongodb.container.controller;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.json.JSONException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

@QuarkusTest
class ExampleControllerTest {


    public static Stream<Arguments> provideShouldSaveExampleDocument() {
        return Stream.of(
                Arguments.of("examples/example1.json", "examples/example1.json")
        );
    }

    @ParameterizedTest
    @MethodSource({"provideShouldSaveExampleDocument"})
    public void shouldSaveExampleDocument(String requestFile, String expectedResponse) throws IOException, JSONException {
        ExtractableResponse<Response> response = given()
                .body(Files.readString(Paths.get(new File(getClass().getClassLoader().getResource(requestFile).getFile()).getPath())))
                .contentType(ContentType.JSON)
                .when()
                .post("/examples/")
                .then()
                .statusCode(200).extract();

        String responsePayload = response.asString();
        JSONAssert.assertEquals(Files.readString(Paths.get(new File(getClass().getClassLoader().getResource(expectedResponse).getFile()).getPath())), responsePayload, false);
    }
}