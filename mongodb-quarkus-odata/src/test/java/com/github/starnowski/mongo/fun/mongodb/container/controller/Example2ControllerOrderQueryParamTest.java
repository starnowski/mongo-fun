package com.github.starnowski.mongo.fun.mongodb.container.controller;

import com.github.starnowski.mongo.fun.mongodb.container.test.MongoDatabaseSetupExtension;
import com.github.starnowski.mongo.fun.mongodb.container.test.MongoDocument;
import com.github.starnowski.mongo.fun.mongodb.container.test.MongoSetup;
import com.mongodb.client.MongoClient;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.json.JSONException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

@QuarkusTest
@ExtendWith(MongoDatabaseSetupExtension.class)
class Example2ControllerOrderQueryParamTest {

    @Inject
    private MongoClient mongoClient;

    public static Stream<Arguments> provideShouldReturnBadRequestForInvalidPayload() {
        return Stream.of(
                Arguments.of("examples/invalid_request_example2.json", "examples/oas_response_example2.json")
        );
    }

    private static String prepareResponseForQueryWithPlainStringProperties(String... properties) {
        return """
                {
                  "results": [
                    %s
                  ],
                  "winningPlan": "COLLSCAN"
                
                }
                """
                .formatted(Stream.of(properties)
                        .map("""
                                        {
                                              "plainString": "%s"
                                        }
                                """::formatted
                        ).collect(Collectors.joining("\n,"))
                );
    }

    public static Stream<Arguments> provideShouldReturnResponseStringBasedOnFiltersExample2() {
        return Stream.of(
                /*
                 * Passing custom as literal value "normalize(''First example'')" because there is a problem with adding custom method
                 */
                Arguments.of(List.of("plainString asc"), prepareResponseForQueryWithPlainStringProperties("Oleksa", "example1", "example2")),
                Arguments.of(List.of("plainString desc"), prepareResponseForQueryWithPlainStringProperties("example2", "example1", "Oleksa"))
//                ,
        );
    }

    @PostConstruct
    public void init() {
    }


    @ParameterizedTest
    @MethodSource({
            "provideShouldReturnResponseStringBasedOnFiltersExample2"
    })
    @MongoSetup(mongoDocuments = {
            @MongoDocument(bsonFilePath = "examples/query/example2_5.json", collection = "examples"),
            @MongoDocument(bsonFilePath = "examples/query/example2_6.json", collection = "examples"),
            @MongoDocument(bsonFilePath = "examples/query/example2_7.json", collection = "examples")
    })
    public void shouldReturnResponseStringBasedOnFiltersExample2(List<String> orders, String expectedResponse) throws IOException, JSONException {
        // WHEN
        ExtractableResponse<Response> getResponse = given()
                .when()
                .queryParams(Map.of("$orderby", orders, "$select", "plainString"))
                .get("/examples2/simple-query")
                .then()
                .statusCode(200).extract();

        // THEN
        JSONAssert.assertEquals(expectedResponse, getResponse.asString(), true);
    }
}