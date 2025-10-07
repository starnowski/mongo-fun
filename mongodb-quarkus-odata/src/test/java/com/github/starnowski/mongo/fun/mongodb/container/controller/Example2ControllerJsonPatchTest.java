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
class Example2ControllerJsonPatchTest {

    private static final String ALL_EXAMPLES_IN_RESPONSE = prepareResponseForQueryWithPlainStringProperties("eOMtThyhVNLWUZNRcBaQKxI",
            "Some text",
            "Poem");
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

    public static Stream<Arguments> provideShouldReturnResponseBasedOnFilters() {
        return Stream.of(
                Arguments.of(List.of("plainString eq 'eOMtThyhVNLWUZNRcBaQKxI'"), "examples/query/responses/example2_1.json", "COLLSCAN"),
                Arguments.of(List.of("tolower(plainString) eq 'eomtthyhvnlwuznrcbaqkxi'"), "examples/query/responses/example2_1.json", "COLLSCAN"),
                Arguments.of(List.of("tolower(plainString) eq tolower('eOMtThyhVNLWUZNRcBaQKxI')"), "examples/query/responses/example2_1.json", "COLLSCAN"),
                Arguments.of(List.of("toupper(plainString) eq 'EOMTTHYHVNLWUZNRCBAQKXI'"), "examples/query/responses/example2_1.json", "COLLSCAN"),
                Arguments.of(List.of("plainString eq 'Some text'"), "examples/query/responses/example2_2.json", "COLLSCAN"),
                Arguments.of(List.of("startswith(plainString,'So')"), "examples/query/responses/example2_2.json", "COLLSCAN"),
                Arguments.of(Arrays.asList("startswith(plainString,'So')", "plainString eq 'Some text'"), "examples/query/responses/example2_2.json", "COLLSCAN"),
                Arguments.of(Arrays.asList("startswith(plainString,'Some t')", "smallInteger eq -1188957731"), "examples/query/responses/example2_2.json", "COLLSCAN"),
                Arguments.of(List.of("startswith(plainString,'Po') or smallInteger eq -113"), "examples/query/responses/example2_3.json", "COLLSCAN"),
                Arguments.of(Arrays.asList("timestamp ge 2024-07-20T10:00:00.00Z", "timestamp le 2024-07-20T20:00:00.00Z"), "examples/query/responses/example2_1.json", "COLLSCAN"),
                Arguments.of(Arrays.asList("plainString eq 'eOMtThyhVNLWUZNRcBaQKxI'", "uuidProp eq b921f1dd-3cbc-0495-fdab-8cd14d33f0aa"), "examples/query/responses/example2_1.json", "COLLSCAN"),
                Arguments.of(Arrays.asList("plainString eq 'eOMtThyhVNLWUZNRcBaQKxI'", "password eq 'password1'"), "examples/query/responses/example2_1.json", "COLLSCAN"),
                Arguments.of(List.of("plainString eq 'eOMtThyhVNLWUZNRcBaQKxI' or password eq 'password1'"), "examples/query/responses/example2_1.json", "COLLSCAN"),

                // Array with primitives
                // tags (String)
                Arguments.of(List.of("tags/any(t:t eq 'developer') or tags/any(t:t eq 'LLM')"), "examples/query/responses/example2_3.json", "COLLSCAN"),

                // String functions
                Arguments.of(List.of("tolower(plainString) eq 'poem'"), "examples/query/responses/example2_3.json", "COLLSCAN"),
                Arguments.of(List.of("toupper(plainString) eq 'POEM'"), "examples/query/responses/example2_3.json", "COLLSCAN"),
                Arguments.of(List.of("length(plainString) eq 4"), "examples/query/responses/example2_3.json", "COLLSCAN")
        );
    }

