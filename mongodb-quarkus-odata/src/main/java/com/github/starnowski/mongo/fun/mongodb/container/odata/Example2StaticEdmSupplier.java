package com.github.starnowski.mongo.fun.mongodb.container.odata;

import com.github.starnowski.mongo.fun.mongodb.container.repositories.ExampleDao;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.olingo.commons.api.edm.Edm;
import org.apache.olingo.commons.core.edm.EdmProviderImpl;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.core.uri.parser.Parser;
import org.apache.olingo.server.core.uri.parser.UriParserException;
import org.apache.olingo.server.core.uri.validator.UriValidationException;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class Example2StaticEdmSupplier {

    private final Edm EDM;


    public Example2StaticEdmSupplier() throws Exception {
        OpenApiToODataMapper openApiToODataMapper = new OpenApiToODataMapper();
        OpenApiToODataMapper.OpenApiToODataMapperResult odataConfig = openApiToODataMapper.returnOpenApiToODataConfiguration("src/main/resources/example2_openapi.yaml", "Example2");
        this.EDM = new EdmProviderImpl(new GenericEdmProvider(new GenericEdmProvider.GenericEdmProviderProperties("Example2", "examples2"), odataConfig));
    }

    public Edm get() {
        return this.EDM;
    }
}