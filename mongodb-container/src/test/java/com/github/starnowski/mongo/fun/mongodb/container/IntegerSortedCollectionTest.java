package com.github.starnowski.mongo.fun.mongodb.container;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension.class)
@ContextConfiguration
public class IntegerSortedCollectionTest {

    @Autowired
    private MongoTemplate mongoTemplate;
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
        Assertions.assertEquals(1000, projection.get(0).getInteger("count"));
    }

}
