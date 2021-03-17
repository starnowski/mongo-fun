package com.github.starnowski.mongo.fun;

import com.github.starnowski.mongo.fun.repositories.PostDao;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AbstractITTest {

    @Autowired
    protected PostDao postDao;

    @AfterEach
    public void deletePostsAfterTests()
    {
        // Delete posts
        postDao.deleteAll();
    }
}
