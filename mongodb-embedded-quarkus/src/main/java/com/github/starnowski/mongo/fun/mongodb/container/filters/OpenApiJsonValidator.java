package com.github.starnowski.mongo.fun.mongodb.container.filters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.oas.models.OpenAPI;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class OpenApiJsonValidator {
    private static final ObjectMapper mapper = new ObjectMapper();
    private final OpenAPI openAPI;

    public OpenApiJsonValidator() {
        openAPI = new OpenAPIV3Parser().read("src/main/resources/example_openapi.yaml");
    }

    public Set<ValidationMessage> validateObject(String model, String json) throws JsonProcessingException {
        String schemaString = mapper.writeValueAsString(
                openAPI.getComponents().getSchemas().get(model)
        );

        // 3. Build schema validator
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchema schema = factory.getSchema(schemaString);

        // 4. Parse and validate input JSON
        JsonNode jsonNode = mapper.readTree(json);
        return schema.validate(jsonNode);
    }

    public static void main(String[] args) throws Exception {
        String openApiPath = "src/main/resources/openapi.yaml"; // Load dynamically
        String jsonString = "{ \"id\": 123, \"name\": \"Test\" }";

        // 1. Parse OpenAPI spec at runtime
        OpenAPI openAPI = new OpenAPIV3Parser().read(openApiPath);

        // 2. Extract schema (example: from components/schemas/MyModel)
        String schemaString = mapper.writeValueAsString(
                openAPI.getComponents().getSchemas().get("MyModel")
        );

        // 3. Build schema validator
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchema schema = factory.getSchema(schemaString);

        // 4. Parse and validate input JSON
        JsonNode jsonNode = mapper.readTree(jsonString);
        Set<ValidationMessage> errors = schema.validate(jsonNode);

        if (errors.isEmpty()) {
            System.out.println("Valid JSON ✅");
        } else {
            errors.forEach(err -> System.out.println("Validation error: " + err.getMessage()));
        }
    }
}
