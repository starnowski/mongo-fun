package com.github.starnowski.mongo.fun.mongodb.container.repositories;

import static com.github.starnowski.mongo.fun.mongodb.container.repositories.DaoProperties.COMMENTS_COLLECTION_NAME;

import com.github.starnowski.mongo.fun.mongodb.container.model.Comment;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CommentDao extends AbstractDao<Comment> {

  @Override
  protected String getCollectionName() {
    return COMMENTS_COLLECTION_NAME;
  }

  @Override
  protected Class<Comment> getDocumentClass() {
    return Comment.class;
  }
}
