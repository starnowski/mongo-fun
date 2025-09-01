package com.github.starnowski.mongo.fun.mongodb.container.controller;

import com.github.starnowski.mongo.fun.mongodb.container.services.ExampleService;
import jakarta.inject.Inject;
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

    @POST
    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    public Response saveExample(Map<String, Object> body) throws IOException {
        return Response.ok(exampleService.saveExample(body)).build();
    }
}
