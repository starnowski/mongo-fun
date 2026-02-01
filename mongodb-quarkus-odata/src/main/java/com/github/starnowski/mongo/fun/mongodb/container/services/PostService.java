package com.github.starnowski.mongo.fun.mongodb.container.services;

import static java.util.stream.Collectors.toList;

import com.github.starnowski.mongo.fun.mongodb.container.model.Post;
import com.github.starnowski.mongo.fun.mongodb.container.repositories.CommentDao;
import com.github.starnowski.mongo.fun.mongodb.container.repositories.PostDao;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;

@ApplicationScoped
public class PostService {

  @Inject private PostDao postDao;

  @Inject private CommentDao commentDao;

  public Post save(Post post) {
    Post result = postDao.save(post);
    if (post.getComments() != null) {
      post.setComments(
          post.getComments().stream()
              .map(
                  comment -> {
                    comment.withPostObjectId(result.getOid());
                    return commentDao.save(comment);
                  })
              .collect(toList()));
    }
    return result;
  }

  public Post find(ObjectId oid) {
    return postDao.findAndFetchComments(oid);
  }
}
