package com.github.starnowski.mongo.fun.mongodb.container;

import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Sorts;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest
@QuarkusTestResource(EmbeddedMongoResource.class)
public class IntegerSortedCollectionTest {
    private static final String INTEGER_COLUMN = "integer_col";

    @Inject
    private MongoClient mongoClient;
    private MongoCollection<Document> sortable;

    private static Stream<Arguments> provide_shouldReturnCorrectCountNumberAfterSeveralStagesWithDifferentCursorOperations() {
        return Stream.of(
                Arguments.of(asList(Aggregates.limit(600), Aggregates.skip(170), Aggregates.skip(200)), 230, "limit 600, skip 170, skip 200"),
                Arguments.of(asList(Aggregates.skip(300), Aggregates.limit(400), Aggregates.skip(200)), 200, "skip 300, limit 400, skip 200")
        );
    }

    private static Stream<Arguments> provide_shouldReturnCorrectFirstAndLastResultAfterSeveralStagesWithDifferentCursorOperations() {
        return Stream.of(
                Arguments.of(asList(Aggregates.limit(600), Aggregates.skip(170), Aggregates.skip(200)), 370, 599, "limit 600, skip 170, skip 200"),
                Arguments.of(asList(Aggregates.limit(600), Aggregates.skip(170), Aggregates.skip(200), Aggregates.sort(Sorts.descending(INTEGER_COLUMN))), 599, 370, "limit 600, skip 170, skip 200, sort ascending"),
                Arguments.of(asList(Aggregates.limit(600), Aggregates.skip(170), Aggregates.skip(200), Aggregates.sort(Sorts.descending(INTEGER_COLUMN)), Aggregates.skip(200)), 399, 370, "limit 600, skip 170, skip 200, sort ascending, skip 200")
        );
    }

    @BeforeAll
    public void setUp() {
        sortable = mongoClient.getDatabase("test").getCollection("integerSortedCollection");
        sortable.deleteMany(new Document());
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            documents.add(new Document(INTEGER_COLUMN, i));
        }
        sortable.insertMany(documents);
    }

    @Test
    @DisplayName("should return 1000 as count result")
    public void shouldReturnCorrectNumberForCountOperation() {
        // GIVEN
        int expectedCount = 1000;
        Bson countBson = Aggregates.count();
        List<Document> projection = new ArrayList<>();

        // WHEN
        sortable.aggregate(singletonList(countBson)).into(projection);

        // THEN
        Assertions.assertEquals(1, projection.size());
        Assertions.assertEquals(expectedCount, projection.get(0).getInteger("count"));
    }

    @Test
    @DisplayName("should return 500 as count result after stage that limits result to 500 records")
    public void shouldReturnCorrectNumberForCountOperationAfterLimit() {
        // GIVEN
        int expectedCount = 500;
        Bson limitBson = Aggregates.limit(500);
        Bson countBson = Aggregates.count();
        List<Document> projection = new ArrayList<>();
        List<Bson> pipeline = new ArrayList<>();
        pipeline.add(limitBson);
        pipeline.add(countBson);

        // WHEN
        sortable.aggregate(pipeline).into(projection);

        // THEN
        Assertions.assertEquals(1, projection.size());
        Assertions.assertEquals(expectedCount, projection.get(0).getInteger("count"));
    }

    @Test
    @DisplayName("should return 314 as count result after stage that skips 686 records")
    public void shouldReturnCorrectCountNumberAfterSkipping() {
        // GIVEN
        int expectedCount = 314;
        Bson skipBson = Aggregates.skip(686);
        Bson countBson = Aggregates.count();
        List<Document> projection = new ArrayList<>();
        List<Bson> pipeline = new ArrayList<>();
        pipeline.add(skipBson);
        pipeline.add(countBson);

        // WHEN
        sortable.aggregate(pipeline).into(projection);

        // THEN
        Assertions.assertEquals(1, projection.size());
        Assertions.assertEquals(expectedCount, projection.get(0).getInteger("count"));
    }

    @ParameterizedTest(name = "should return expected count number {1} after several stage operations : {2}")
    @MethodSource("provide_shouldReturnCorrectCountNumberAfterSeveralStagesWithDifferentCursorOperations")
    public void shouldReturnCorrectCountNumberAfterSeveralStagesWithDifferentCursorOperations(List<Bson> stages, int expectedCount, String message) {
        // GIVEN
        List<Document> projection = new ArrayList<>();
        List<Bson> pipeline = new ArrayList<>();
        pipeline.addAll(stages);
        pipeline.add(Aggregates.count());

        // WHEN
        sortable.aggregate(pipeline).into(projection);

        // THEN
        Assertions.assertEquals(1, projection.size());
        Assertions.assertEquals(expectedCount, projection.get(0).getInteger("count"));
    }

    @ParameterizedTest(name = "should return expected first number {1} and last number {2} after several stage operations : {3}")
    @MethodSource("provide_shouldReturnCorrectFirstAndLastResultAfterSeveralStagesWithDifferentCursorOperations")
    public void shouldReturnCorrectFirstAndLastResultAfterSeveralStagesWithDifferentCursorOperations(List<Bson> pipeline, int first, int last, String message) {
        // GIVEN
        List<Document> results = new ArrayList<>();

        // WHEN
        sortable.aggregate(pipeline).into(results);

        // THEN
        Assertions.assertEquals(first, results.get(0).getInteger(INTEGER_COLUMN));
        Assertions.assertEquals(last, results.get(results.size() - 1).getInteger(INTEGER_COLUMN));
    }
}
