package com.github.starnowski.mongo.fun.mongodb.container.odata;

import com.google.common.collect.Streams;
import com.mongodb.client.model.Filters;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import lombok.Builder;
import org.apache.olingo.commons.api.edm.*;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.*;
import org.apache.olingo.server.api.uri.queryoption.expression.*;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;

public class MongoFilterVisitor implements ExpressionVisitor<Bson> {

  public static final String CUSTOM_LITERAL_VALUE_PROPERTY = "$odata.literal";
  public static final String ODATA_MEMBER_PROPERTY = "$odata.member";
  public static final String ODATA_MEMBER_TYPE_PROPERTY = "$odata.member.type";
  public static final String ODATA_MEMBER_LAMBDA_ROOT_PROPERTY =
      "$odata.member.lambda.root.property";
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

  private static String extractFieldName(Member member) {
    return extractFieldName(member, 0);
  }

  private static String extractFieldName(Member member, int startIndex) {
    StringBuilder field = new StringBuilder();
    Iterator<UriResource> it =
        member
            .getResourcePath()
            .getUriResourceParts()
            .subList(startIndex, member.getResourcePath().getUriResourceParts().size())
            .iterator();
    boolean nested = false;
    while (it.hasNext()) {
      UriResource property = it.next();
      if (property instanceof UriResourcePrimitiveProperty
          || property instanceof UriResourceComplexProperty) {
        UriResourceProperty uiProperty = (UriResourceProperty) property;
        if (nested) {
          field.append(".");
        }
        field.append(uiProperty.getProperty().getName());
      } else {
        break;
      }
      nested = true;
    }
    return field.toString();
  }

  // --- Literals ---
  @Override
  public Bson visitLiteral(Literal literal) {
    String text = literal.getText();
    if ("null".equals(text)) {
      return literal(null);
    }
    if (text.startsWith("'") && text.endsWith("'")) {
      /*
       * Custom support of "normalize" method because there is a problem with adding custom method to Olingo project.
       */
      if (text.startsWith("'normalize('") && text.endsWith("')'")) {
        return literal(
            NormalizeHelper.normalize(
                text.substring(
                    "'normalize('".length() + 1,
                    text.length() - "')'".length() - 1))); // placeholder, field comes later
      }
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
      String field = extractFieldName(member);
      if (member.getResourcePath().getUriResourceParts().get(0)
          instanceof UriResourceLambdaVariable variable) {
        if ((this.context.isLambdaAnyContext() || this.context.isLambdaAllContext())
            && this.context.lambdaVariableAliases().containsKey(variable.getVariableName())) {
          if (this.context.isExprMode()) {
            return prepareMemberDocument("$$" + variable.getVariableName(), variable.getType());
          }
          return this.context.lambdaVariableAliases().get(variable.getVariableName());
        }
        return prepareMemberDocument(field, variable.getType());
      } else {
        return prepareMemberDocument(field);
      }
    } else if (member.getResourcePath().getUriResourceParts().get(0)
        instanceof UriResourceLambdaVariable variable) {
      String field = extractFieldName(member, 1);
      EdmType fieldType = null;
      if (member
              .getResourcePath()
              .getUriResourceParts()
              .get(member.getResourcePath().getUriResourceParts().size() - 1)
          instanceof UriResourceProperty uriResourceProperty) {
        fieldType = uriResourceProperty.getType();
      }
      if ((this.context.isLambdaAnyContext() || this.context.isLambdaAllContext())
          && this.context.lambdaVariableAliases().containsKey(variable.getVariableName())) {
        if (this.context.isExprMode()) {
          return prepareMemberDocument("$$" + variable.getVariableName() + "." + field, fieldType);
        }
        String rootPath =
            this.context
                .lambdaVariableAliases()
                .get(variable.getVariableName())
                .toBsonDocument()
                .get(ODATA_MEMBER_PROPERTY)
                .asString()
                .getValue();
        // Checking if another lambda is beaing used
        UriResource last =
            member
                .getResourcePath()
                .getUriResourceParts()
                .get(member.getResourcePath().getUriResourceParts().size() - 1);
        if (last instanceof UriResourceLambdaAny any) {
          return getBsonForUriResourceLambdaAny(any, field);
        } else if (last instanceof UriResourceLambdaAll all) {
          return getBsonForUriResourceLambdaAll(all, field);
        }

        if (this.context.isElementMatchContext()) {
          return prepareMemberDocumentForLambda(field, fieldType, rootPath);
        }
        return prepareMemberDocument(rootPath + "." + field, fieldType);
      }
      return prepareMemberDocument(field, variable.getType());
    } else {
      UriResource last =
          member
              .getResourcePath()
              .getUriResourceParts()
              .get(member.getResourcePath().getUriResourceParts().size() - 1);
      if (last instanceof UriResourceLambdaAny any) {
        String field = extractFieldName(member);
        return getBsonForUriResourceLambdaAny(any, field);
      } else if (last instanceof UriResourceLambdaAll all) {
        String field = extractFieldName(member);
        return getBsonForUriResourceLambdaAll(all, field);
      }
    }
    String field = extractFieldName(member);
    return prepareMemberDocument(field);
  }

