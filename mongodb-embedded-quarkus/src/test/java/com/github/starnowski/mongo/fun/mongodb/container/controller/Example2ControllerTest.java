package com.github.starnowski.mongo.fun.mongodb.container.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.api.Randomizer;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import static io.restassured.RestAssured.given;

@QuarkusTest
class Example2ControllerTest {

    @Test
    public void shouldSaveAndReturnTheSameGeneratedExample() throws IOException, JSONException {
        // GIVEN
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
        EasyRandom generator = new EasyRandom(parameters);
        Example2Dto dto = generator.nextObject(Example2Dto.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        //TODO temporary erasing binary field
        dto.setFileUpload(null);

        // Convert to JSON string
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dto);

        // WHEN
        ExtractableResponse<Response> response = given()
                .body(json)
                .contentType(ContentType.JSON)
                .when()
                .post("/examples2/")
                .then()
                .statusCode(200).extract();

        JSONAssert.assertEquals(json, response.asString(), true);
    }
}