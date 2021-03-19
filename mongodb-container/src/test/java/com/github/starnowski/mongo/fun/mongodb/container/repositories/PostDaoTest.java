package com.github.starnowski.mongo.fun.mongodb.container.repositories;

import com.github.starnowski.mongo.fun.mongodb.container.model.Post;
import com.github.starnowski.mongo.fun.mongodb.container.model.PostAuthor;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.WriteModel;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Sorts.descending;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ContextConfiguration
class PostDaoTest {

    @Autowired
    PostDao postDao;

    private static Post postForAuthor(String author) {
        return new Post().withText("test").withEmail(author);
    }

    private static Stream<Arguments> provide_shouldGroupPostAuthorsWithDescendingOrder() {
        return Stream.of(
                Arguments.of(Arrays.asList(postForAuthor("ala@hot.mail"), postForAuthor("szymon132@hot.mail"), postForAuthor("szymon132@hot.mail"), postForAuthor("szymon132@hot.mail"), postForAuthor("ala@hot.mail"), postForAuthor("mike@gmail.com")),
                        Arrays.asList(new PostAuthor("szymon132@hot.mail", 3), new PostAuthor("ala@hot.mail", 2), new PostAuthor("mike@gmail.com", 1))),
                Arguments.of(Arrays.asList(postForAuthor("kylie@hot.mail"), postForAuthor("kylie@hot.mail"), postForAuthor("szymon132@hot.mail"), postForAuthor("ala@hot.mail"), postForAuthor("ala@hot.mail"), postForAuthor("kylie@hot.mail")),
                        Arrays.asList(new PostAuthor("kylie@hot.mail", 3), new PostAuthor("ala@hot.mail", 2), new PostAuthor("szymon132@hot.mail", 1)))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"post1", "post2", "postFinal"})
    public void shouldSavePost(String postText) {
        // GIVEN
        Post post = new Post();
        String email = "john2001.doe@gmailcom";
        post.setEmail(email);
        post.setText(postText);

        // WHEN
        Post result = postDao.save(post);

        // THEN
        result = postDao.find(result.getOid());
        Assertions.assertNotNull(result.getId());
        Assertions.assertEquals(postText, result.getText());
        Assertions.assertEquals(email, result.getEmail());
    }


    @Test
    public void shouldSaveAllRecordsWhenUsingOrderedBulkWriteAndAllInsertsAreSuccessful() {
        // GIVEN
        ObjectId postOid1 = returnNonExistedObjectId();
        ObjectId postOid2 = returnNonExistedObjectId();
        ObjectId postOid3 = returnNonExistedObjectId();
        ObjectId postOid4 = returnNonExistedObjectId();
        ObjectId postOid5 = returnNonExistedObjectId();
        List<WriteModel<Post>> bulkWrites = new ArrayList<>();
        bulkWrites.add(insertOneModelForPostOid(postOid1));
        bulkWrites.add(insertOneModelForPostOid(postOid2));
        bulkWrites.add(insertOneModelForPostOid(postOid3));
        bulkWrites.add(insertOneModelForPostOid(postOid4));
        bulkWrites.add(insertOneModelForPostOid(postOid5));
        BulkWriteOptions bulkWriteOptions = new BulkWriteOptions().ordered(true);

        // WHEN
        BulkWriteResult bulkWriteResult = postDao.getCollection().bulkWrite(bulkWrites, bulkWriteOptions);

        // THEN
        assertEquals(5, bulkWriteResult.getInsertedCount());
        assertNotNull(postDao.find(postOid1));
        assertNotNull(postDao.find(postOid2));
        assertNotNull(postDao.find(postOid3));
        assertNotNull(postDao.find(postOid4));
        assertNotNull(postDao.find(postOid5));
    }