  private Bson getBsonForUriResourceLambdaAll(UriResourceLambdaAll all, String field) {
    boolean expressionOperantRequiredExceptionThrown = false;
    boolean multipleElementMatchOperantRequiredExceptionThrown = false;
    boolean allVariantTested = false;
    MongoFilterVisitor visitor =
        new MongoFilterVisitor(
            edm,
            MongoFilterVisitorContext.builder()
                .lambdaVariableAliases(
                    Map.of(all.getLambdaVariable(), prepareMemberDocument(field)))
                .isLambdaAllContext(true)
                .isExprMode(expressionOperantRequiredExceptionThrown)
                .elementMatchContext(
                    new ElementMatchContext(
                        field, multipleElementMatchOperantRequiredExceptionThrown))
                .build());
    Supplier<Bson> function =
        () -> {
          Bson innerObject =
              visitor.visitLambdaExpression("ALL", all.getLambdaVariable(), all.getExpression());
          return visitor.context.isExprMode()
              ? prepareExprDocumentForAllLambdaWithExpr(innerObject, field, all.getLambdaVariable())
              : visitor.prepareElementMatchDocumentForAllLambda(innerObject, field, false);
        };
    while (!allVariantTested) {
      try {
        allVariantTested =
            expressionOperantRequiredExceptionThrown
                && multipleElementMatchOperantRequiredExceptionThrown;
        return function.get();
      } catch (ExpressionOperantRequiredException ex) {
        expressionOperantRequiredExceptionThrown = true;
        function =
            () -> {
              MongoFilterVisitor innerMongoFilterVisitor =
                  new MongoFilterVisitor(
                      edm,
                      MongoFilterVisitorContext.builder()
                          .lambdaVariableAliases(
                              Map.of(all.getLambdaVariable(), prepareMemberDocument(field)))
                          .isLambdaAllContext(true)
                          .isExprMode(true)
                          .build());
              Bson innerObject =
                  innerMongoFilterVisitor.visitLambdaExpression(
                      "ALL", all.getLambdaVariable(), all.getExpression());
              return prepareExprDocumentForAllLambdaWithExpr(
                  innerObject, field, all.getLambdaVariable());
            };
      } catch (ElementMatchOperantRequiredException ex) {
        MongoFilterVisitor innerMongoFilterVisitor =
            new MongoFilterVisitor(
                edm,
                MongoFilterVisitorContext.builder()
                    .lambdaVariableAliases(
                        Map.of(all.getLambdaVariable(), prepareMemberDocument(field)))
                    .isLambdaAllContext(true)
                    .isExprMode(expressionOperantRequiredExceptionThrown)
                    .elementMatchContext(
                        new ElementMatchContext(
                            field, multipleElementMatchOperantRequiredExceptionThrown))
                    .build());
        function =
            () -> {
              Bson innerObject =
                  innerMongoFilterVisitor.visitLambdaExpression(
                      "ALL", all.getLambdaVariable(), all.getExpression());
              return innerMongoFilterVisitor.context.isExprMode()
                  ? prepareExprDocumentForAllLambdaWithExpr(
                      innerObject, field, all.getLambdaVariable())
                  : prepareElementMatchDocumentForAllLambda(innerObject, field, false);
            };
      } catch (MultipleElementMatchOperantRequiredException ex) {
        multipleElementMatchOperantRequiredExceptionThrown = true;
        MongoFilterVisitor innerMongoFilterVisitor =
            new MongoFilterVisitor(
                edm,
                MongoFilterVisitorContext.builder()
                    .lambdaVariableAliases(
                        Map.of(all.getLambdaVariable(), prepareMemberDocument(field)))
                    .isLambdaAllContext(true)
                    .isExprMode(expressionOperantRequiredExceptionThrown)
                    .elementMatchContext(
                        new ElementMatchContext(
                            field, multipleElementMatchOperantRequiredExceptionThrown))
                    .build());
        function =
            () -> {
              Bson innerObject =
                  innerMongoFilterVisitor.visitLambdaExpression(
                      "ALL", all.getLambdaVariable(), all.getExpression());
              return innerMongoFilterVisitor.context.isExprMode()
                  ? prepareExprDocumentForAllLambdaWithExpr(
                      innerObject, field, all.getLambdaVariable())
                  : innerObject;
            };
      }
    }
    return null;
  }