    public static Stream<Arguments> provideShouldReturnResponseBasedOnFiltersArrayTypes() {
        return Stream.of(
                // Array with primitives
                // tags (String)
                Arguments.of(List.of("tags/any(t:t eq 'developer') or tags/any(t:t eq 'LLM')"), "examples/query/responses/example2_3.json", "COLLSCAN"),
                Arguments.of(List.of("tags/any(t:startswith(t,'dev'))"), "examples/query/responses/example2_3.json", "COLLSCAN"),
                Arguments.of(List.of("tags/any(t:startswith(t,'dev') and length(t) eq 9)"), "examples/query/responses/example2_3.json", "COLLSCAN"),
                Arguments.of(List.of("tags/any(t:length(t) eq 13)"), "examples/query/responses/example2_1.json", "COLLSCAN"),
                Arguments.of(List.of("tags/any(t:tolower(t) eq 'developer')"), "examples/query/responses/example2_3.json", "COLLSCAN"),
                Arguments.of(List.of("tags/any(t:startswith(t,'spider') and endswith(t, 'web'))"), "examples/query/responses/example2_2.json", "COLLSCAN"),
                Arguments.of(List.of("tags/any(t:startswith(t,'spider') and t eq 'spiderweb')"), "examples/query/responses/example2_2.json", "COLLSCAN"),
                Arguments.of(List.of("tags/any(t:startswith(t,'spider') and t ne 'spiderweb')"), "examples/query/responses/example2_1.json", "COLLSCAN"),
                Arguments.of(List.of("tags/any(t:startswith(t,'spider') and t ne 'spiderweb' or endswith(t,'web') and t ne 'spiderweb')"), "examples/query/responses/example2_1.json", "COLLSCAN"),
                Arguments.of(List.of("tags/any(t:startswith(t,'spider') and t ne 'spiderweb' or endswith(t,'web') and t ne 'spiderweb' or contains(t,'wide') and t ne 'word wide')"), "examples/query/responses/example2_1.json", "COLLSCAN")
        );
    }

    public static Stream<Arguments> provideShouldReturnResponseStringBasedOnFilters() {
        return Stream.of(
                Arguments.of(List.of("uuidProp eq b921f1dd-3cbc-0495-fdab-8cd14d33f0aa"), ALL_EXAMPLES_IN_RESPONSE),
//                Arguments.of(List.of("trim(concat('  ', plainString, '  ')) eq 'Poem'"), ALL_EXAMPLES_IN_RESPONSE),
                Arguments.of(List.of("trim('   Poem   ') eq 'Poem'"), ALL_EXAMPLES_IN_RESPONSE),

// Date functions
                Arguments.of(List.of("year(birthDate) eq 2024"), ALL_EXAMPLES_IN_RESPONSE),
                Arguments.of(List.of("month(birthDate) eq 6"), ALL_EXAMPLES_IN_RESPONSE),
                Arguments.of(List.of("day(birthDate) eq 18"), ALL_EXAMPLES_IN_RESPONSE),
                Arguments.of(List.of("hour(timestamp) eq 10"), ALL_EXAMPLES_IN_RESPONSE),
                Arguments.of(List.of("minute(timestamp) eq 15"), ALL_EXAMPLES_IN_RESPONSE),
                Arguments.of(List.of("second(timestamp) eq 26"), ALL_EXAMPLES_IN_RESPONSE),

// Numeric functions
                Arguments.of(List.of("ceiling(floatValue) eq 1"), ALL_EXAMPLES_IN_RESPONSE),
                Arguments.of(List.of("floor(floatValue) eq 0"), ALL_EXAMPLES_IN_RESPONSE),
                Arguments.of(List.of("round(floatValue) eq 1"), ALL_EXAMPLES_IN_RESPONSE),
                // Arrays
                Arguments.of(List.of("tags/any(t:startswith(t,'spider') and t ne 'spiderweb' or startswith(t,'spider') and t ne 'spider' or contains(t,'wide') and t ne 'word wide')"), prepareResponseForQueryWithPlainStringProperties(
                        "Some text",
                        "eOMtThyhVNLWUZNRcBaQKxI")),
                Arguments.of(List.of("tags/any(t:startswith(t,'spider') and t ne 'spiderweb' or endswith(t,'web') and t ne 'spiderwebgg' or contains(t,'wide') and t ne 'word wide')"), prepareResponseForQueryWithPlainStringProperties(
                        "Some text",
                        "eOMtThyhVNLWUZNRcBaQKxI"))
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


    @Test
    public void shouldSaveAndPatchModelWithJsonPatchSpecification() throws IOException, JSONException {
        // GIVEN
        UUID uuid = UUID.randomUUID();

        // Convert to JSON string
        String json = """
                    {
                        "plainString": "test1"
                    }
                """;
        System.out.println("Request payload:");
        System.out.println(json);

        given()
                .body(json)
                .contentType(ContentType.JSON)
                .when()
                .post("/examples2/{id}", uuid)
                .then()
                .statusCode(200).extract();

        // WHEN
        given()
                .body("""
                        [
                          { "op": "add", "path": "/nestedObject/tokens/-", "value": "last token" }
                        ]
                        """)//doubleValue
                //{ "op": "add", "path": "/nestedObject/tokens/-", "value": "last token" }
                .contentType("application/json-patch+json")
                .when()
                .patch("/examples2/{id}", uuid)
                .then()
                .statusCode(400).extract();
    }

}