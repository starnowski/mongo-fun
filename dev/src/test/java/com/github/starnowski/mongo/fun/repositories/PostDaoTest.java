package com.github.starnowski.mongo.fun.repositories;

import com.github.starnowski.mongo.fun.model.Post;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

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
        Assertions.assertNotNull(result.getId());
        Assertions.assertEquals(postText, result.getText());
        Assertions.assertEquals(email, result.getEmail());
    }
}