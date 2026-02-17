package com.github.starnowski.mongo.fun.mongodb.container.changestream;

import static com.github.starnowski.mongo.fun.mongodb.container.repositories.DaoProperties.POSTS_COLLECTION_NAME;

import com.github.starnowski.mongo.fun.mongodb.container.AbstractITTest;
import com.github.starnowski.mongo.fun.mongodb.container.model.Post;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.OperationType;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class CollectionChangeStreamListenerTest extends AbstractITTest {

  @Inject MongoClient mongoClient;

  @org.junit.jupiter.api.BeforeEach
  public void initReplicaSet() {
    //        System.out.println("Initializing replica set...");
    //        try {
    //            mongoClient.getDatabase("admin").runCommand(new Document("replSetInitiate", new
    // Document("_id", "rs0").append("members", java.util.List.of(new Document("_id",
    // 0).append("host", "localhost:27018")))));
    //            System.out.println("Replica set initialized.");
    //        } catch (Exception e) {
    //            System.out.println("Replica set initialization failed or already initialized: " +
    // e.getMessage());
    //        }
  }

  @Test
  public void testShouldListenToChangeStream() throws InterruptedException {
    // GIVEN
    String databaseName = "test"; // Default database name used in AbstractDao
    CollectionChangeStreamListener listener =
        new CollectionChangeStreamListener(mongoClient, databaseName, POSTS_COLLECTION_NAME);
    BlockingQueue<ChangeStreamDocument<Document>> events = new ArrayBlockingQueue<>(1);

    // WHEN
    listener.startListening(events::add);
    Thread.sleep(2000); // Wait for listener to start

    // Perform an operation that triggers the change stream
    Post post = new Post();
    post.setText("Test post");
    postDao.save(post);

    // THEN
    ChangeStreamDocument<Document> event = events.poll(20, TimeUnit.SECONDS);
    Assertions.assertNotNull(event, "Should have received a change stream event");
    Assertions.assertEquals(OperationType.INSERT, event.getOperationType());

    listener.stop();
  }
}
