package com.github.starnowski.mongo.fun.mongodb.container.repositories;

import com.github.starnowski.mongo.fun.mongodb.container.odata.Example2StaticEdmSupplier;
import com.github.starnowski.mongo.fun.mongodb.container.odata.ODataToMongoParser;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@ApplicationScoped
public class ExampleDao extends AbstractDao<Document> {

    @Inject
    private Example2StaticEdmSupplier example2StaticEdmSupplier;

    @Override
    protected String getCollectionName() {
        return "examples";
    }

    @Override
    protected Class<Document> getDocumentClass() {
        return Document.class;
    }

    public List<Document> query(String filter) throws ExpressionVisitException, ODataApplicationException, UriValidationException, UriParserException {
        List<Bson> pipeline = preparePipelineBasedOnFilter(Collections.singletonList(filter));

        return pipeline.isEmpty() ? new ArrayList<>() : getCollection().aggregate(pipeline).into(new ArrayList<>());
    }

    private List<Bson> preparePipelineBasedOnFilter(List<String> filters) throws UriValidationException, UriParserException, ExpressionVisitException, ODataApplicationException {
        List<Bson> pipeline = new ArrayList<>();

        if (filters != null && !filters.isEmpty() &&
                filters.stream().filter(Objects::nonNull).anyMatch(filter -> !filter.trim().isEmpty())) {
            // Parse OData $filter into UriInfo (simplified)
            UriInfo uriInfo = new Parser(example2StaticEdmSupplier.get(), OData.newInstance())
                    .parseUri("examples2",
//                            "$filter=" + filter
                            filters.stream().filter(Objects::nonNull)
                                    .filter(filter -> !filter.trim().isEmpty())
                                    .map(filter -> "$filter=" + filter)
                                    .collect(Collectors.joining("&"))
                            , null, null);

            FilterOption filterOption = uriInfo.getFilterOption();
            if (filterOption != null) {
                Bson bsonFilter = ODataToMongoParser.parseFilter(uriInfo);
                pipeline.add(Aggregates.match(Filters.and(bsonFilter)));
            }

        }
        return pipeline;
    }
}