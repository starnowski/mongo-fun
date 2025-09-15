package com.github.starnowski.mongo.fun.mongodb.container.test;

import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.quarkus.arc.Arc;
import org.bson.Document;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static com.github.starnowski.mongo.fun.mongodb.container.AbstractITTest.TEST_DATABASE;

public class MongoDatabaseSetupExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) throws IllegalAccessException {
        MongoSetup annotation = context.getTestMethod()
                .stream()
                .map(t -> t.getAnnotation(MongoSetup.class))
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
        if (annotation != null) {
            // Get test instance (the test class object)
            Object testInstance = context.getRequiredTestInstance();
            // Find MongoClient field
//            MongoClient mongoClient = null;
//            for (Field field : testInstance.getClass().getDeclaredFields()) {
//                if (MongoClient.class.isAssignableFrom(field.getType())) {
//                    field.setAccessible(true);
//                    mongoClient = (MongoClient) field.get(testInstance);
//                    break;
//                }
//            }

            MongoClient mongoClient = Arc.container().instance(MongoClient.class).get();

            if (mongoClient == null) {
                throw new IllegalStateException("No MongoClient field found in test class: "
                        + testInstance.getClass().getName());
            }

            Set<String> collectionNames = new HashSet<>();
            Arrays.stream(annotation.mongoDocuments()).forEach(an -> {
                collectionNames.add(an.collection());
            });

            MongoDatabase database = mongoClient.getDatabase(TEST_DATABASE); // change if needed
            collectionNames.forEach(collectionName -> {
                MongoCollection<Document> collection = database.getCollection(collectionName);
                collection.withWriteConcern(WriteConcern.W1); // no .withJournal(true)
                collection.deleteMany(new Document()); // clears collection
            });

            Arrays.stream(annotation.mongoDocuments()).forEach(an -> {
                MongoCollection<Document> collection = database.getCollection(an.collection());
                try {
                    String bson = Files.readString(Paths.get(new File(getClass().getClassLoader().getResource(an.bsonFilePath()).getFile()).getPath()));
                    collection.insertOne(Document.parse(bson));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}