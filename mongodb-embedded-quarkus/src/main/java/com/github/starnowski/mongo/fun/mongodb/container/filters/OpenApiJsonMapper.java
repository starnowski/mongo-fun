package com.github.starnowski.mongo.fun.mongodb.container.filters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.Binary;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

@ApplicationScoped
public class OpenApiJsonMapper {

    private final ObjectMapper mapper;

    public OpenApiJsonMapper() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); // handle java.time types
        SimpleModule mongoDBModule = new SimpleModule();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mongoDBModule.addSerializer(Binary.class, new JsonSerializer<Binary>() {
            @Override
            public void serialize(Binary value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(Base64.getEncoder().encodeToString(value.getData()));
            }
        });

        mapper.registerModule(mongoDBModule);
    }

    public Map<String, Object> coerceRawJsonTypesToOpenApiJavaTypes(
            Map<String, Object> jsonMap,
            String openApiSpec,
            String schemaRef
    ) throws Exception {
        return parse(mapper.valueToTree(jsonMap), openApiSpec, schemaRef, (value, type, format) -> coerceWithSchema(value, type, format));
    }

    public Map<String, Object> coerceMongoDecodedTypesToOpenApiJavaTypes(
            Map<String, Object> jsonMap,
            String openApiSpec,
            String schemaRef
    ) throws Exception {
        return parse(mapper.valueToTree(jsonMap), openApiSpec, schemaRef, (value, type, format) -> coerceJavaTypeWithSchema(value, type, format));
    }

    private Map<String, Object> parse(
            JsonNode node,
            String openApiSpec,
            String schemaRef,
            CoerceValueWithSchema coerceValueWithSchema
    ) throws Exception {
        // 1. Load OpenAPI spec
        OpenAPI openAPI = new OpenAPIV3Parser().readContents(Files.readString(Path.of(openApiSpec)), null, null).getOpenAPI();
        if (openAPI == null) {
            throw new IllegalArgumentException("Invalid OpenAPI spec");
        }

        // 2. Get schema by ref (e.g. "#/components/schemas/MyType")
        String schemaName = schemaRef.replace("#/components/schemas/", "");
        Schema<?> oasSchema = openAPI.getComponents().getSchemas().get(schemaName);


        // 6. Convert to typed Map
        Map<String, Object> result = mapper.convertValue(node, Map.class);

        // 7. Post-process for UUIDs, Dates (Jackson modules handle java.time)
        return (Map<String, Object>) coerceDeepWithSchema(result, oasSchema, coerceValueWithSchema );
    }



    @SuppressWarnings("unchecked")
    private Object coerceDeepWithSchema(Object value, Schema<?> schema, CoerceValueWithSchema coerceValueWithSchema) {
        if (value == null || schema == null) return value;

        // Handle polymorphic schemas
        if (schema.getAllOf() != null && !schema.getAllOf().isEmpty()) {
            Map<String, Object> merged = new LinkedHashMap<>();
            for (Schema<?> subSchema : schema.getAllOf()) {
                Object coerced = coerceDeepWithSchema(value, subSchema, coerceValueWithSchema);
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
                    Object coerced = coerceDeepWithSchema(value, option, coerceValueWithSchema);
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
                newMap.put(k.toString(), coerceDeepWithSchema(v, propSchema, coerceValueWithSchema));
            });
            return newMap;
        } else if (value instanceof List<?> list && schema.getItems() != null) {
            List<Object> newList = new ArrayList<>();
            for (Object item : list) {
                newList.add(coerceDeepWithSchema(item, (Schema<?>) schema.getItems(), coerceValueWithSchema));
            }
            return newList;
        } else {
            return coerceValueWithSchema.coerce(value, type, format);
        }
    }

    interface CoerceValueWithSchema {

        Object coerce(Object value, String type, String format);
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
                    } else if ("int32".equals(format)) {
                        return Integer.valueOf(value.toString());
                    } else {
                        return new BigInteger(value.toString());
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

    private Object coerceJavaTypeWithSchema(Object value, String type, String format) {
        if (value == null) return null;

        try {
            switch (type) {
                case "string" -> {
                    if ("uuid".equals(format)) {
                        return UUID.fromString(value.toString());
                    } else if ("date".equals(format)) {
//                        return dateStringToLocalDate(value.toString());
                        return dateToInstant(value.toString()).atZone(ZoneId.of("UTC")).toLocalDate();
                    } else if ("date-time".equals(format)) {
                        return dateToInstant(value.toString()).atOffset(ZoneOffset.UTC);
                    } else if ("byte".equals(format)) {
                        return Base64.getDecoder().decode(value.toString());
                    } else {
                        return value.toString();
                    }
                }
                case "integer" -> {
                    if ("int64".equals(format)) {
                        return Long.valueOf(value.toString());
                    } else if ("int32".equals(format)) {
                        return Integer.valueOf(value.toString());
                    } else {
                        return new BigInteger(value.toString());
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

    private Instant dateToInstant(String dateValue) throws ParseException {
        return mapper.getDateFormat().parse(dateValue).toInstant();
    }

    private LocalDate dateStringToLocalDate(String dateValue) {
        Date date = Date.from(Instant.ofEpochMilli(Long.parseLong(dateValue)));
        Instant instant = date.toInstant();
        return instant.atZone(ZoneId.of("UTC")).toLocalDate();
    }
}
