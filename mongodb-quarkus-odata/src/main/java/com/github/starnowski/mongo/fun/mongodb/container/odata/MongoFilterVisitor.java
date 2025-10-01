package com.github.starnowski.mongo.fun.mongodb.container.odata;

import com.mongodb.client.model.Filters;
import lombok.Builder;
import org.apache.olingo.commons.api.edm.*;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceLambdaAny;
import org.apache.olingo.server.api.uri.UriResourceLambdaVariable;
import org.apache.olingo.server.api.uri.queryoption.expression.*;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class MongoFilterVisitor implements ExpressionVisitor<Bson> {

    public static final String CUSTOM_LITERAL_VALUE_PROPERTY = "$odata.literal";
    public static final String ODATA_MEMBER_PROPERTY = "$odata.member";
    public static final String ODATA_MEMBER_TYPE_PROPERTY = "$odata.member.type";
    private final Edm edm;
    private final MongoFilterVisitorContext context;


    public MongoFilterVisitor(Edm edm) {
        this(edm, MongoFilterVisitorContext.builder().build());
    }

    public MongoFilterVisitor(Edm edm, MongoFilterVisitorContext context) {
        this.edm = edm;
        this.context = context;
    }

    public static Document literal(Object value) {
        return new Document(CUSTOM_LITERAL_VALUE_PROPERTY, value);
    }

    // --- Literals ---
    @Override
    public Bson visitLiteral(Literal literal) {
        String text = literal.getText();
        if ("null".equals(text)) {
            return literal(null);
        }
        if (text.startsWith("'") && text.endsWith("'")) {
            return literal(text.substring(1, text.length() - 1)); // placeholder, field comes later
        }
        try {
            return literal(Integer.parseInt(text));
        } catch (NumberFormatException e) {
            try {
                return literal(Double.parseDouble(text));
            } catch (NumberFormatException ignored) {
            }
        }
        return literal(text);
    }

    // --- Members (fields) ---
    @Override
    public Bson visitMember(Member member) {
        if (member.getResourcePath().getUriResourceParts().size() == 1) {
            String field = member.getResourcePath().getUriResourceParts().get(0).toString();
            if (member.getResourcePath().getUriResourceParts().get(0) instanceof UriResourceLambdaVariable variable) {
                if (this.context.isLambdaAnyContext() && this.context.lambdaVariableAliases().containsKey(variable.getVariableName())) {
                    if (this.context.isExprMode()) {
                        return prepareMemberDocument("$$" + variable.getVariableName(), variable.getType());
                    }
                    return this.context.lambdaVariableAliases().get(variable.getVariableName());
                }
                return prepareMemberDocument(field, variable.getType());
            } else {
                return prepareMemberDocument(field);
            }
        } else {
            UriResource last = member.getResourcePath().getUriResourceParts().get(member.getResourcePath().getUriResourceParts().size() - 1);
            if (last instanceof UriResourceLambdaAny any) {
                String field = member.getResourcePath().getUriResourceParts().get(0).toString();
                try {
                    MongoFilterVisitor innerMongoFilterVisitor = new MongoFilterVisitor(edm,
                            MongoFilterVisitorContext.builder()
                                    .lambdaVariableAliases(Map.of(any.getLambdaVariable(), prepareMemberDocument(field)))
                                    .isLambdaAnyContext(true)
                                    .build());
//                    Bson innerObject = innerMongoFilterVisitor.visitLambdaExpression("ANY", any.getLambdaVariable(), any.getExpression());
//                    return prepareElementMatchDocumentForAnyLambda(innerObject, field);
                    return innerMongoFilterVisitor.visitLambdaExpression("ANY", any.getLambdaVariable(), any.getExpression());
                } catch (ExpressionOperantRequiredException ex) {
                    MongoFilterVisitor innerMongoFilterVisitor = new MongoFilterVisitor(edm,
                            MongoFilterVisitorContext.builder()
                                    .lambdaVariableAliases(Map.of(any.getLambdaVariable(), prepareMemberDocument(field)))
                                    .isLambdaAnyContext(true)
                                    .isExprMode(true)
                                    .build());
                    Bson innerObject = innerMongoFilterVisitor.visitLambdaExpression("ANY", any.getLambdaVariable(), any.getExpression());
                    return prepareExprDocumentForAnyLambdaWithExpr(innerObject, field, any.getLambdaVariable());
                } catch (ElementMatchOperantRequiredException ex) {
                    MongoFilterVisitor innerMongoFilterVisitor = new MongoFilterVisitor(edm,
                            MongoFilterVisitorContext.builder()
                                    .lambdaVariableAliases(Map.of(any.getLambdaVariable(), prepareMemberDocument(field)))
                                    .isLambdaAnyContext(true)
                                    .isElementMatchContext(true)
                                    .build());
                    Bson innerObject = innerMongoFilterVisitor.visitLambdaExpression("ANY", any.getLambdaVariable(), any.getExpression());
                    return prepareElementMatchDocumentForAnyLambda(innerObject, field);
                }

            }

        }
        String field = member.getResourcePath().getUriResourceParts().get(0).toString();
        return prepareMemberDocument(field);
    }

    private Bson prepareExprDocumentForAnyLambdaWithExpr(Bson innerPart, String field, String lambdaVariable) {
        return new Document("$expr",
                new Document("$gt", Arrays.asList(
                        new Document("$size",
                                new Document("$filter",
                                        new Document("input", "$" + field)
                                                .append("as", lambdaVariable)
                                                .append("cond",
                                                        innerPart
                                                )
                                )
                        ),
                        0
                ))
        );
    }

    private Bson prepareElementMatchDocumentForAnyLambda(Bson innerPart, String field) {
        return new Document(field,
                new Document("$elemMatch",
                        innerPart
                )
        );
    }

    private Document prepareMemberDocument(String field) {
        Optional<EdmEntityType> entityType = edm.getSchemas().get(0).getEntityTypes().stream().findFirst();
        Document result = new Document(ODATA_MEMBER_PROPERTY, field);
        if (entityType.isPresent()) {
            EdmElement property = entityType.get().getProperty(field);
            if (property != null) {
                result.append(ODATA_MEMBER_TYPE_PROPERTY, property.getType().getFullQualifiedName().toString());
            }
        }
        return result;
    }

    private Document prepareMemberDocument(String field, EdmType edmType) {
        Document result = new Document(ODATA_MEMBER_PROPERTY, field);
        result.append(ODATA_MEMBER_TYPE_PROPERTY, edmType.getFullQualifiedName().toString());
        return result;
    }

    // --- Binary operators ---
    @Override
    public Bson visitBinaryOperator(BinaryOperatorKind operator, Bson left, Bson right)
            throws ExpressionVisitException, ODataApplicationException {
        switch (operator) {
            case EQ:
                return combineEq(left, right);
            case NE:
                return combineFieldOp(left, right, Filters::ne);
            case GT:
                return combineFieldOp(left, right, Filters::gt);
            case GE:
                return combineFieldOp(left, right, Filters::gte);
            case LT:
                return combineFieldOp(left, right, Filters::lt);
            case LE:
                return combineFieldOp(left, right, Filters::lte);
            case AND:
                if (this.context.isLambdaAnyContext() && !this.context.isExprMode() && !this.context.isElementMatchContext()) {
                    throw new ElementMatchOperantRequiredException("Required elementMatch");
                }
                return Filters.and(left, right);
            case OR:
                if (this.context.isLambdaAnyContext() && !this.context.isExprMode() && !this.context.isElementMatchContext()) {
                    throw new ElementMatchOperantRequiredException("Required elementMatch");
                }
                return Filters.or(left, right);
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
        switch (parameters.size()) {
            case 1:
                return visitMethodWithOneParameter(methodCall, parameters);
            case 2:
                return visitMethodWithTwoParameters(methodCall, parameters);
            default:
                throw new UnsupportedOperationException("Method not supported: " + methodCall);
        }
    }

    private Bson visitMethodWithTwoParameters(MethodKind methodCall, List<Bson> parameters) {
        String field = extractField(parameters.get(0));
        String value = extractValue(parameters.get(1));

//        switch (methodCall) {
//            case STARTSWITH:
//                return this.context.isExprMode() ? prepareRegexMatchExpr(field, Pattern.compile("^" + Pattern.quote(value)).pattern()) : Filters.regex(field, Pattern.compile("^" + Pattern.quote(value)));
//            case ENDSWITH:
//                return this.context.isExprMode() ? prepareRegexMatchExpr(field, Pattern.compile(Pattern.quote(value) + "$").pattern()) : Filters.regex(field, Pattern.compile(Pattern.quote(value) + "$"));
//            case CONTAINS:
//                return this.context.isExprMode() ? prepareRegexMatchExpr(field, Pattern.compile(Pattern.quote(value)).pattern()) : Filters.regex(field, Pattern.compile(Pattern.quote(value)));
//            default:
//                throw new UnsupportedOperationException("Method not supported: " + methodCall);
//        }
        switch (methodCall) {
            case STARTSWITH:
                return this.context.isExprMode() ? prepareRegexMatchExpr(field, Pattern.compile("^" + Pattern.quote(value)).pattern()) : this.context.isElementMatchContext() ? prepareRegexOperator(field,Pattern.compile("^" + Pattern.quote(value)).pattern()) : Filters.regex(field, Pattern.compile("^" + Pattern.quote(value)));
            case ENDSWITH:
                return this.context.isExprMode() ? prepareRegexMatchExpr(field, Pattern.compile(Pattern.quote(value) + "$").pattern()) : this.context.isElementMatchContext() ? prepareRegexOperator(field,Pattern.compile(Pattern.quote(value) + "$").pattern()) : Filters.regex(field, Pattern.compile(Pattern.quote(value) + "$"));
            case CONTAINS:
                return this.context.isExprMode() ? prepareRegexMatchExpr(field, Pattern.compile(Pattern.quote(value)).pattern()) : this.context.isElementMatchContext() ? prepareRegexOperator(field,Pattern.compile(Pattern.quote(value)).pattern()) : Filters.regex(field, Pattern.compile(Pattern.quote(value)));
            default:
                throw new UnsupportedOperationException("Method not supported: " + methodCall);
        }
    }

    private Bson prepareRegexOperator(String field, String regex) {
        return new Document("$regularExpression", new Document("pattern", regex).append("options", "i"));
//        return new Document(field, new Document("$regex", regex));
//        return new Document("$regex", regex);
//        return new Document("$regex", regex);
    }

    private Bson prepareRegexMatchExpr(String field, String regex) {
        return new Document("$regexMatch",
                new Document("input", field)
                        .append("regex", regex)
                        .append("options", "i")
        );
    }

    private Bson visitMethodWithOneParameter(MethodKind methodCall, List<Bson> parameters) {
        String field = extractField(parameters.get(0));
        Object value = extractValueObj(parameters.get(0));
        String type = extractFieldType(parameters.get(0));
        value = tryConvertValueByEdmType(value, type);
        Object passedValue = null;
        if (field != null) {
            passedValue = this.context.isExprMode() ? field : "$" + field;
        } else if (value != null) {
            passedValue = value;
        }
        ODataMongoFunctionMapper.MappedFunction mongoOperator = ODataMongoFunctionMapper.toOneArgumentMongoOperator(methodCall.toString());
        if (mongoOperator != null) {
            if (this.context.isLambdaAnyContext() && !mongoOperator.isResultBoolean()) {
                if (!this.context.isExprMode()) {
                    throw new ExpressionOperantRequiredException("Operant [%s] mapped from [%s] requires expr".formatted(mongoOperator.mappedFunction(), methodCall.toString()));
                }
            }
            return new Document(mongoOperator.mappedFunction(), passedValue);
        } else {

            switch (methodCall) {
                case TRIM:
                    return new Document("$trim", new Document("input", value));
            }
            throw new UnsupportedOperationException("Method not supported: " + methodCall);
        }
    }

    // --- Helpers ---
    private Bson combineEq(Bson left, Bson right) {
        String field = extractField(left);
        Object value = extractValueObj(right);
        String type = extractFieldType(left);
        value = tryConvertValueByEdmType(value, type);
        if (field == null) {
            return this.context.isExprMode() ? new Document("$eq", Arrays.asList(left, value == null ? right : value)) : new Document("$expr", new Document("$eq", Arrays.asList(left, value == null ? right : value)));
        }
        return Filters.eq(field, value);
    }

    private Bson combineFieldOp(Bson left, Bson right,
                                java.util.function.BiFunction<String, Object, Bson> fn) {
        String field = extractField(left);
        Object value = extractValueObj(right);
        String type = extractFieldType(left);
        value = tryConvertValueByEdmType(value, type);
        return fn.apply(field, value);
    }

    private String extractField(Bson bson) {
        BsonDocument document = bson.toBsonDocument();
        return document.containsKey(ODATA_MEMBER_PROPERTY) ? document.get(ODATA_MEMBER_PROPERTY).asString().getValue() : null;
    }

    private String extractValue(Bson bson) {
        return bson.toBsonDocument().get(CUSTOM_LITERAL_VALUE_PROPERTY).asString().getValue();
    }

    private Object extractValueObj(Bson bson) {
        return bson.toBsonDocument().get(CUSTOM_LITERAL_VALUE_PROPERTY);
    }

    private String extractFieldType(Bson field) {
        BsonDocument document = field.toBsonDocument();
        return document.containsKey(ODATA_MEMBER_TYPE_PROPERTY) ? document.get(ODATA_MEMBER_TYPE_PROPERTY).asString().getValue() : null;
    }

    private Object tryConvertValueByEdmType(Object value, String type) {
        if (value instanceof String && type != null) {
            return ODataToBsonConverter.toBsonValue((String) value, type);
        } else if (value instanceof BsonString && type != null) {
            return ODataToBsonConverter.toBsonValue(((BsonString) value).asString().getValue(), type);
        }
        return value;
    }

    // --- Not used in this example ---
    @Override
    public Bson visitAlias(String aliasName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bson visitTypeLiteral(EdmType edmType) throws ExpressionVisitException, ODataApplicationException {
        return null;
    }

    @Override
    public Bson visitLambdaExpression(String lambdaFunction, String lambdaVariable, Expression expression) {
        try {
            // Visit the inner expression
            Bson inner = expression.accept(this);
            return inner;

//            if ("any".equalsIgnoreCase(lambdaFunction)) {
//                // Extract the array field from inner expression
//                String field = extractField(inner);
//                Object value = extractValueObj(inner);
//
//                // If the inner expression is just `t eq 'literal'`
//                if (field != null && value != null) {
//                    return Filters.eq(field, value);
//                }
//                //TODO
////                return inner;
//
//                // Generic fallback: use $elemMatch with the inner filter
//                return Filters.elemMatch(field, inner);
//            } else if ("all".equalsIgnoreCase(lambdaFunction)) {
//                String field = extractField(inner);
//                Object value = extractValueObj(inner);
//                if (field != null && value != null) {
//                    return Filters.all(field, value);
//                }
//                return Filters.all(field, inner);
//            }
//
//            throw new UnsupportedOperationException("Lambda function not supported: " + lambdaFunction);

        } catch (ExpressionVisitException | ODataApplicationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Bson visitLambdaReference(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bson visitEnum(EdmEnumType edmEnumType, List<String> list) throws ExpressionVisitException, ODataApplicationException {
        return null;
    }

    @Override
    public Bson visitBinaryOperator(BinaryOperatorKind binaryOperatorKind, Bson bson, List<Bson> list) throws ExpressionVisitException, ODataApplicationException {
        return null;
    }

    @Builder
    public record MongoFilterVisitorContext(boolean isLambdaAnyContext, Map<String, Bson> lambdaVariableAliases,
                                            boolean isExprMode, boolean isElementMatchContext) {
    }

    private static class ExpressionOperantRequiredException extends RuntimeException {

        public ExpressionOperantRequiredException(String message) {
            super(message);
        }
    }

    private static class ElementMatchOperantRequiredException extends RuntimeException {

        public ElementMatchOperantRequiredException(String message) {
            super(message);
        }
    }
}
