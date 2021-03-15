package com.github.starnowski.mongo.fun.repositories;

import com.github.starnowski.mongo.fun.model.Post;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.WriteModel;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PostDaoTest {

    @Autowired
    PostDao postDao;

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
        assertNotNull(result.getId());
        Assertions.assertEquals(postText, result.getText());
        Assertions.assertEquals(email, result.getEmail());
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