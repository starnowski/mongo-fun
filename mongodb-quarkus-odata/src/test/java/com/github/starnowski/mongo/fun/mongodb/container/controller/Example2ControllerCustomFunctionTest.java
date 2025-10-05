package com.github.starnowski.mongo.fun.mongodb.container.controller;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.bson.Document;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.api.Randomizer;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.starnowski.mongo.fun.mongodb.container.AbstractITTest.TEST_DATABASE;
import static com.github.starnowski.mongo.fun.mongodb.container.controller.Example2ControllerComplexTypesTest.TEST_CASE_NESTED_OBJECT_TOKENS_ANY_T_T_EQ_FIRST_EXAMPLE_AND_NESTED_OBJECT_NUMBERS_ANY_T_T_GT_5_AND_T_LT_27;
import static io.restassured.RestAssured.given;

@QuarkusTest
@ExtendWith(MongoDatabaseSetupExtension.class)
class Example2ControllerCustomFunctionTest {

    private static final String ALL_EXAMPLES_IN_RESPONSE = prepareResponseForQueryWithPlainStringProperties("eOMtThyhVNLWUZNRcBaQKxI",
            "Some text",
            "Poem",
            "Mario",
            "Oleksa");
    public static final List<String> TEST_CASE_NESTED_OBJECT_TOKENS_ANY_T_T_EQ_FIRST_EXAMPLE_AND_NESTED_OBJECT_NUMBERS_ANY_T_T_GT_5_AND_T_LT_27 = List.of("nestedObject/tokens/any(t:t eq 'first example') and nestedObject/numbers/any(t:t gt 5 and t lt 27)");
    private JavaTimeModule javaTimeModule;
    private EasyRandom generator;
    private ObjectMapper mapper;
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
                  ]
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
                Arguments.of(List.of("nestedObject/tokens/any(t:t eq 'normalize(''First example'')')"), prepareResponseForQueryWithPlainStringProperties("example1"))
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
            @MongoDocument(bsonFilePath = "examples/query/example2_6.json", collection = "examples"),
            @MongoDocument(bsonFilePath = "examples/query/example2_7.json", collection = "examples")
    })
    public void shouldReturnResponseStringBasedOnFiltersExample2(List<String> filters, String expectedResponse) throws IOException, JSONException {
        // WHEN
        ExtractableResponse<Response> getResponse = given()
                .when()
                .queryParams(Map.of("$filter", filters))
                .get("/examples2/simple-query")
                .then()
                .statusCode(200).extract();

        // THEN
        JSONAssert.assertEquals(expectedResponse, getResponse.asString(), false);
    }
}