package com.github.starnowski.mongo.fun.mongodb.container.filters;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

@Secured
@Provider
public class OpenApiValidationFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {

        // Log the request
        System.out.printf("Incoming %s request to %s%n",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri());

        requestContext.abortWith(Response
                    .status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid or missing API key")
                    .build());
    }
}
