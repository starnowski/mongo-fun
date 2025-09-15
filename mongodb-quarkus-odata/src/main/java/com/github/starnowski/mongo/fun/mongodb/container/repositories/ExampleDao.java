package com.github.starnowski.mongo.fun.mongodb.container.repositories;

import com.github.starnowski.mongo.fun.mongodb.container.odata.MongoFilterVisitor;
import com.github.starnowski.mongo.fun.mongodb.container.odata.ODataToMongoParser;
import com.mongodb.client.model.Aggregates;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.olingo.commons.api.edm.Edm;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.core.uri.parser.Parser;
import org.apache.olingo.server.core.uri.parser.UriParserException;
import org.apache.olingo.server.core.uri.validator.UriValidationException;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ExampleDao extends AbstractDao<Document> {

    @Override
    protected String getCollectionName() {
        return "examples";
    }

    @Override
    protected Class<Document> getDocumentClass() {
        return Document.class;
    }

    public List<Document> query(String filter) throws ExpressionVisitException, ODataApplicationException, UriValidationException, UriParserException {
        List<Bson> pipeline = new ArrayList<>();

        if (filter != null && !filter.isEmpty()) {
            UriInfo uriInfo;
            if (filter != null) {
                // Parse OData $filter into UriInfo (simplified)
                uriInfo = new Parser((Edm) null, null).parseUri(null, "$filter=" + filter, null, null);
                Bson mongoFilter = ODataToMongoParser.parseFilter(uriInfo);

                FilterOption filterOption = uriInfo.getFilterOption();
                if (filterOption != null) {
                    Expression expr = filterOption.getExpression();
                    Bson bsonFilter = expr.accept(new MongoFilterVisitor());
                    pipeline.add(Aggregates.match(bsonFilter));
                }
            }

        }

        return getCollection().aggregate(pipeline).into(new ArrayList<>());
    }
}