  private Bson getBsonForUriResourceLambdaAny(UriResourceLambdaAny any, String field) {
    // TODO Fix Resolving like it was done to ALL lambda (check isExprMode() before return results)
    Supplier<Bson> function =
        () -> {
          MongoFilterVisitor innerMongoFilterVisitor =
              new MongoFilterVisitor(
                  edm,
                  MongoFilterVisitorContext.builder()
                      .lambdaVariableAliases(
                          Map.of(any.getLambdaVariable(), prepareMemberDocument(field)))
                      .isLambdaAnyContext(true)
                      .build());
          return innerMongoFilterVisitor.visitLambdaExpression(
              "ANY", any.getLambdaVariable(), any.getExpression());
        };

    boolean expressionOperantRequiredExceptionThrown = false;
    boolean elementMatchOperantRequiredExceptionThrown = false;
    boolean multipleElementMatchOperantRequiredExceptionThrown = false;
    boolean allVariantTested = false;
    while (!allVariantTested) {
      try {
        allVariantTested =
            expressionOperantRequiredExceptionThrown
                && elementMatchOperantRequiredExceptionThrown
                && multipleElementMatchOperantRequiredExceptionThrown;
        return function.get();
      } catch (ExpressionOperantRequiredException ex) {
        expressionOperantRequiredExceptionThrown = true;
        function =
            () -> {
              MongoFilterVisitor innerMongoFilterVisitor =
                  new MongoFilterVisitor(
                      edm,
                      MongoFilterVisitorContext.builder()
                          .lambdaVariableAliases(
                              Map.of(any.getLambdaVariable(), prepareMemberDocument(field)))
                          .isLambdaAnyContext(true)
                          .isExprMode(true)
                          .build());
              Bson innerObject =
                  innerMongoFilterVisitor.visitLambdaExpression(
                      "ANY", any.getLambdaVariable(), any.getExpression());
              return prepareExprDocumentForAnyLambdaWithExpr(
                  innerObject, field, any.getLambdaVariable());
            };
      } catch (ElementMatchOperantRequiredException ex) {
        elementMatchOperantRequiredExceptionThrown = true;
        MongoFilterVisitor innerMongoFilterVisitor =
            new MongoFilterVisitor(
                edm,
                MongoFilterVisitorContext.builder()
                    .lambdaVariableAliases(
                        Map.of(any.getLambdaVariable(), prepareMemberDocument(field)))
                    .isLambdaAnyContext(true)
                    .isExprMode(expressionOperantRequiredExceptionThrown)
                    .elementMatchContext(new ElementMatchContext(field, false))
                    .build());
        function =
            () -> {
              Bson innerObject =
                  innerMongoFilterVisitor.visitLambdaExpression(
                      "ANY", any.getLambdaVariable(), any.getExpression());
              return prepareElementMatchDocumentForAnyLambda(innerObject, field);
            };
      } catch (MultipleElementMatchOperantRequiredException ex) {
        multipleElementMatchOperantRequiredExceptionThrown = true;
        MongoFilterVisitor innerMongoFilterVisitor =
            new MongoFilterVisitor(
                edm,
                MongoFilterVisitorContext.builder()
                    .lambdaVariableAliases(
                        Map.of(any.getLambdaVariable(), prepareMemberDocument(field)))
                    .isLambdaAnyContext(true)
                    .isExprMode(expressionOperantRequiredExceptionThrown)
                    .elementMatchContext(new ElementMatchContext(field, true))
                    .build());
        function =
            () ->
                innerMongoFilterVisitor.visitLambdaExpression(
                    "ANY", any.getLambdaVariable(), any.getExpression());
      }
    }
    return null;
  }

