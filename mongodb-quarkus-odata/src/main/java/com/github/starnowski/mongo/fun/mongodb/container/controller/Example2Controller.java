package com.github.starnowski.mongo.fun.mongodb.container.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.starnowski.mongo.fun.mongodb.container.exceptions.DuplicatedKeyException;
import com.github.starnowski.mongo.fun.mongodb.container.filters.OpenApiJsonMapper;
import com.github.starnowski.mongo.fun.mongodb.container.filters.SecuredExample2;
import com.github.starnowski.mongo.fun.mongodb.container.patch.PatchHelper;
import com.github.starnowski.mongo.fun.mongodb.container.services.ExampleService;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonMergePatch;
import jakarta.json.JsonPatch;
import jakarta.json.JsonReader;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/examples2")
public class Example2Controller {

    private final ObjectMapper mapper;
    @Inject
    private ExampleService exampleService;
    @Inject
    private OpenApiJsonMapper openApiJsonMapper;
    @Inject
    private PatchHelper patchHelper;

    public Example2Controller() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); // handle java.time types
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getExample(@PathParam("id") UUID id) throws Exception {
        Map<String, Object> savedModel = exampleService.getById(id);
        savedModel.remove("_id");
        savedModel = openApiJsonMapper.coerceMongoDecodedTypesToOpenApiJavaTypesV2(savedModel, "src/main/resources/example2_openapi.yaml", "Example2");
        return Response.ok(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(savedModel)).build();
    }


//    @Secured
    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @SecuredExample2
    public Response saveExample(Map<String, Object> body) throws Exception {
        Map<String, Object> coercedMap = openApiJsonMapper.coerceRawJsonTypesToOpenApiJavaTypes(body, "src/main/resources/example2_openapi.yaml", "Example2");
        Map<String, Object> savedModel = exampleService.saveExample(coercedMap);
        savedModel.remove("_id");
        savedModel = openApiJsonMapper.coerceMongoDecodedTypesToOpenApiJavaTypesV2(savedModel, "src/main/resources/example2_openapi.yaml", "Example2");
        return Response.ok(mapper.writeValueAsString(savedModel)).build();
    }

    @POST
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @SecuredExample2
    public Response saveExampleWithId(@PathParam("id") UUID id, Map<String, Object> body, @Context UriInfo uriInfo) throws Exception {
        Map<String, Object> coercedMap = openApiJsonMapper.coerceRawJsonTypesToOpenApiJavaTypes(body, "src/main/resources/example2_openapi.yaml", "Example2");
        Map<String, Object> savedModel = null;
        try {
            savedModel = exampleService.saveAndUpdate(id, coercedMap, Map.copyOf(uriInfo.getQueryParameters()));
        } catch (DuplicatedKeyException duplicatedKeyException) {
            return Response.status(400).build();
        }
        savedModel.remove("_id");
        savedModel = openApiJsonMapper.coerceMongoDecodedTypesToOpenApiJavaTypesV2(savedModel, "src/main/resources/example2_openapi.yaml", "Example2");
        return Response.ok(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(savedModel)).build();
    }

    @PUT
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @SecuredExample2
    public Response updateExampleWithId(@PathParam("id") UUID id, Map<String, Object> body, @Context UriInfo uriInfo) throws Exception {
        return updateExample(id, body, Map.copyOf(uriInfo.getQueryParameters()));
    }

    private Response updateExample(UUID id, Map<String, Object> body, Map<String, Object> queryParams) throws Exception {
        Map<String, Object> coercedMap = openApiJsonMapper.coerceRawJsonTypesToOpenApiJavaTypes(body, "src/main/resources/example2_openapi.yaml", "Example2");
        Map<String, Object> savedModel = exampleService.update(id, coercedMap, queryParams);
        savedModel.remove("_id");
        savedModel = openApiJsonMapper.coerceMongoDecodedTypesToOpenApiJavaTypesV2(savedModel, "src/main/resources/example2_openapi.yaml", "Example2");
        return Response.ok(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(savedModel)).build();
    }

    // JSON Patch (application/json-patch+json)
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @Path("/{id}")//consumes = "application/json-patch+json")
    @PATCH
    public Response patchUser(@PathParam("id") UUID id, String patchJson) throws Exception {
        Response getResponse = getExample(id);
        Map<String, Object> savedModel = mapper.readValue((String)getResponse.getEntity(), Map.class);

        JsonReader reader = Json.createReader(new StringReader(patchJson));
        JsonPatch patch = Json.createPatch(reader.readArray());
        Map patched = patchHelper.applyJsonPatch(patch, savedModel, Map.class);
        return updateExample(id, patched, null);
    }

    // JSON Merge Patch (application/merge-patch+json)
    @Consumes("application/merge-patch+json")
    @Path("/{id}")
    @PATCH
    public Response mergePatchUser(@PathParam("id") UUID id, String mergePatchJson) throws Exception {
        Response getResponse = getExample(id);
        Map<String, Object> savedModel = mapper.readValue((String)getResponse.getEntity(), Map.class);

        JsonReader reader = Json.createReader(new StringReader(mergePatchJson));
        JsonMergePatch mergePatch = Json.createMergePatch(reader.readValue());

        Map patched = patchHelper.applyMergePatch(mergePatch, savedModel, Map.class);
        return updateExample(id, patched, null);
    }

    @GET
    @Path("/simple-query")
    @Produces(MediaType.APPLICATION_JSON)
    public Response simpleFilterQuery(@QueryParam("$filter") String filter) throws Exception {
        List<Map<String, Object>> results = exampleService.query(filter);
        QueryResponse queryResponse = new QueryResponse(results.stream()
                .map(rec -> {
                    try {
                        return openApiJsonMapper.coerceMongoDecodedTypesToOpenApiJavaTypesV2(rec, "src/main/resources/example2_openapi.yaml", "Example2");
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).toList(), exampleService.explain(filter));
        return Response.ok(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(queryResponse)).build();
    }

    public record QueryResponse(List<Map<String, Object>> results, String winningPlan) {

    }
}
