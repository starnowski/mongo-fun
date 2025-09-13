package com.github.starnowski.mongo.fun.mongodb.container.odata;

import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.api.ODataApplicationException;
import org.bson.conversions.Bson;

public class ODataToMongoParser {

    public static Bson parseFilter(UriInfo uriInfo) throws ODataApplicationException, ExpressionVisitException {
        FilterOption filter = uriInfo.getFilterOption();
        if (filter == null) return null;
        Expression expr = filter.getExpression();
        return expr.accept(new MongoFilterVisitor());
    }
}
