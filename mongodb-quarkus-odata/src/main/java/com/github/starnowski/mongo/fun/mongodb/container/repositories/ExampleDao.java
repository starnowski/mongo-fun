package com.github.starnowski.mongo.fun.mongodb.container.repositories;

import com.github.starnowski.mongo.fun.mongodb.container.odata.Example2StaticEdmSupplier;
import com.github.starnowski.mongo.fun.mongodb.container.odata.ODataToMongoParser;
import com.github.starnowski.mongo.fun.mongodb.container.odata.OdataOrderToMongoSortParser;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.core.uri.parser.Parser;
import org.apache.olingo.server.core.uri.parser.UriParserException;
import org.apache.olingo.server.core.uri.validator.UriValidationException;
import org.bson.Document;
import org.bson.conversions.Bson;

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

    public List<Document> query(List<String> filters) throws ExpressionVisitException, ODataApplicationException, UriValidationException, UriParserException {
        List<Bson> pipeline = preparePipelineBasedOnFilter(filters);
        System.out.println("pipeline: " + pipeline);
        return pipeline.isEmpty() ? new ArrayList<>() : getCollection().aggregate(pipeline).into(new ArrayList<>());
    }

    public String explain(List<String> filters) throws ExpressionVisitException, ODataApplicationException, UriValidationException, UriParserException {
        List<Bson> pipeline = preparePipelineBasedOnFilter(filters);

        // Run explain on the aggregation
        Document explain = getCollection().aggregate(pipeline).explain();

        // Navigate to winning plan
        Document queryPlanner = (Document) explain.get("queryPlanner");
//                (Document) explain.get("stages", List.of())
//                .stream()
//                .filter(o -> ((Document) o).containsKey("$cursor"))
//                .map(o -> (Document) o)
//                .findFirst()
//                .map(o -> (Document) ((Document) o.get("$cursor")).get("queryPlanner"))
//                .orElse(null);

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

    private List<Bson> preparePipelineBasedOnFilter(List<String> filters) throws UriValidationException, UriParserException, ExpressionVisitException, ODataApplicationException {
        List<Bson> pipeline = new ArrayList<>();

        if (filters != null && !filters.isEmpty() &&
                filters.stream().filter(Objects::nonNull).anyMatch(filter -> !filter.trim().isEmpty())) {
            // Parse OData $filter into UriInfo (simplified)
            UriInfo uriInfo = new Parser(example2StaticEdmSupplier.get(), OData.newInstance())
                    .parseUri("examples2",
                            // https://docs.oasis-open.org/odata/odata/v4.0/os/part2-url-conventions/odata-v4.0-os-part2-url-conventions.html?utm_source=chatgpt.com
                            /*
                             * "The same system query option MUST NOT be specified more than once for any resource."
                             */
                            "$filter=" +
                            filters.stream().filter(Objects::nonNull)
                                    .filter(filter -> !filter.trim().isEmpty())
                                    .collect(Collectors.joining(" and "))
                            , null, null);

            FilterOption filterOption = uriInfo.getFilterOption();
            if (filterOption != null) {
                Bson bsonFilter = ODataToMongoParser.parseFilter(uriInfo, example2StaticEdmSupplier.get());
                pipeline.add(Aggregates.match(Filters.and(bsonFilter)));
            }

            //OdataOrderToMongoSortParser
            OrderByOption orderOption = uriInfo.getOrderByOption();
            if (orderOption != null) {
                Bson bsonFilter = OdataOrderToMongoSortParser.parseOrder(uriInfo, example2StaticEdmSupplier.get());
                pipeline.add(bsonFilter);
            }

        }
        return pipeline;
    }
}