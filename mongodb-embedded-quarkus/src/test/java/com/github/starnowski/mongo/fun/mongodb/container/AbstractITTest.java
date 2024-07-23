package com.github.starnowski.mongo.fun.mongodb.container;

import com.github.starnowski.mongo.fun.mongodb.container.repositories.CommentDao;
import com.github.starnowski.mongo.fun.mongodb.container.repositories.PostDao;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration
public class AbstractITTest {

    @Autowired
    protected PostDao postDao;

    @Autowired
    protected CommentDao commentDao;

    @AfterEach
    public void deletePostsAfterTests() {
        // Delete posts
        postDao.deleteAll();
    }

    @AfterEach
    public void deleteCommentsAfterTests() {
        // Delete posts
        commentDao.deleteAll();
    }
}
