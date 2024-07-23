package com.github.starnowski.mongo.fun.mongodb.container.repositories;

import com.github.starnowski.mongo.fun.mongodb.container.AbstractITTest;
import com.github.starnowski.mongo.fun.mongodb.container.model.Comment;
import com.github.starnowski.mongo.fun.mongodb.container.model.Post;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

class CommentDaoTest extends AbstractITTest {

    @Autowired
    PostDao postDao;

    @Autowired
    CommentDao tested;

    private ObjectId postObjectId;

    @BeforeEach
    public void createPost() {
        Post post = new Post();
        post.setText("Test post");
        post.setEmail("john.doe.2014@gmail.com");
        post = postDao.save(post);
        postObjectId = post.getOid();
    }

    @ParameterizedTest
    @ValueSource(strings = {"comment1", "comment2", "commentFinal"})
    public void shouldSavePost(String commentText) {
        // GIVEN
        Comment comment = new Comment();
        String email = "john2001.doe@gmailcom";
        comment.setEmail(email);
        comment.setText(commentText);
        comment.setPostObjectId(postObjectId);

        // WHEN
        Comment result = tested.save(comment);

        // THEN
        Assertions.assertNotNull(result.getId());
        result = tested.find(result.getOid());
        Assertions.assertNotNull(result.getId());
        Assertions.assertEquals(commentText, result.getText());
        Assertions.assertEquals(email, result.getEmail());
        Assertions.assertEquals(postObjectId, result.getPostObjectId());
    }

}