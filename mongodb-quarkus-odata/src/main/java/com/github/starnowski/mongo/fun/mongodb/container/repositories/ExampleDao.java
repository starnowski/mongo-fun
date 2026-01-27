package com.github.starnowski.mongo.fun.mongodb.container.repositories;

import com.github.starnowski.mongo.fun.mongodb.container.odata.Example2StaticEdmSupplier;
import com.github.starnowski.mongo.fun.mongodb.container.odata.ODataToMongoParser;
import com.github.starnowski.mongo.fun.mongodb.container.odata.OdataOrderToMongoSortParser;
import com.github.starnowski.mongo.fun.mongodb.container.odata.OdataSelectToMongoProjectParser;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.core.uri.parser.Parser;
import org.apache.olingo.server.core.uri.parser.UriParserException;
import org.apache.olingo.server.core.uri.validator.UriValidationException;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.json.JsonWriterSettings;

import java.util.*;
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

    public List<Document> query(List<String> filters, List<String> orders, List<String> select) throws ExpressionVisitException, ODataApplicationException, UriValidationException, UriParserException {
        List<Bson> pipeline = preparePipelineBasedOnFilter(filters, orders, select);
        System.out.println("pipeline: " + pipeline);
        return pipeline.isEmpty() ? new ArrayList<>() : getCollection().aggregate(pipeline).into(new ArrayList<>());
    }

    public String explain(List<String> filters, List<String> orders, List<String> select) throws ExpressionVisitException, ODataApplicationException, UriValidationException, UriParserException {
        List<Bson> pipeline = preparePipelineBasedOnFilter(filters, orders, select);

        System.out.println(new Document("pipeline", pipeline).toJson(getCollection().getCodecRegistry().get(Document.class)));

        // Run explain on the aggregation
        Document explain = getCollection().aggregate(pipeline).explain();
        System.out.println(explain.toJson());

        // Navigate to winning plan
        Document queryPlanner = (Document) explain.get("queryPlanner");

        if (queryPlanner == null) {
            //Extracting query plan for $cursor
            queryPlanner = (Document) Optional.ofNullable(explain.get("stages")).filter(stages -> stages instanceof List)
                    .map(stages -> (List)stages).orElse(List.of()).stream().filter(i -> i instanceof Document)
                    .filter(i -> ((Document)i).containsKey("$cursor")).findFirst()
                    .map(c -> ((Document)c).get("$cursor")).or(() -> new Document())
                    .map(c -> ((Document)c).get("queryPlanner")).orElse(null);
        }

        if (queryPlanner == null) {
            System.out.println("No query planner info found in explain output.");
            return null;
        }

        Document winningPlan = (Document) queryPlanner.get("winningPlan");
        String stage = winningPlan.getString("stage");

        System.out.println("Winning plan stage: " + stage);

        // Check index usage
        if ("IXSCAN".equals(stage)) {
            System.out.println("✅ Pure index scan (covered aggregation).");
            return "IXSCAN";
        } else if ("FETCH".equals(stage)) {
            Document inputStage = (Document) winningPlan.get("inputStage");
            if (inputStage != null && "IXSCAN".equals(inputStage.getString("stage"))) {
                System.out.println("✅ Index scan with fetch (aggregation not covered, but index is used).");
                return "FETCH + IXSCAN";
            }
            return "FETCH";
        } else if ("COLLSCAN".equals(stage)) {
            System.out.println("❌ Collection scan (no index used in aggregation).");
            return "COLLSCAN";
        } else {
            System.out.println("ℹ️ Other plan stage: " + stage);
        }
        return tryToResolveKnowStage(winningPlan);
    }

    private String tryToResolveKnowStage(Document winningPlan) {
        String stage = winningPlan.getString("stage");
        if ("IXSCAN".equals(stage) || "COLLSCAN".equals(stage)) {
            return stage;
        }
        boolean fetchExists = "FETCH".equals(stage);
        if (winningPlan.containsKey("inputStage")) {
            String innerStage = tryToResolveKnowStage((Document) winningPlan.get("inputStage"));
//            if ("COLLSCAN".equals(innerStage)) {
//                return innerStage;
//            }
            if ("IXSCAN".equals(innerStage)) {
                return fetchExists ? "FETCH + IXSCAN" : innerStage;
            }
            return innerStage;
        } else if (winningPlan.containsKey("inputStages")) {
            List<Document> inputStages = winningPlan.getList("inputStages", Document.class);
            Set<String> stages = inputStages.stream().map(
                    this::tryToResolveKnowStage).filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
            if (stages.contains("COLLSCAN")) {
                return "COLLSCAN";
            }
            if (stages.equals(Set.of("IXSCAN"))) {
                return fetchExists ? "FETCH + IXSCAN" : "IXSCAN";
            }
            System.out.println("Resolved stages are: " + stages);
            return stages.stream().findFirst().orElse(null);
        }
        return null;
    }

    private List<Bson> preparePipelineBasedOnFilter(List<String> filters, List<String> orders, List<String> select) throws UriValidationException, UriParserException, ExpressionVisitException, ODataApplicationException {
        List<Bson> pipeline = new ArrayList<>();

        if (filters != null && !filters.isEmpty() &&
                filters.stream().filter(Objects::nonNull).anyMatch(filter -> !filter.trim().isEmpty())) {
            // Parse OData $filter into UriInfo (simplified)
            String filterString = "$filter=" +
                    filters.stream().filter(Objects::nonNull)
                            .filter(filter -> !filter.trim().isEmpty())
                            .collect(Collectors.joining(" and "));
            UriInfo uriInfo = new Parser(example2StaticEdmSupplier.get(), OData.newInstance())
                    .parseUri("examples2",
                            // https://docs.oasis-open.org/odata/odata/v4.0/os/part2-url-conventions/odata-v4.0-os-part2-url-conventions.html?utm_source=chatgpt.com
                            /*
                             * "The same system query option MUST NOT be specified more than once for any resource."
                             */
                            filterString
                            , null, null);

            FilterOption filterOption = uriInfo.getFilterOption();
            if (filterOption != null) {
                Bson bsonFilter = ODataToMongoParser.parseFilter(uriInfo, example2StaticEdmSupplier.get());
                pipeline.add(Aggregates.match(Filters.and(bsonFilter)));
                try {
                    System.out.println("<test>");
                    System.out.println("<filter>" + filterString + "</filter>");
                    System.out.println("<pipeline>" + Aggregates.match(Filters.and(bsonFilter)).toBsonDocument().toJson() + "</pipeline>");
                    System.out.println("</test>");
                } catch (Exception exception) {
                    //TODO do nothing
                }
            }

        }
        if (orders != null && !orders.isEmpty() &&
                orders.stream().filter(Objects::nonNull).anyMatch(order -> !order.trim().isEmpty())) {
            // Parse OData $orderby into UriInfo (simplified)
            UriInfo uriInfo = new Parser(example2StaticEdmSupplier.get(), OData.newInstance())
                    .parseUri("examples2",
                            "$orderby=" +
                                    orders.stream().filter(Objects::nonNull)
                                            .filter(order -> !order.trim().isEmpty())
                                            .collect(Collectors.joining(","))
                            , null, null);
            //OdataOrderToMongoSortParser
            OrderByOption orderOption = uriInfo.getOrderByOption();
            if (orderOption != null) {
                Bson bsonFilter = OdataOrderToMongoSortParser.parseOrder(uriInfo, example2StaticEdmSupplier.get());
                pipeline.add(Aggregates.sort(bsonFilter));
            }
        }
        if (select != null && !select.isEmpty() &&
                select.stream().filter(Objects::nonNull).anyMatch(s -> !s.trim().isEmpty())) {
            // Parse OData $select into UriInfo (simplified)
            UriInfo uriInfo = new Parser(example2StaticEdmSupplier.get(), OData.newInstance())
                    .parseUri("examples2",
                            "$select=" +
                                    select.stream().filter(Objects::nonNull)
                                            .filter(s -> !s.trim().isEmpty())
                                            .collect(Collectors.joining(","))
                            , null, null);
            //OdataSelectToMongoProjectParser
            SelectOption selectOption = uriInfo.getSelectOption();
            if (selectOption != null) {
                Bson bsonFilter = OdataSelectToMongoProjectParser.buildProjection(selectOption);
                BsonDocument doc = bsonFilter.toBsonDocument();
                doc.append("_id", new BsonInt32(0));
                bsonFilter = doc;
                pipeline.add(Aggregates.project(bsonFilter));
                printBSONDocument(Aggregates.project(bsonFilter));
            }
        }
        return pipeline;
    }

    private void printBSONDocument(Bson bson) {
        JsonWriterSettings settings = JsonWriterSettings.builder()
                .outputMode(org.bson.json.JsonMode.RELAXED)
                .indent(true)
                .build();

        String json = bson.toBsonDocument().toJson(settings);
        System.out.println(json);
    }
}