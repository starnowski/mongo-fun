package com.github.starnowski.mongo.fun.mongodb.container.controller;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ExampleControllerTest {


    public static Stream<Arguments> provideShouldSaveExampleDocument() {
        return Stream.of(
                Arguments.of("examples/example1.json", "examples/example1.json")
        );
    }

    @ParameterizedTest
    @MethodSource({"provideShouldSaveExampleDocument"})
    public void shouldSaveExampleDocument(String requestFile, String expectedResponse) throws IOException {
        given()
                .body(Files.readString(Paths.get(new File(getClass().getClassLoader().getResource(requestFile).getFile()).getPath())))
                .contentType(ContentType.JSON)
                .when()
                .post("/examples/")
                .then()
                .statusCode(200)
                .body(is(Files.readString(Paths.get(new File(getClass().getClassLoader().getResource(expectedResponse).getFile()).getPath()))));
    }
}