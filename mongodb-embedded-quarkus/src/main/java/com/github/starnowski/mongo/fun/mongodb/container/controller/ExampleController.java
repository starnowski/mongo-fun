package com.github.starnowski.mongo.fun.mongodb.container.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.starnowski.mongo.fun.mongodb.container.filters.OpenApiJsonMapper;
import com.github.starnowski.mongo.fun.mongodb.container.filters.Secured;
import com.github.starnowski.mongo.fun.mongodb.container.services.ExampleService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.util.Map;

@Path("/examples")
public class ExampleController {

    @Inject
    private ExampleService exampleService;

    @Inject
    private OpenApiJsonMapper openApiJsonMapper;

    private final ObjectMapper mapper;

    public ExampleController() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); // handle java.time types
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Secured
    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveExample(Map<String, Object> body) throws Exception {
        Map<String, Object> coercedMap = openApiJsonMapper.coerceMapToJson(body, "src/main/resources/example_openapi.yaml", "Example");
        Map<String, Object> savedModel = exampleService.saveExample(coercedMap);
        return Response.ok(mapper.writeValueAsString(savedModel)).build();
    }
}
