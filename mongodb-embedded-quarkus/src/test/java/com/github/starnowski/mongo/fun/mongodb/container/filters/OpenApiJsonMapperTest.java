package com.github.starnowski.mongo.fun.mongodb.container.filters;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiJsonMapperTest {

    @Test
    public void shouldSetupCorrectTypes() throws Exception {
        // GIVEN
        Map<String, Object> mongoDocument = new HashMap<>();
        mongoDocument.put("birthDate", new Date());
        mongoDocument.put("timestamp", new Date());
        mongoDocument.put("nonSpecifiedDateProperty", new Date());
        OpenApiJsonMapper tested = new OpenApiJsonMapper();

        // WHEN
        Map<String, Object> result = tested.coerceMongoDecodedTypesToOpenApiJavaTypes(mongoDocument, "src/main/resources/example2_openapi.yaml", "Example2");

        // THEN
        Assertions.assertEquals(LocalDate.class,  result.get("birthDate").getClass());
        Assertions.assertEquals(OffsetDateTime.class,  result.get("timestamp").getClass());
//        Assertions.assertEquals(Date.class,  result.get("nonSpecifiedDateProperty").getClass());
    }
}