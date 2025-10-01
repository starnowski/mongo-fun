package com.github.starnowski.mongo.fun.mongodb.container.repositories;

import com.github.starnowski.mongo.fun.mongodb.container.test.MongoDatabaseSetupExtension;
import com.github.starnowski.mongo.fun.mongodb.container.test.MongoDocument;
import com.github.starnowski.mongo.fun.mongodb.container.test.MongoSetup;
import com.mongodb.MongoCommandException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static com.github.starnowski.mongo.fun.mongodb.container.AbstractITTest.TEST_DATABASE;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@ExtendWith(MongoDatabaseSetupExtension.class)
class ExampleDaoTest {

    @Inject
    private MongoClient mongoClient;

    public static Stream<Arguments> provideTestPipeline() {
        return Stream.of(
                Arguments.of(Aggregates.match(
                        Filters.elemMatch("tags",
                                Filters.and(
                                        new Document("$regex", "x").append("$options", "i"),
                                        new Document("$regex", "y").append("$options", "i")
                                )
                                )),
                            0
                        ),
                Arguments.of(Aggregates.match(
                                Filters.elemMatch("tags",
                                        Filters.and(
                                                new Document("$regex", "x").append("$options", "i")
                                        )
                                )),
                        0
                ),
                Arguments.of(Aggregates.match(
                                Filters.elemMatch("tags",
                                        Filters.or(
                                                new Document("$regex", "x").append("$options", "i")
                                        )
                                )),
                        0
                ),
                Arguments.of(Aggregates.match(
                                Filters.elemMatch("tags",
                                        new Document("$regex", "x").append("$options", "i")
                                )),
                        0
                ),
                Arguments.of(Aggregates.match(
                                Filters.elemMatch("tags",
                                        Filters.and(
                                                new Document("$eq", "x")
                                        )
                                )),
                        0
                ),
                //Command failed with error 2 (BadValue): '$elemMatch needs an Object'
                Arguments.of(Aggregates.match(
                                new Document("tags",
                                                new Document("$elemMatch", Arrays.asList(
                                                        new Document("$regex", "x").append("$options", "i"),
                                                        new Document("$regex", "y").append("$options", "i")
                                                        )
                                                    )
                                                )
                                ),
                        0
                )
        );
    }


    @ParameterizedTest
    @MethodSource({
            "provideTestPipeline"

    })
    @MongoSetup(mongoDocuments = {
            @MongoDocument(bsonFilePath = "examples/query/example2_1.json", collection = "examples"),
            @MongoDocument(bsonFilePath = "examples/query/example2_2.json", collection = "examples"),
            @MongoDocument(bsonFilePath = "examples/query/example2_3.json", collection = "examples")
    })
    public void testPipeline(Bson matchStage, long expectedCount){

        // WHEN
        try {
            ArrayList<Document> documents = mongoClient.getDatabase(TEST_DATABASE).getCollection("examples").aggregate(List.of(matchStage)).into(new ArrayList<>());
            Assertions.fail("It works");
        } catch (Exception ex) {
            System.out.println(ex);
            assertEquals(MongoCommandException.class, ex.getClass());
            assertEquals(2, ((MongoCommandException)ex).getErrorCode());
        }
        // THEN
//        assertEquals(expectedCount, documents.size());
    }
}