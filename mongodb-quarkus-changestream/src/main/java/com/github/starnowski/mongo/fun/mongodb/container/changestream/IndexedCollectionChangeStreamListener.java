package com.github.starnowski.mongo.fun.mongodb.container.changestream;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import jakarta.annotation.PreDestroy;
import org.bson.Document;
import org.jboss.logging.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

public class IndexedCollectionChangeStreamListener {

    private static final Logger LOGGER = Logger.getLogger(IndexedCollectionChangeStreamListener.class);

    private final MongoClient mongoClient;
    private final MongoCollection<Document> collection;
    private final ExecutorService executorService;
    private volatile boolean running = true;

    public IndexedCollectionChangeStreamListener(MongoClient mongoClient, String databaseName, String collectionName) {
        this.mongoClient = mongoClient;
        this.collection = mongoClient.getDatabase(databaseName).getCollection(collectionName);
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void startListening(BiConsumer<ChangeStreamDocument<Document>, Long> consumer) {
        executorService.submit(() -> {
            long counter = 0;
            LOGGER.infof("Started listening to change stream for collection: %s", collection.getNamespace().getFullName());
            try {
                LOGGER.info("Attempting to watch collection...");
                try (MongoCursor<ChangeStreamDocument<Document>> cursor = collection.watch().iterator()) {
                    LOGGER.info("Watching collection successfully.");
                    while (running && cursor.hasNext()) {
                        ChangeStreamDocument<Document> event = cursor.next();
                        LOGGER.infof("Received change stream event: %s", event);
                        consumer.accept(event, counter++);
                    }
                }
            } catch (Exception e) {
                if (running) {
                    LOGGER.error("Error in change stream listener", e);
                }
            }
        });
    }

    @PreDestroy
    public void stop() {
        running = false;
        executorService.shutdownNow();
        LOGGER.info("Stopped change stream listener");
    }
}