    @Test
    public void shouldSaveAllRecordsBeforeFailedWriteWhenUsingOrderedBulkWrite() {
        // GIVEN
        // Create one post that will cause bulk write failure
        Post post = postDao.save(new Post().withText("post XXX 1"));
        ObjectId nonUniqueId = post.getOid();
        ObjectId postOid1 = returnNonExistedObjectId();
        ObjectId postOid2 = returnNonExistedObjectId();
        ObjectId postOid3 = nonUniqueId;
        ObjectId postOid4 = returnNonExistedObjectId();
        ObjectId postOid5 = returnNonExistedObjectId();
        List<WriteModel<Post>> bulkWrites = new ArrayList<>();
        bulkWrites.add(insertOneModelForPostOid(postOid1));
        bulkWrites.add(insertOneModelForPostOid(postOid2));
        bulkWrites.add(insertOneModelForPostOid(postOid3)); // Write should failed because non-unique key
        bulkWrites.add(insertOneModelForPostOid(postOid4));
        bulkWrites.add(insertOneModelForPostOid(postOid5));
        BulkWriteOptions bulkWriteOptions = new BulkWriteOptions().ordered(true);

        // WHEN
        BulkWriteResult bulkWriteResult = null;
        try {
            postDao.getCollection().bulkWrite(bulkWrites, bulkWriteOptions);
            Assertions.fail("expected an exception of type MongoBulkWriteException");
        } catch (MongoBulkWriteException ex) {
            bulkWriteResult = ex.getWriteResult();
            bulkWriteResult.getInserts();
        }

        // THEN
        assertEquals(2, bulkWriteResult.getInsertedCount());
        assertNotNull(postDao.find(postOid1));
        assertNotNull(postDao.find(postOid2));
        assertNull(postDao.find(postOid4));
        assertNull(postDao.find(postOid5));
    }

    @Test
    public void shouldSaveAllRecordsExceptThoseWhichFailedWhenUsingUnorderedBulkWrite() {
        // GIVEN
        // Create one post that will cause bulk write failure
        Post post = postDao.save(new Post().withText("post XXX 1"));
        ObjectId nonUniqueId = post.getOid();
        ObjectId postOid1 = returnNonExistedObjectId();
        ObjectId postOid2 = nonUniqueId;
        ObjectId postOid3 = returnNonExistedObjectId();
        ObjectId postOid4 = nonUniqueId;
        ObjectId postOid5 = returnNonExistedObjectId();
        List<WriteModel<Post>> bulkWrites = new ArrayList<>();
        bulkWrites.add(insertOneModelForPostOid(postOid1));
        bulkWrites.add(insertOneModelForPostOid(postOid2)); // Write should failed because non-unique key
        bulkWrites.add(insertOneModelForPostOid(postOid3));
        bulkWrites.add(insertOneModelForPostOid(postOid4)); // Write should failed because non-unique key
        bulkWrites.add(insertOneModelForPostOid(postOid5));
        BulkWriteOptions bulkWriteOptions = new BulkWriteOptions().ordered(false);

        // WHEN
        BulkWriteResult bulkWriteResult = null;
        try {
            postDao.getCollection().bulkWrite(bulkWrites, bulkWriteOptions);
            Assertions.fail("expected an exception of type MongoBulkWriteException");
        } catch (MongoBulkWriteException ex) {
            bulkWriteResult = ex.getWriteResult();
            bulkWriteResult.getInserts();
        }

        // THEN
        assertEquals(3, bulkWriteResult.getInsertedCount());
        assertNotNull(postDao.find(postOid1));
        assertNotNull(postDao.find(postOid3));
        assertNotNull(postDao.find(postOid5));
    }

    @ParameterizedTest
    @MethodSource("provide_shouldGroupPostAuthorsWithDescendingOrder")
    public void shouldGroupPostAuthorsWithDescendingOrder(List<Post> posts, List<PostAuthor> expectedMostActivePostAuthors) {
        // GIVEN
        posts.forEach(post ->
        {
            postDao.save(post);
        });
        List<PostAuthor> mostActive = new ArrayList<>();
        List<Bson> pipeLine = new ArrayList<>();
        pipeLine.add(group("$email", Accumulators.sum("count", 1L)));
        pipeLine.add(sort(descending("count")));
        pipeLine.add(limit(20));

        // WHEN
        AggregateIterable<PostAuthor> aggregate = postDao.getCollection()
//                .withReadConcern(ReadConcern.MAJORITY) // Not crucial for tests case
                .aggregate(pipeLine, PostAuthor.class);
        List<PostAuthor> results = new ArrayList<>();
        aggregate.forEach(new Consumer<PostAuthor>() {
                              @Override
                              public void accept(PostAuthor critic) {
                                  results.add(critic);
                              }
                          }
        );

        // THEN
        Assertions.assertIterableEquals(expectedMostActivePostAuthors, results);
    }

    private InsertOneModel<Post> insertOneModelForPostOid(ObjectId objectId) {
        return new InsertOneModel(new Post().withOid(objectId).withText("Test"));
    }

    private ObjectId returnNonExistedObjectId() {
        ObjectId oid = ObjectId.get();
        while (postDao.find(oid) != null) {
            oid = ObjectId.get();
        }
        return oid;
    }
}