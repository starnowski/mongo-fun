package com.github.starnowski.mongo.fun.mongodb.container.changestream;

import static com.github.starnowski.mongo.fun.mongodb.container.repositories.DaoProperties.POSTS_COLLECTION_NAME;

import com.github.starnowski.mongo.fun.mongodb.container.AbstractITTest;
import com.github.starnowski.mongo.fun.mongodb.container.model.Post;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.OperationType;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.bson.BsonDocument;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class IndexedCollectionChangeStreamListenerTest extends AbstractITTest {

  @Inject MongoClient mongoClient;

  @Test
  public void testShouldListenToChangeStreamWithCounter() throws InterruptedException {
    // GIVEN
    String databaseName = "test";
    IndexedCollectionChangeStreamListener listener =
        new IndexedCollectionChangeStreamListener(mongoClient, databaseName, POSTS_COLLECTION_NAME);
    BlockingQueue<EventWithCounter> events = new ArrayBlockingQueue<>(2);

    // WHEN
    listener.startListening((event, counter) -> events.add(new EventWithCounter(event, counter)));
    Thread.sleep(2000); // Wait for listener to start

    // Perform operations
    Post post1 = new Post();
    post1.setText("Test post 1");
    postDao.save(post1);

    Post post2 = new Post();
    post2.setText("Test post 2");
    postDao.save(post2);

    // THEN
    EventWithCounter result1 = events.poll(20, TimeUnit.SECONDS);
    Assertions.assertNotNull(result1, "Should have received first event");
    Assertions.assertEquals(OperationType.INSERT, result1.event.getOperationType());

    EventWithCounter result2 = events.poll(20, TimeUnit.SECONDS);
    Assertions.assertNotNull(result2, "Should have received second event");
    Assertions.assertEquals(OperationType.INSERT, result2.event.getOperationType());
    Assertions.assertEquals(Set.of(0L, 1L), Set.of(result1.counter, result2.counter));

    listener.stop();
    Assertions.assertEquals(0, events.size()); // No more events processed
  }

  @Test
  public void
      testShouldListenToChangeStreamWithCounterAndListenAgainAndStartAfterPassedResumeToken()
          throws InterruptedException {
    // GIVEN
    String databaseName = "test";
    IndexedCollectionChangeStreamListener listener =
        new IndexedCollectionChangeStreamListener(mongoClient, databaseName, POSTS_COLLECTION_NAME);
    BlockingQueue<EventWithCounter> events = new ArrayBlockingQueue<>(4);

    // WHEN
    listener.startListening(
        (event, counter) -> {
          if (counter < 2) {
            events.add(new EventWithCounter(event, counter));
          }
        });
    Thread.sleep(2000); // Wait for listener to start

    // Perform operations

    extracted(4);

    // THEN
    EventWithCounter result1 = events.poll(20, TimeUnit.SECONDS);
    Assertions.assertNotNull(result1, "Should have received first event");
    Assertions.assertEquals(OperationType.INSERT, result1.event.getOperationType());

    EventWithCounter result2 = events.poll(20, TimeUnit.SECONDS);
    Assertions.assertNotNull(result2, "Should have received second event");
    Assertions.assertEquals(OperationType.INSERT, result2.event.getOperationType());
    Assertions.assertEquals(Set.of(0L, 1L), Set.of(result1.counter, result2.counter));
    listener.stop();
    Assertions.assertEquals(0, events.size()); // No more events processed

    BsonDocument lastResumeToken =
        result1.counter == 1L ? result1.event.getResumeToken() : result2.event.getResumeToken();
    String lastToken = lastResumeToken.toJson();
    // Running listener again to process the last two MongoDB documents

    listener =
        new IndexedCollectionChangeStreamListener(mongoClient, databaseName, POSTS_COLLECTION_NAME);

    // WHEN
    listener.startListening(
        (event, counter) -> events.add(new EventWithCounter(event, counter)), lastToken);

    Thread.sleep(2000); // Wait for listener to start
    result1 = events.poll(20, TimeUnit.SECONDS);
    result2 = events.poll(20, TimeUnit.SECONDS);
    Assertions.assertNotNull(result1, "Should have received first event");
    Assertions.assertNotNull(result2, "Should have received second event");
    Assertions.assertEquals(Set.of(0L, 1L), Set.of(result1.counter, result2.counter));
    listener.stop();
    Assertions.assertEquals(0, events.size()); // No more events processed
  }

  @Test
  public void
      testShouldListenToChangeStreamWithCounterAndListenAgainAndStartAfterPassedResumeTokenBasicTestCase()
          throws InterruptedException {
    // GIVEN
    String databaseName = "test";
    IndexedCollectionChangeStreamListener listener =
        new IndexedCollectionChangeStreamListener(mongoClient, databaseName, POSTS_COLLECTION_NAME);
    BlockingQueue<EventWithCounter> events = new ArrayBlockingQueue<>(4);

    // WHEN
    listener.startListening((event, counter) -> events.add(new EventWithCounter(event, counter)));

    Thread.sleep(2000); // Wait for listener to start

    // Perform operations

    extracted(2);

    // THEN
    EventWithCounter result1 = events.poll(20, TimeUnit.SECONDS);
    Assertions.assertNotNull(result1, "Should have received first event");
    Assertions.assertEquals(OperationType.INSERT, result1.event.getOperationType());

    EventWithCounter result2 = events.poll(20, TimeUnit.SECONDS);
    Assertions.assertNotNull(result2, "Should have received second event");
    Assertions.assertEquals(OperationType.INSERT, result2.event.getOperationType());
    Assertions.assertEquals(Set.of(0L, 1L), Set.of(result1.counter, result2.counter));
    listener.stop();
    Assertions.assertEquals(0, events.size()); // No more events processed

    // Adding new MongoDB documents when listener is not listening
    extracted(2);

    BsonDocument lastResumeToken =
        result1.counter == 1L ? result1.event.getResumeToken() : result2.event.getResumeToken();
    String lastToken = lastResumeToken.toJson();
    // Running listener again to process the last two MongoDB documents

    listener =
        new IndexedCollectionChangeStreamListener(mongoClient, databaseName, POSTS_COLLECTION_NAME);

    // WHEN
    listener.startListening(
        (event, counter) -> events.add(new EventWithCounter(event, counter)), lastToken);

    Thread.sleep(2000); // Wait for listener to start
    result1 = events.poll(20, TimeUnit.SECONDS);
    result2 = events.poll(20, TimeUnit.SECONDS);
    Assertions.assertNotNull(result1, "Should have received first event");
    Assertions.assertNotNull(result2, "Should have received second event");
    Assertions.assertEquals(Set.of(0L, 1L), Set.of(result1.counter, result2.counter));
    listener.stop();
    Assertions.assertEquals(0, events.size()); // No more events processed
  }

  @Test
  public void
      testShouldListenToChangeStreamWithCounterAndListenAgainAndStartAfterPassedResumeTokenWithUsingCASComponent()
          throws InterruptedException {
    // GIVEN
    ResumeTokenCas resumeTokenCas = new ResumeTokenCas();
    String databaseName = "test";
    IndexedCollectionChangeStreamListener listener =
        new IndexedCollectionChangeStreamListener(mongoClient, databaseName, POSTS_COLLECTION_NAME);
    BlockingQueue<EventWithCounter> events = new ArrayBlockingQueue<>(4);

    // WHEN
    listener.startListening(
        (event, counter) -> {
          events.add(new EventWithCounter(event, counter));
          resumeTokenCas.compareAndSet(
              new ResumeTokenInfo(event.getResumeToken().toJson(), counter));
        });

    Thread.sleep(2000); // Wait for listener to start

    // Perform operations

    extracted(2);

    // THEN
    EventWithCounter result1 = events.poll(20, TimeUnit.SECONDS);
    Assertions.assertNotNull(result1, "Should have received first event");
    Assertions.assertEquals(OperationType.INSERT, result1.event.getOperationType());

    EventWithCounter result2 = events.poll(20, TimeUnit.SECONDS);
    Assertions.assertNotNull(result2, "Should have received second event");
    Assertions.assertEquals(OperationType.INSERT, result2.event.getOperationType());
    Assertions.assertEquals(Set.of(0L, 1L), Set.of(result1.counter, result2.counter));
    listener.stop();
    Assertions.assertEquals(0, events.size()); // No more events processed

    // Adding new MongoDB documents when listener is not listening
    extracted(2);

    String lastToken = resumeTokenCas.get().resumeTokenJson();
    // Running listener again to process the last two MongoDB documents

    listener =
        new IndexedCollectionChangeStreamListener(mongoClient, databaseName, POSTS_COLLECTION_NAME);

    // WHEN
    listener.startListening(
        (event, counter) -> {
          events.add(new EventWithCounter(event, counter));
          resumeTokenCas.compareAndSet(
              new ResumeTokenInfo(event.getResumeToken().toJson(), counter));
        },
        lastToken);

    Thread.sleep(2000); // Wait for listener to start
    result1 = events.poll(20, TimeUnit.SECONDS);
    result2 = events.poll(20, TimeUnit.SECONDS);
    Assertions.assertNotNull(result1, "Should have received first event");
    Assertions.assertNotNull(result2, "Should have received second event");
    Assertions.assertEquals(Set.of(0L, 1L), Set.of(result1.counter, result2.counter));
    listener.stop();
    Assertions.assertEquals(0, events.size()); // No more events processed
  }

  private void extracted(int amountOfDocuments) {
    for (int i = 0; i < amountOfDocuments; i++) {
      Post post = new Post();
      post.setText("Test post " + i);
      postDao.save(post);
    }
  }

  private static class EventWithCounter {
    ChangeStreamDocument<Document> event;
    long counter;

    public EventWithCounter(ChangeStreamDocument<Document> event, long counter) {
      this.event = event;
      this.counter = counter;
    }
  }
}
