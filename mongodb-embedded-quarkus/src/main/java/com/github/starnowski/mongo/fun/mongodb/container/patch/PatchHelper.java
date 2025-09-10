package com.github.starnowski.mongo.fun.mongodb.container.patch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.*;

import java.io.IOException;
import java.io.StringReader;

@ApplicationScoped
public class PatchHelper {

    private final ObjectMapper mapper = new ObjectMapper();

    // JSON Patch (RFC 6902)
    public <T> T applyJsonPatch(JsonPatch patch, T target, Class<T> type) throws IOException, JsonProcessingException {
        // Convert POJO -> JSON string
        String targetJson = mapper.writeValueAsString(target);

        // Parse string into JSON-P structure
        JsonReader reader = Json.createReader(new StringReader(targetJson));
        JsonStructure targetStructure = reader.read();
        JsonValue patchedJson = patch.apply(targetStructure);
        return mapper.readValue(patchedJson.toString(), type);
    }

    // JSON Merge Patch (RFC 7396)
    public <T> T applyMergePatch(JsonMergePatch mergePatch, T target, Class<T> type) throws IOException, IOException {
        JsonStructure targetJson = mapper.readValue(mapper.writeValueAsBytes(target), JsonStructure.class);
        JsonValue patchedJson = mergePatch.apply(targetJson);
        return mapper.readValue(patchedJson.toString(), type);
    }
}
