package com.github.starnowski.mongo.fun.mongodb.container.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.starnowski.mongo.fun.mongodb.container.exceptions.DuplicatedKeyException;
import com.github.starnowski.mongo.fun.mongodb.container.filters.OpenApiJsonMapper;
import com.github.starnowski.mongo.fun.mongodb.container.services.ExampleService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.util.Map;
import java.util.UUID;

@Path("/examples2")
public class Example2Controller {

    private final ObjectMapper mapper;
    @Inject
    private ExampleService exampleService;
    @Inject
    private OpenApiJsonMapper openApiJsonMapper;

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
}