  private Bson prepareExprDocumentForAnyLambdaWithExpr(
      Bson innerPart, String field, String lambdaVariable) {
    return new Document(
        "$expr",
        new Document(
            "$gt",
            Arrays.asList(
                new Document(
                    "$size",
                    new Document(
                        "$filter",
                        new Document("input", "$" + field)
                            .append("as", lambdaVariable)
                            .append("cond", innerPart))),
                0)));
  }

  private Bson prepareExprDocumentForAllLambdaWithExpr(
      Bson innerPart, String field, String lambdaVariable) {
    return new Document(
        "$expr",
        new Document(
            "$eq",
            Arrays.asList(
                new Document(
                    "$size",
                    new Document(
                        "$filter",
                        new Document("input", "$" + field)
                            .append("as", lambdaVariable)
                            .append("cond", innerPart))),
                new Document("$size", "$" + field))));
  }

  private Bson prepareElementMatchDocumentForAnyLambda(Bson innerPart, String field) {
    return new Document(field, new Document("$elemMatch", innerPart));
  }

  private Bson prepareElementMatchDocumentForAllLambda(
      Bson innerPart, String field, boolean returnUnwrappedBson) {
    if (this.context.isElementMatchContext()) {
      if (innerPart.toBsonDocument().containsKey(this.context.elementMatchContext().property())) {
        BsonValue innerValuePart =
            innerPart.toBsonDocument().get(this.context.elementMatchContext().property());
        if (!innerValuePart.isDocument() && !innerValuePart.isRegularExpression()) {
          if (returnUnwrappedBson) {
            return new Document("$eq", innerValuePart);
          }
          return new Document(
              field,
              new Document(
                  "$not", new Document("$elemMatch", new Document("$ne", innerValuePart))));
        }
        if (returnUnwrappedBson) {
          return innerValuePart.asDocument();
        }
        return new Document(
            field,
            new Document("$not", new Document("$elemMatch", new Document("$not", innerValuePart))));
      } else {
        BsonDocument doc = innerPart.toBsonDocument();
        if (doc.size() == 1 && !doc.getFirstKey().startsWith("$")) {
          String innerField = doc.getFirstKey();
          BsonValue innerValuePart = doc.get(innerField);
          Bson finalValue = null;
          if (!innerValuePart.isDocument() && !innerValuePart.isRegularExpression()) {
            finalValue = new Document("$eq", innerValuePart);
          }
          return new Document(
              field,
              new Document(
                  "$not",
                  new Document(
                      "$elemMatch",
                      new Document(
                          innerField,
                          new Document(
                              "$not", finalValue == null ? innerValuePart : finalValue)))));
        }
      }
      if (returnUnwrappedBson) {
        return innerPart.toBsonDocument();
      }
    }
    return new Document(
        field, new Document("$not", new Document("$elemMatch", new Document("$not", innerPart))));
  }

