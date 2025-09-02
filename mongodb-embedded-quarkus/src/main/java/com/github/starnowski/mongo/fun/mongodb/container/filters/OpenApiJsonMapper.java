package com.github.starnowski.mongo.fun.mongodb.container.filters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class OpenApiJsonMapper {

    private final ObjectMapper mapper;

    public OpenApiJsonMapper() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); // handle java.time types
    }

    public Map<String, Object> coerceJsonString(
            String json,
            String openApiSpec,
            String schemaRef
    ) throws Exception {
        return parse(mapper.readTree(json), openApiSpec, schemaRef);
    }

    public Map<String, Object> coerceMapToJson(
            Map<String, Object> jsonMap,
            String openApiSpec,
            String schemaRef
    ) throws Exception {
        return parse(mapper.valueToTree(jsonMap), openApiSpec, schemaRef);
    }

    private Map<String, Object> parse(
            JsonNode node,
            String openApiSpec,
            String schemaRef
    ) throws Exception {

        // 1. Load OpenAPI spec
        OpenAPI openAPI = new OpenAPIV3Parser().readContents(openApiSpec, null, null).getOpenAPI();
        if (openAPI == null) {
            throw new IllegalArgumentException("Invalid OpenAPI spec");
        }

        // 2. Get schema by ref (e.g. "#/components/schemas/MyType")
        String schemaName = schemaRef.replace("#/components/schemas/", "");
        Schema<?> oasSchema = openAPI.getComponents().getSchemas().get(schemaName);
        if (oasSchema == null) {
            throw new IllegalArgumentException("Schema not found: " + schemaName);
        }


        // 6. Convert to typed Map
        Map<String, Object> result = mapper.convertValue(node, Map.class);

        // 7. Post-process for UUIDs, Dates (Jackson modules handle java.time)
        result.replaceAll((k, v) -> coerceValue(v));

        return result;
    }

    private Object coerceValue(Object value) {
        if (value instanceof String str) {
            try {
                return java.util.UUID.fromString(str);
            } catch (IllegalArgumentException ignored) {
            }

            try {
                return java.time.OffsetDateTime.parse(str);
            } catch (Exception ignored) {
            }

            try {
                return java.time.LocalDate.parse(str);
            } catch (Exception ignored) {
            }
        }
        return value;
    }
}
