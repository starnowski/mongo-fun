package com.github.starnowski.mongo.fun;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;

@DataMongoTest
@TestInstance(Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension.class)
public class IntegerSortedCollectionTest {

    @Autowired
    private  MongoTemplate mongoTemplate;
    private MongoCollection<Document> sortable;

    @BeforeAll
    public void setUp() {
        sortable = mongoTemplate.getCollection("integerSortedCollection");
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            documents.add(new Document("i", i));
        }
        sortable.insertMany(documents);
    }

    @Test
    @DisplayName("should return 1000 as count result")
    public void shouldReturnCorrectNumberForCountOperation()
    {
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
    public void shouldReturnCorrectNumberForCountOperationAfterLimit()
    {
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
    public void shouldReturnCorrectCountNumberAfterSkipping()
    {
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

}
