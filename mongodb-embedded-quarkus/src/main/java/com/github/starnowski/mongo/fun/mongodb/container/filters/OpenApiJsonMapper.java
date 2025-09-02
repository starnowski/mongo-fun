package com.github.starnowski.mongo.fun.mongodb.container.filters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

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
        OpenAPI openAPI = new OpenAPIV3Parser().readContents(Files.readString(Path.of(openApiSpec)), null, null).getOpenAPI();
        if (openAPI == null) {
            throw new IllegalArgumentException("Invalid OpenAPI spec");
        }

        // 2. Get schema by ref (e.g. "#/components/schemas/MyType")
        String schemaName = schemaRef.replace("#/components/schemas/", "");
        Schema<?> oasSchema = openAPI.getComponents().getSchemas().get(schemaName);
//        if (oasSchema == null) {
//            throw new IllegalArgumentException("Schema not found: " + schemaName);
//        }


        // 6. Convert to typed Map
        Map<String, Object> result = mapper.convertValue(node, Map.class);

        // 7. Post-process for UUIDs, Dates (Jackson modules handle java.time)
        return (Map<String, Object>) coerceDeepWithSchema(result, oasSchema);
    }

    @SuppressWarnings("unchecked")
    private Object coerceDeepWithSchema(Object value, Schema<?> schema) {
        if (value == null || schema == null) return value;

        // Handle polymorphic schemas
        if (schema.getAllOf() != null && !schema.getAllOf().isEmpty()) {
            Map<String, Object> merged = new LinkedHashMap<>();
            for (Schema<?> subSchema : schema.getAllOf()) {
                Object coerced = coerceDeepWithSchema(value, subSchema);
                if (coerced instanceof Map<?, ?> m) {
                    merged.putAll((Map<String, Object>) m);
                }
            }
            return merged;
        }

        if ((schema.getOneOf() != null && !schema.getOneOf().isEmpty()) ||
                (schema.getAnyOf() != null && !schema.getAnyOf().isEmpty())) {

            List<Schema> options = schema.getOneOf() != null ? schema.getOneOf() : schema.getAnyOf();
            for (Schema<?> option : options) {
                try {
                    Object coerced = coerceDeepWithSchema(value, option);
                    return coerced;
                } catch (Exception ignored) {
                    // try next option
                }
            }
            // fallback
            return value;
        }

        String type = schema.getType();
        String format = schema.getFormat();

        if (value instanceof Map<?, ?> subMap && schema.getProperties() != null) {
            Map<String, Object> newMap = new LinkedHashMap<>();
            subMap.forEach((k, v) -> {
                Schema<?> propSchema = (Schema<?>) schema.getProperties().get(k);
                newMap.put(k.toString(), coerceDeepWithSchema(v, propSchema));
            });
            return newMap;
        } else if (value instanceof List<?> list && schema.getItems() != null) {
            List<Object> newList = new ArrayList<>();
            for (Object item : list) {
                newList.add(coerceDeepWithSchema(item, (Schema<?>) schema.getItems()));
            }
            return newList;
        } else {
            return coerceWithSchema(value, type, format);
        }
    }

    private Object coerceWithSchema(Object value, String type, String format) {
        if (value == null) return null;

        try {
            switch (type) {
                case "string" -> {
                    if ("uuid".equals(format)) {
                        return UUID.fromString(value.toString());
                    } else if ("date".equals(format)) {
                        return java.time.LocalDate.parse(value.toString());
                    } else if ("date-time".equals(format)) {
                        return java.time.OffsetDateTime.parse(value.toString());
                    } else if ("byte".equals(format)) {
                        return Base64.getDecoder().decode(value.toString());
                    } else {
                        return value.toString();
                    }
                }
                case "integer" -> {
                    if ("int64".equals(format)) {
                        return Long.valueOf(value.toString());
                    } else {
                        return Integer.valueOf(value.toString());
                    }
                }
                case "number" -> {
                    if ("float".equals(format)) {
                        return Float.valueOf(value.toString());
                    } else {
                        return Double.valueOf(value.toString());
                    }
                }
                case "boolean" -> {
                    return Boolean.valueOf(value.toString());
                }
                default -> {
                    return value;
                }
            }
        } catch (Exception e) {
            // If parsing fails, keep original value
            return value;
        }
    }
}
