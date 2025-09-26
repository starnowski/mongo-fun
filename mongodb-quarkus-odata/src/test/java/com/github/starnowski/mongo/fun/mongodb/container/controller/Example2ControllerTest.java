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
class Example2ControllerTest {

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
                Arguments.of(List.of("tags/any(t:startswith(t,'dev') and length(t) eq 9)"), "examples/query/responses/example2_3.json", "COLLSCAN")
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
                Arguments.of(List.of("round(floatValue) eq 1"), ALL_EXAMPLES_IN_RESPONSE)
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
    public void shouldSaveAndReturnTheSameGeneratedExample() throws IOException, JSONException {
        // GIVEN
        Example2Dto dto = generator.nextObject(Example2Dto.class);

        //TODO temporary erasing binary field
        dto.setFileUpload(null);

        // Convert to JSON string
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dto);
        System.out.println("Request payload:");
        System.out.println(json);

        // WHEN
        ExtractableResponse<Response> response = given()
                .body(json)
                .contentType(ContentType.JSON)
                .when()
                .post("/examples2/")
                .then()
                .statusCode(200).extract();

        // THEN
        System.out.println("Response payload:");
        System.out.println(response.asPrettyString());
        JSONAssert.assertEquals(json, response.asString(), true);
    }

    @Test
    public void shouldSaveAndReturnTheSameGeneratedExampleForGetByIdEndpoint() throws IOException, JSONException {
        // GIVEN
        UUID uuid = UUID.randomUUID();
        Example2Dto dto = generator.nextObject(Example2Dto.class);

        //TODO temporary erasing binary field
        dto.setFileUpload(null);

        // Convert to JSON string
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dto);
        System.out.println("Request payload:");
        System.out.println(json);

        // WHEN
        given()
                .body(json)
                .contentType(ContentType.JSON)
                .when()
                .post("/examples2/{id}", uuid)
                .then()
                .statusCode(200).extract();

        // THEN
        ExtractableResponse<Response> response = given()
                .when()
                .get("/examples2/{id}", uuid)
                .then()
                .statusCode(200).extract();
        System.out.println("Response payload:");
        System.out.println(response.asPrettyString());
        JSONAssert.assertEquals(json, response.asString(), true);

        // Print MongoDB document in BSON format
        Document document = mongoClient.getDatabase(TEST_DATABASE).getCollection("examples").find(new Document("_id", uuid)).first();
        System.out.println("BSON Document : " + document.toJson());
    }

    //shouldSaveAndReturnBadRequestWhenTryingToInsertDocumentWithSameId

    @Test
    public void shouldSaveAndReturnBadRequestWhenTryingToInsertDocumentWithSameId() throws IOException, JSONException {
        // GIVEN
        UUID uuid = UUID.randomUUID();
        Example2Dto dto = generator.nextObject(Example2Dto.class);

        //TODO temporary erasing binary field
        dto.setFileUpload(null);

        // Convert to JSON string
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dto);
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
        ExtractableResponse<Response> response = given()
                .body(json)
                .contentType(ContentType.JSON)
                .when()
                .post("/examples2/{id}", uuid)
                .then()
                .statusCode(400).extract();

    }

    @Test
    public void shouldSaveAndPatchModelWithJsonPatchSpecification() throws IOException, JSONException {
        // GIVEN
        UUID uuid = UUID.randomUUID();
        Example2Dto dto = generator.nextObject(Example2Dto.class);

        //TODO temporary erasing binary field
        dto.setFileUpload(null);

        // Convert to JSON string
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dto);
        System.out.println("Request payload:");
        System.out.println(json);

        ExtractableResponse<Response> postResponse = given()
                .body(json)
                .contentType(ContentType.JSON)
                .when()
                .post("/examples2/{id}", uuid)
                .then()
                .statusCode(200).extract();

        dto.setBirthDate(LocalDate.of(1973, 7, 3));
        String expectedJsonAfterPath = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dto);

        // WHEN
        given()
                .body("""
                        [
                          { "op": "replace", "path": "/birthDate", "value": "1973-07-03" }
                        ]
                        """)
                .contentType("application/json-patch+json")
                .when()
                .patch("/examples2/{id}", uuid)
                .then()
                .statusCode(200).extract();

        // THEN
        ExtractableResponse<Response> getResponse = given()
                .when()
                .get("/examples2/{id}", uuid)
                .then()
                .statusCode(200).extract();
        System.out.println("Response payload:");
        System.out.println(getResponse.asPrettyString());
        JSONAssert.assertEquals(expectedJsonAfterPath, getResponse.asString(), true);
    }

    @Test
    public void shouldSaveAndPatchModelWithMergePatchSpecification() throws IOException, JSONException {
        // GIVEN
        UUID uuid = UUID.randomUUID();
        Example2Dto dto = generator.nextObject(Example2Dto.class);

        //TODO temporary erasing binary field
        dto.setFileUpload(null);
        Map<String, Object> innerObject = new HashMap<>();
        innerObject.put("prop1", "val3");
        innerObject.put("prop2", "val19");
        dto.getMetadata().put("innerObject", innerObject);

        // Convert to JSON string
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dto);
        System.out.println("Request payload:");
        System.out.println(json);
        given()
                .body(json)
                .contentType(ContentType.JSON)
                .when()
                .post("/examples2/{id}", uuid)
                .then()
                .statusCode(200).extract();

        innerObject.put("prop2", 1987);
        String expectedJsonAfterPath = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dto);

        // WHEN
        given()
                .body("""
                        {
                            "metadata": {
                                "innerObject": {
                                    "prop2": 1987
                                }
                            }
                        }
                        """)
                .contentType("application/merge-patch+json")
                .when()
                .patch("/examples2/{id}", uuid)
                .then()
                .statusCode(200).extract();

        // THEN
        ExtractableResponse<Response> getResponse = given()
                .when()
                .get("/examples2/{id}", uuid)
                .then()
                .statusCode(200).extract();
        System.out.println("Response payload:");
        System.out.println(getResponse.asPrettyString());
        JSONAssert.assertEquals(expectedJsonAfterPath, getResponse.asString(), true);
    }

    @ParameterizedTest
    @MethodSource({"provideShouldReturnBadRequestForInvalidPayload"})
    public void shouldReturnBadRequestForInvalidPayload(String requestFile, String expectedResponseFilePath) throws IOException, JSONException {
        // GIVEN
        String expectedResponse = Files.readString(Paths.get(new File(getClass().getClassLoader().getResource(expectedResponseFilePath).getFile()).getPath()));

        // WHEN
        ExtractableResponse<Response> response = given()
                .body(Files.readString(Paths.get(new File(getClass().getClassLoader().getResource(requestFile).getFile()).getPath())))
                .contentType(ContentType.JSON)
                .when()
                .post("/examples2/")
                .then()
                .statusCode(400).extract();

        // THEN
        JSONAssert.assertEquals(expectedResponse, response.asString(), false);
    }

    @ParameterizedTest
    @MethodSource({
            "provideShouldReturnResponseBasedOnFilters",
            "provideShouldReturnResponseBasedOnFiltersArrayTypes"

    })
    @MongoSetup(mongoDocuments = {
            @MongoDocument(bsonFilePath = "examples/query/example2_1.json", collection = "examples"),
            @MongoDocument(bsonFilePath = "examples/query/example2_2.json", collection = "examples"),
            @MongoDocument(bsonFilePath = "examples/query/example2_3.json", collection = "examples")
    })
    public void shouldReturnResponseBasedOnFilters(List<String> filters, String expectedResponseFilePath) throws IOException, JSONException {
        // GIVEN
        String expectedResponse = Files.readString(Paths.get(new File(getClass().getClassLoader().getResource(expectedResponseFilePath).getFile()).getPath()));

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

    @ParameterizedTest
    @MethodSource({
            "provideShouldReturnResponseStringBasedOnFilters"
    })
    @MongoSetup(mongoDocuments = {
            @MongoDocument(bsonFilePath = "examples/query/example2_1.json", collection = "examples"),
            @MongoDocument(bsonFilePath = "examples/query/example2_2.json", collection = "examples"),
            @MongoDocument(bsonFilePath = "examples/query/example2_3.json", collection = "examples")
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

    @ParameterizedTest
    @MethodSource({"provideShouldReturnResponseBasedOnFilters", "provideShouldReturnResponseBasedOnFiltersArrayTypes"})
    @MongoSetup(mongoDocuments = {
            @MongoDocument(bsonFilePath = "examples/query/example2_1.json", collection = "examples"),
            @MongoDocument(bsonFilePath = "examples/query/example2_2.json", collection = "examples"),
            @MongoDocument(bsonFilePath = "examples/query/example2_3.json", collection = "examples")
    })
    public void shouldReturnResponseWithFirstStageQueryPlannerWinningPlanBasedOnFilters(List<String> filters, String expectedResponseFilePath, String firstStageWinningPlan) throws IOException, JSONException {
        // GIVEN
        String jsonWithWinningPlanProperty = """
                {
                    "winningPlan": "%s"
                }
                """.formatted(firstStageWinningPlan);
        // WHEN
        ExtractableResponse<Response> getResponse = given()
                .when()
                .queryParams(Map.of("$filter", filters))
                .get("/examples2/simple-query")
                .then()
                .statusCode(200).extract();

        // THEN
        JSONAssert.assertEquals(jsonWithWinningPlanProperty, getResponse.asString(), false);
    }

}