  private List<Bson> tryExtractElementMatchDocumentForAnyLambda(Bson innerPart, String field) {
    if (innerPart.toBsonDocument().containsKey("$or")) {
      return (List<Bson>) innerPart.toBsonDocument().get("$or");
    }
    return List.of(prepareElementMatchDocumentForAnyLambda(innerPart, field));
  }

  private List<Bson> tryExtractElementMatchDocumentForAllLambdaWithAndOperator(
      Bson innerPart, String field) {
    if (innerPart.toBsonDocument().containsKey("$and")) {
      return (List<Bson>) innerPart.toBsonDocument().get("$and");
    }
    return List.of(prepareElementMatchDocumentForAllLambda(innerPart, field, false));
  }

  private List<Bson> tryExtractElementMatchDocumentForAllLambdaWithOrOperator(
      Bson innerPart, String field) {
    if (innerPart.toBsonDocument().containsKey("$or")) {
      return (List<Bson>) innerPart.toBsonDocument().get("$or");
    }
    return List.of(prepareElementMatchDocumentForAllLambda(innerPart, field, true));
  }

  private Document prepareMemberDocument(String field) {
    Optional<EdmEntityType> entityType =
        edm.getSchemas().get(0).getEntityTypes().stream().findFirst();
    Document result = new Document(ODATA_MEMBER_PROPERTY, field);
    if (entityType.isPresent()) {
      EdmElement property = entityType.get().getProperty(field);
      if (property != null) {
        result.append(
            ODATA_MEMBER_TYPE_PROPERTY, property.getType().getFullQualifiedName().toString());
      }
    }
    return result;
  }

  private Document prepareMemberDocument(String field, EdmType edmType) {
    Document result = new Document(ODATA_MEMBER_PROPERTY, field);
    result.append(ODATA_MEMBER_TYPE_PROPERTY, edmType.getFullQualifiedName().toString());
    return result;
  }

