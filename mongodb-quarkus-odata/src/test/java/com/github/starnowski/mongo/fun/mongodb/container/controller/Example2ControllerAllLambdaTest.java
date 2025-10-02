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
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.bson.Document;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.api.Randomizer;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.starnowski.mongo.fun.mongodb.container.AbstractITTest.TEST_DATABASE;
import static io.restassured.RestAssured.given;

@QuarkusTest
@ExtendWith(MongoDatabaseSetupExtension.class)
class Example2ControllerAllLambdaTest {

    private static final String ALL_EXAMPLES_IN_RESPONSE = prepareResponseForQueryWithPlainStringProperties("eOMtThyhVNLWUZNRcBaQKxI",
            "Some text",
            "Poem",
            "Mario",
            "Oleksa");
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

    public static Stream<Arguments> provideShouldReturnResponseStringBasedOnFilters() {
        return Stream.of(
                Arguments.of(List.of("tags/all(t:t ne 'no such text' and t ne 'no such word')"), ALL_EXAMPLES_IN_RESPONSE),
                Arguments.of(List.of("tags/all(t:startswith(t,'star') and t ne 'starlord')"), prepareResponseForQueryWithPlainStringProperties("Mario"))
        );
    }

    @PostConstruct
    public void init() {
        javaTimeModule = new JavaTimeModule();

        // Custom serializer for OffsetDateTime
        javaTimeModule.addSerializer(OffsetDateTime.class, new com.fasterxml.jackson.databind.JsonSerializer<OffsetDateTime>() {
            @Override
            public void serialize(OffsetDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                // https://www.mongodb.com/docs/manual/reference/method/Date/
                /*
                 Internally, Mongod Date objects are stored as a signed 64-bit integer representing the number of milliseconds since the Unix epoch (Jan 1, 1970).
                 No microseconds or nanoseconds
                 */
                gen.writeString(value.truncatedTo(ChronoUnit.MILLIS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            }
        });
//        javaTimeModule.disable();
        // https://www.mongodb.com/docs/manual/reference/method/Date/
        EasyRandomParameters parameters = new EasyRandomParameters()
                .randomize(InputStream.class, new Randomizer<InputStream>() {
                    private final Random random = new Random();

                    @Override
                    public InputStream getRandomValue() {
                        byte[] bytes = new byte[16 + random.nextInt(64)]; // random size 16–80 bytes
                        random.nextBytes(bytes);
                        return new ByteArrayInputStream(bytes);
                    }
                })
                .randomize(Object.class, new Randomizer<Object>() {

                    private final Random random = new Random();

                    @Override
                    public Object getRandomValue() {
                        return random.nextInt();
                    }
                });
        generator = new EasyRandom(parameters);
        mapper = new ObjectMapper();
        mapper.registerModule(javaTimeModule);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @ParameterizedTest
    @MethodSource({
            "provideShouldReturnResponseStringBasedOnFilters"
    })
    @MongoSetup(mongoDocuments = {
            @MongoDocument(bsonFilePath = "examples/query/example2_1.json", collection = "examples"),
            @MongoDocument(bsonFilePath = "examples/query/example2_2.json", collection = "examples"),
            @MongoDocument(bsonFilePath = "examples/query/example2_3.json", collection = "examples"),
            @MongoDocument(bsonFilePath = "examples/query/example2_4.json", collection = "examples"),
            @MongoDocument(bsonFilePath = "examples/query/example2_5.json", collection = "examples")
    })
    public void provideShouldReturnResponseStringBasedOnFilters(List<String> filters, String expectedResponse) throws IOException, JSONException {
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