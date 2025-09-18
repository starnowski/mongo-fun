package com.github.starnowski.mongo.fun.mongodb.container.odata;

import com.mongodb.client.model.Aggregates;
import org.apache.olingo.commons.api.edm.EdmEnumType;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.server.api.uri.queryoption.expression.*;
import org.apache.olingo.server.api.ODataApplicationException;
import org.bson.Document;
import org.bson.conversions.Bson;
import com.mongodb.client.model.Filters;

import java.util.*;
import java.util.regex.Pattern;

public class MongoFilterVisitor implements ExpressionVisitor<Bson> {

    public static final String CUSTOM_LITERAL_VALUE_PROPERTY = "$odata.literal";

    // --- Literals ---
    @Override
    public Bson visitLiteral(Literal literal) {
        String text = literal.getText();
        if ("null".equals(text)) {
            //TODO do not support
            return Filters.eq(null, null);
        }
        if (text.startsWith("'") && text.endsWith("'")) {
            return literal(text.substring(1, text.length() - 1)); // placeholder, field comes later
        }
        return literal(text);
//        try {
//            return Filters.eq(Integer.parseInt(text), null);
//        } catch (NumberFormatException e) {
//            try {
//                return Filters.eq(Double.parseDouble(text), null);
//            } catch (NumberFormatException ex) {
//                return Filters.eq(text, null);
//            }
//        }
    }

    public static Document literal(Object value) {
        return new Document(CUSTOM_LITERAL_VALUE_PROPERTY, value);
    }

    // --- Members (fields) ---
    @Override
    public Bson visitMember(Member member) {
        String field = member.getResourcePath().getUriResourceParts().get(0).toString();
        return Filters.exists(field); // placeholder, combined later
    }

    // --- Binary operators ---
    @Override
    public Bson visitBinaryOperator(BinaryOperatorKind operator, Bson left, Bson right)
            throws ExpressionVisitException, ODataApplicationException {
        switch (operator) {
            case EQ:  return combineEq(left, right);
            case NE:  return combineFieldOp(left, right, Filters::ne);
            case GT:  return combineFieldOp(left, right, Filters::gt);
            case GE:  return combineFieldOp(left, right, Filters::gte);
            case LT:  return combineFieldOp(left, right, Filters::lt);
            case LE:  return combineFieldOp(left, right, Filters::lte);
            case AND: return Filters.and(left, right);
            case OR:  return Filters.or(left, right);
            default:
                throw new UnsupportedOperationException("Operator not supported: " + operator);
        }
    }

    // --- Unary operators ---
    @Override
    public Bson visitUnaryOperator(UnaryOperatorKind operator, Bson operand) {
        if (operator == UnaryOperatorKind.NOT) {
            return Filters.not(operand);
        }
        throw new UnsupportedOperationException("Unary operator not supported: " + operator);
    }

    // --- Methods (functions) ---
    @Override
    public Bson visitMethodCall(MethodKind methodCall, List<Bson> parameters) {
        if (parameters.size() != 2) {
            throw new UnsupportedOperationException("Expected field and value for: " + methodCall);
        }
        String field = extractField(parameters.get(0));
        String value = extractValue(parameters.get(1));

        switch (methodCall) {
            case STARTSWITH:
                return Filters.regex(field, Pattern.compile("^" + Pattern.quote(value)));
            case ENDSWITH:
                return Filters.regex(field, Pattern.compile(Pattern.quote(value) + "$"));
            case CONTAINS:
                return Filters.regex(field, Pattern.compile(Pattern.quote(value)));
            default:
                throw new UnsupportedOperationException("Method not supported: " + methodCall);
        }
    }

    // --- Helpers ---
    private Bson combineEq(Bson left, Bson right) {
        String field = extractField(left);
        Object value = extractValueObj(right);
        return Filters.eq(field, value);
    }

    private Bson combineFieldOp(Bson left, Bson right,
                                java.util.function.BiFunction<String, Object, Bson> fn) {
        String field = extractField(left);
        Object value = extractValueObj(right);
        return fn.apply(field, value);
    }

    private String extractField(Bson bson) {
        return bson.toBsonDocument().keySet().iterator().next();
    }

    private String extractValue(Bson bson) {
        return bson.toBsonDocument().get(CUSTOM_LITERAL_VALUE_PROPERTY).asString().getValue();
    }

    private Object extractValueObj(Bson bson) {
        return bson.toBsonDocument().get(CUSTOM_LITERAL_VALUE_PROPERTY);
    }

    // --- Not used in this example ---
    @Override public Bson visitAlias(String aliasName) { throw new UnsupportedOperationException(); }

    @Override
    public Bson visitTypeLiteral(EdmType edmType) throws ExpressionVisitException, ODataApplicationException {
        return null;
    }

    @Override public Bson visitLambdaExpression(String s, String s1, Expression expression) { throw new UnsupportedOperationException(); }
    @Override public Bson visitLambdaReference(String s) { throw new UnsupportedOperationException(); }

    @Override
    public Bson visitEnum(EdmEnumType edmEnumType, List<String> list) throws ExpressionVisitException, ODataApplicationException {
        return null;
    }

    @Override
    public Bson visitBinaryOperator(BinaryOperatorKind binaryOperatorKind, Bson bson, List<Bson> list) throws ExpressionVisitException, ODataApplicationException {
        return null;
    }
}
