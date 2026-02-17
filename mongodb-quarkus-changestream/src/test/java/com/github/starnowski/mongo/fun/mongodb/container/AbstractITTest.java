package com.github.starnowski.mongo.fun.mongodb.container;

import com.github.starnowski.mongo.fun.mongodb.container.repositories.CommentDao;
import com.github.starnowski.mongo.fun.mongodb.container.repositories.PostDao;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;

@QuarkusTest
@QuarkusTestResource(EmbeddedMongoResource.class)
public class AbstractITTest {

  @Inject protected PostDao postDao;

  @Inject protected CommentDao commentDao;

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
