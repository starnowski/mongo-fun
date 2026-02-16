package com.github.starnowski.mongo.fun.mongodb.container.filters;

import com.networknt.schema.ValidationMessage;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.bson.Document;
import org.bson.json.JsonObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SecuredExample2
@Provider
public class OpenApiExample2ValidationFilter implements ContainerRequestFilter {

    @Inject
    private OpenApiJsonValidator openApiJsonValidator;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {

        // Log the request
        System.out.printf("Incoming %s request to %s%n",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri());

        Set<ValidationMessage> errors = this.openApiJsonValidator.validateObjectSpec2("Example2", getRequestBody(requestContext));
        if (errors.isEmpty()) {
            System.out.println("Valid JSON ✅");
        } else {
            errors.forEach(err -> System.out.println("Validation error: " + err.getMessage()));
            Document document = new Document();
            List<Document> errorsList = new ArrayList<>();
            errors.forEach(err -> {
                Document error = new Document();
                error.append("code", "oas.validation");
                error.append("reason", "validation_failed");
                error.append("message", err.getMessage());
                error.append("location", err.getInstanceLocation().toString().replace("$.", "request."));
                errorsList.add(error);
            });
            document.append("errors", errorsList);
            requestContext.abortWith(Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(document.toJson())
                        .build());
        }
    }

    public String getRequestBody(ContainerRequestContext requestContext) throws IOException {
        InputStream entityStream = requestContext.getEntityStream();

        // Copy the stream to a byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = entityStream.read(buffer)) != -1) {
            baos.write(buffer, 0, length);
        }
        byte[] entityBytes = baos.toByteArray();

        // Restore the stream so the resource method can still consume it
        requestContext.setEntityStream(new ByteArrayInputStream(entityBytes));

        // Convert to string (assuming UTF-8)
        return new String(entityBytes, StandardCharsets.UTF_8);
    }
}