  private Document prepareMemberDocumentForLambda(
      String field, EdmType edmType, String lambdaRootProperty) {
    Document result = prepareMemberDocument(field, edmType);
    result.append(ODATA_MEMBER_LAMBDA_ROOT_PROPERTY, lambdaRootProperty);
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
      case ADD:
        // TODO
        return combineFieldOp(left, right, (s, o) -> new Document("$add", Arrays.asList(s, o)));
      case AND:
        if ((this.context.isLambdaAnyContext() || this.context.isLambdaAllContext())
            && !this.context.isExprMode()
            && !this.context.isElementMatchContext()) {
          throw new ElementMatchOperantRequiredException(
              "Required elementMatch for operator [%s], left [%s], right [%s]"
                  .formatted(operator, left, right));
        }
        if (this.context.isElementMatchContext()) {
          if (this.context.isLambdaAllContext()
              && !this.context.elementMatchContext().multipleElemMatch()
              && !this.context.isExprMode()) {
            throw new MultipleElementMatchOperantRequiredException("Multiple elemMatch required");
          }
          if (this.context.isLambdaAllContext()
              && this.context.elementMatchContext().multipleElemMatch()) {
            BsonDocument leftDoc = left.toBsonDocument();
            BsonDocument rightDoc = right.toBsonDocument();
            Document leftPartDocument = new Document();
            Document partPartDocument = new Document();
            enrichDocumentWithQueryDocumentValues(leftDoc, leftPartDocument);
            enrichDocumentWithQueryDocumentValues(rightDoc, partPartDocument);
            List<Bson> andFilters =
                Streams.concat(
                        tryExtractElementMatchDocumentForAllLambdaWithAndOperator(
                            leftPartDocument, this.context.elementMatchContext().property())
                            .stream(),
                        tryExtractElementMatchDocumentForAllLambdaWithAndOperator(
                            partPartDocument, this.context.elementMatchContext().property())
                            .stream())
                    .toList();
            return new Document("$and", andFilters);
          }
          BsonDocument leftDoc = left.toBsonDocument();
          BsonDocument rightDoc = right.toBsonDocument();
          Document finalDOcument = new Document();
          Document leftPartDocument = new Document();
          Document partPartDocument = new Document();
          enrichDocumentWithQueryDocumentValues(leftDoc, leftPartDocument);
          enrichDocumentWithQueryDocumentValues(rightDoc, partPartDocument);

          // Checking keys conflict
          if (leftPartDocument.keySet().stream().anyMatch(partPartDocument::containsKey)) {
            throw new ExpressionOperantRequiredException("Operators duplicated!");
          }

          finalDOcument.putAll(leftPartDocument);
          finalDOcument.putAll(partPartDocument);
          return finalDOcument;
        }
        return Filters.and(left, right);
      case OR:
        if ((this.context.isLambdaAnyContext() || this.context.isLambdaAllContext())
            && !this.context.isExprMode()
            && !this.context.isElementMatchContext()) {
          throw new ElementMatchOperantRequiredException(
              "Required elementMatch for operator [%s], left [%s], right [%s]"
                  .formatted(operator, left, right));
        }
        if (this.context.isElementMatchContext()) {
          if (!this.context.elementMatchContext().multipleElemMatch()) {
            throw new MultipleElementMatchOperantRequiredException("Multiple elemMatch required");
          }
          if (this.context.isLambdaAnyContext()) {
            List<Bson> orFilters =
                Streams.concat(
                        tryExtractElementMatchDocumentForAnyLambda(
                            left, this.context.elementMatchContext().property())
                            .stream(),
                        tryExtractElementMatchDocumentForAnyLambda(
                            right, this.context.elementMatchContext().property())
                            .stream())
                    .toList();
            return new Document("$or", orFilters);
          } else if (this.context.isLambdaAllContext()) {
            // TODO FIX ALL
            if (!this.context.isExprMode()) {
              throw new ExpressionOperantRequiredException(
                  "ALL lambda for OR operator required expr");
            }
            List<Bson> orFilters =
                Streams.concat(
                        tryExtractElementMatchDocumentForAllLambdaWithOrOperator(
                            left, this.context.elementMatchContext().property())
                            .stream(),
                        tryExtractElementMatchDocumentForAllLambdaWithOrOperator(
                            right, this.context.elementMatchContext().property())
                            .stream())
                    .toList();
            // TODO validate if expression required
            Document main = new Document();
            main.putAll(orFilters.get(0).toBsonDocument());
            main.putAll(orFilters.get(1).toBsonDocument());
            return new Document(
                this.context.elementMatchContext().property(),
                new Document("$not", new Document("$elemMatch", new Document("$not", main))));
          }
        }
        return Filters.or(left, right);
      default:
        throw new UnsupportedOperationException("Operator not supported: " + operator);
    }
  }

  private void enrichDocumentWithQueryDocumentValues(BsonDocument doc, Document finalDOcument) {
    if (doc.containsKey(this.context.elementMatchContext().property())) {
      BsonValue value = doc.get(this.context.elementMatchContext().property());
      if (value.isDocument()) {
        finalDOcument.putAll(value.asDocument());
      } else {
        finalDOcument.append("$eq", value);
      }
    } else {
      finalDOcument.putAll(doc);
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
  public Bson visitMethodCall(MethodKind methodCall, List<Bson> parameters)
      throws ExpressionVisitException {
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
    String pattern = null;
    if (value == null || field == null) {
      if (!this.context.isExprMode()) {
        throw new ExpressionOperantRequiredException("value for regex pattern is null");
      }
    }
    switch (methodCall) {
      case STARTSWITH:
        if (value == null) {
          return new Document(
              "$eq",
              Arrays.asList(
                  new Document(
                      "$indexOfBytes",
                      Arrays.asList(
                          field == null ? parameters.get(0) : "$" + field, parameters.get(1))),
                  0));
        } else {
          pattern = "^" + Pattern.quote(value);
        }
        break;
      case ENDSWITH:
        if (value == null) {
          new Document(
              "$endsWith",
              new Document("input", field == null ? parameters.get(0) : "$" + field)
                  .append("suffix", parameters.get(1)));
          return new Document(
              "$eq",
              Arrays.asList(
                  new Document(
                      "$substrBytes",
                      Arrays.asList(
                          field == null ? parameters.get(0) : "$" + field,
                          new Document(
                              "$subtract",
                              Arrays.asList(
                                  new Document(
                                      "$strLenBytes",
                                      field == null ? parameters.get(0) : "$" + field),
                                  new Document("$strLenBytes", parameters.get(1)))),
                          new Document("$strLenBytes", parameters.get(1)))),
                  parameters.get(1)));
        } else {
          pattern = Pattern.quote(value) + "$";
        }
        break;
      case CONTAINS:
        if (value == null) {
          return new Document(
              "$gte",
              Arrays.asList(
                  new Document(
                      "$indexOfBytes",
                      Arrays.asList(
                          field == null ? parameters.get(0) : "$" + field, parameters.get(1))),
                  0));
        } else {
          pattern = Pattern.quote(value);
        }
        break;
      default:
        throw new UnsupportedOperationException("Method not supported: " + methodCall);
    }
    return this.context.isExprMode()
        ? prepareRegexMatchExpr(
            field == null ? parameters.get(0) : field, Pattern.compile(pattern).pattern())
        : this.context.isElementMatchContext()
            ? prepareRegexOperator(field, Pattern.compile(pattern).pattern())
            : Filters.regex(field, Pattern.compile(pattern));
  }

  private Bson prepareRegexOperator(String field, String regex) {
    Document regexObject = new Document("$regex", regex);
    if (field != null
        && !field.startsWith("$")
        && this.context.isElementMatchContext()
        && !field.equals(this.context.elementMatchContext().property())) {
      return new Document(field, regexObject);
    }
    return regexObject;
  }

  private Bson prepareRegexMatchExpr(Object input, String regex) {
    return new Document(
        "$regexMatch", new Document("input", input).append("regex", regex).append("options", "i"));
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
    ODataMongoFunctionMapper.MappedFunction mongoOperator =
        ODataMongoFunctionMapper.toOneArgumentMongoOperator(methodCall.toString());
    if (mongoOperator != null) {
      if ((this.context.isLambdaAnyContext() || this.context.isLambdaAllContext())
          && !mongoOperator.isResultBoolean()) {
        if (!this.context.isExprMode()) {
          throw new ExpressionOperantRequiredException(
              "Operant [%s] mapped from [%s] requires expr"
                  .formatted(mongoOperator.mappedFunction(), methodCall.toString()));
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
      return this.context.isExprMode()
          ? new Document("$eq", Arrays.asList(left, value == null ? right : value))
          : new Document(
              "$expr", new Document("$eq", Arrays.asList(left, value == null ? right : value)));
    }
    if (this.context.isExprMode()) {
      return new Document("$eq", Arrays.asList(field, value == null ? right : value));
    }
    return Filters.eq(field, value);
  }

  private Bson combineFieldOp(
      Bson left, Bson right, java.util.function.BiFunction<String, Object, Bson> fn) {
    String field = extractField(left);
    Object value = extractValueObj(right);
    String type = extractFieldType(left);
    value = tryConvertValueByEdmType(value, type);
    if (!this.context.isExprMode() && field == null) {
      throw new ExpressionOperantRequiredException(
          "The field is null for combineFieldOp, expression support is required");
    }
    if (this.context.isExprMode()) {
      String operatorKey = field == null ? "combineFieldOpOperatorKey" : field;
      Bson operatorObject = fn.apply(operatorKey, value);
      BsonDocument document = operatorObject.toBsonDocument();
      if (document.size() == 1 && document.containsKey(operatorKey)) {
        BsonValue operator = document.get(operatorKey);
        if (operator.isDocument()) {
          document = operator.asDocument();
          return new Document(
              document.getFirstKey(),
              Arrays.asList(field == null ? left : field, value == null ? right : value));
        }
      }
    }
    Bson result = fn.apply(field, value);
    if (this.context.isLambdaAllContext()) {
      if (!this.context.isElementMatchContext()) {
        throw new ElementMatchOperantRequiredException(
            "Required element match for the ALL lambda, left [%s], right [%s]"
                .formatted(left, right));
      }
      BsonDocument document = result.toBsonDocument();
      if (document.size() == 1 && document.containsKey(field)) {
        BsonValue operator = document.get(field);
        if (operator.isDocument()) {
          document = operator.asDocument();
          if (left.toBsonDocument().containsKey(ODATA_MEMBER_LAMBDA_ROOT_PROPERTY)
              && left.toBsonDocument().containsKey(ODATA_MEMBER_PROPERTY)) {
            return new Document(field, new Document(document.getFirstKey(), value));
          }
          return new Document(document.getFirstKey(), value);
        }
      }
    }
    return result;
  }

  private String extractField(Bson bson) {
    BsonDocument document = bson.toBsonDocument();
    return document.containsKey(ODATA_MEMBER_PROPERTY)
        ? document.get(ODATA_MEMBER_PROPERTY).asString().getValue()
        : null;
  }

  private String extractValue(Bson bson) {
    BsonDocument document = bson.toBsonDocument();
    return document.containsKey(CUSTOM_LITERAL_VALUE_PROPERTY)
        ? document.get(CUSTOM_LITERAL_VALUE_PROPERTY).asString().getValue()
        : null;
  }

  private Object extractValueObj(Bson bson) {
    return bson.toBsonDocument().get(CUSTOM_LITERAL_VALUE_PROPERTY);
  }

  private String extractFieldType(Bson field) {
    BsonDocument document = field.toBsonDocument();
    return document.containsKey(ODATA_MEMBER_TYPE_PROPERTY)
        ? document.get(ODATA_MEMBER_TYPE_PROPERTY).asString().getValue()
        : null;
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
  public Bson visitTypeLiteral(EdmType edmType)
      throws ExpressionVisitException, ODataApplicationException {
    return null;
  }

  @Override
  public Bson visitLambdaExpression(
      String lambdaFunction, String lambdaVariable, Expression expression) {
    try {
      // Visit the inner expression
      Bson inner = expression.accept(this);
      return inner;

    } catch (ExpressionVisitException | ODataApplicationException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Bson visitLambdaReference(String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Bson visitEnum(EdmEnumType edmEnumType, List<String> list)
      throws ExpressionVisitException, ODataApplicationException {
    return null;
  }

  @Override
  public Bson visitBinaryOperator(BinaryOperatorKind operator, Bson left, List<Bson> list)
      throws ExpressionVisitException, ODataApplicationException {
    switch (operator) {
      case IN:
        String field = extractField(left);
        String type = extractFieldType(left);
        List<Object> values =
            list.stream()
                .map(this::extractValueObj)
                .map(v -> tryConvertValueByEdmType(v, type))
                .toList();
        if (this.context.isExprMode()) {
          return new Document("$in", Arrays.asList(field, values));
        }
        return Filters.in(field, values);
      default:
        throw new UnsupportedOperationException("Operator not supported: " + operator);
    }
  }

  public record ElementMatchContext(String property, boolean multipleElemMatch) {}

  @Builder
  public record MongoFilterVisitorContext(
      boolean isLambdaAnyContext,
      Map<String, Bson> lambdaVariableAliases,
      boolean isExprMode,
      ElementMatchContext elementMatchContext,
      boolean isLambdaAllContext) {

    public boolean isElementMatchContext() {
      return elementMatchContext != null;
    }
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

  private static class MultipleElementMatchOperantRequiredException extends RuntimeException {

    public MultipleElementMatchOperantRequiredException(String message) {
      super(message);
    }
  }
}
