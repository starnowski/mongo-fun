package com.github.starnowski.mongo.fun.services;

import com.github.starnowski.mongo.fun.model.Post;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PostServiceTest {

    @Autowired
    PostService tested;

    @Test
    public void shouldCreatePost()
    {
        // GIVEN
        String email = "szymon.doe@gmail.com";
        String text = "szymon.doe@gmail.com";
        Post post = new Post();
        post.setEmail(email);
        post.setText(text);

        // WHEN
        Post result = tested.save(post);

        // THEN
        Assertions.assertNotNull(result.getId());
        result = tested.find(result.getOid());
        Assertions.assertEquals(text, result.getText());
        Assertions.assertEquals(email, result.getEmail());
        Assertions.assertTrue(result.getComments() == null);
    }
}