package com.github.starnowski.mongo.fun.mongodb.container.filters;

import com.fasterxml.jackson.annotation.JsonInclude;
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

import java.util.Locale;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class OpenApiJsonValidator {
    private final ObjectMapper mapper;
    private final OpenAPI openAPI;
    private final OpenAPI openAPI2;

    static {
        Locale.setDefault(Locale.ENGLISH);  // 👈 must be set before validator loads ResourceBundle
    }

    public OpenApiJsonValidator() {
        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        openAPI = new OpenAPIV3Parser().read("src/main/resources/example_openapi.yaml");// Exclude all null fields globally
        openAPI2 = new OpenAPIV3Parser().read("src/main/resources/example2_openapi.yaml");// Exclude all null fields globally
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

    public Set<ValidationMessage> validateObjectSpec2(String model, String json) throws JsonProcessingException {
        String schemaString = mapper.writeValueAsString(
                openAPI2.getComponents().getSchemas().get(model)
        );

        // 3. Build schema validator
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchema schema = factory.getSchema(schemaString);

        // 4. Parse and validate input JSON
        JsonNode jsonNode = mapper.readTree(json);
        return schema.validate(jsonNode);
    }

}
