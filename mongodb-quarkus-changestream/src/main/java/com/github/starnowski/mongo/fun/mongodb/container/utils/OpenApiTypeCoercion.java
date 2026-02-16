package com.github.starnowski.mongo.fun.mongodb.container.utils;

import org.openapi4j.parser.model.v3.MediaType;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.openapi4j.parser.model.v3.Operation;
import org.openapi4j.parser.model.v3.Path;
import org.openapi4j.parser.model.v3.Schema;

import jakarta.enterprise.context.ApplicationScoped;
import java.text.SimpleDateFormat;
import java.util.*;

@ApplicationScoped
public class OpenApiTypeCoercion {
    private static final SimpleDateFormat ISO_DATE = new SimpleDateFormat("yyyy-MM-dd");

    public void coerceTypes(OpenApi3 api, String path, String method, Map<String, Object> body) {
        Path p = api.getPath(path);
        if (p == null) return;

        Operation op = p.getOperation(method.toLowerCase());
        if (op == null || op.getRequestBody() == null) return;

        MediaType mt = op.getRequestBody().getContentMediaType("application/json");
        if (mt == null || mt.getSchema() == null) return;

        Schema schema = mt.getSchema();
        if ("object".equals(schema.getType())) {
            applyFormatConversions(body, schema);
        }
    }

    private void applyFormatConversions(Map<String, Object> data, Schema schema) {
        if (data == null) return;
        Map<String, Schema> props = schema.getProperties();
        if (props == null) return;

        for (Map.Entry<String, Schema> entry : props.entrySet()) {
            String key = entry.getKey();
            Schema propSchema = entry.getValue();
            Object value = data.get(key);

            if (value == null) continue;

            if ("string".equals(propSchema.getType())) {
                if ("date".equals(propSchema.getFormat()) && value instanceof String s) {
                    data.put(key, parseDate(s));
                }
                if ("uuid".equals(propSchema.getFormat()) && value instanceof String s) {
                    data.put(key, UUID.fromString(s));
                }
            }
            if ("object".equals(propSchema.getType()) && value instanceof Map<?, ?> nested) {
                // recursive conversion for nested objects
                //noinspection unchecked
                applyFormatConversions((Map<String, Object>) nested, propSchema);
            }
        }
    }

    private Date parseDate(String s) {
        try {
            return ISO_DATE.parse(s);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format: " + s, e);
        }
    }
}

