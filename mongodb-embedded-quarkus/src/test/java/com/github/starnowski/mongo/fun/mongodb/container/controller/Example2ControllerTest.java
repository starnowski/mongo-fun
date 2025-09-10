package com.github.starnowski.mongo.fun.mongodb.container.controller;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.UUIDDeserializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.OffsetDateTimeSerializer;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import jakarta.annotation.PostConstruct;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.api.Randomizer;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.UUID;

import static io.restassured.RestAssured.given;

@QuarkusTest
class Example2ControllerTest {

    private JavaTimeModule javaTimeModule;
    private EasyRandom generator;
    private ObjectMapper mapper;

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
                })
                ;
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
        ExtractableResponse<Response> response = given()
                .body(json)
                .contentType(ContentType.JSON)
                .when()
                .post("/examples2/{id}", uuid)
                .then()
                .statusCode(200).extract();

        // THEN
        response = given()
                .when()
                .get("/examples2/{id}", uuid)
                .then()
                .statusCode(200).extract();
        System.out.println("Response payload:");
        System.out.println(response.asPrettyString());
        JSONAssert.assertEquals(json, response.asString(), true);
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
    public void shouldSaveAndPatchModel() throws IOException, JSONException {
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
        ExtractableResponse<Response> patchResponse = given()
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
}