package com.github.starnowski.mongo.fun.mongodb.container.repositories;

import static com.github.starnowski.mongo.fun.mongodb.container.repositories.DaoProperties.COMMENTS_POSTS_ID_COLLUMN_NAME;
import static com.github.starnowski.mongo.fun.mongodb.container.repositories.DaoProperties.POSTS_COLLECTION_NAME;

import com.github.starnowski.mongo.fun.mongodb.container.model.Post;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.ClientSession;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

@ApplicationScoped
public class PostDao extends AbstractDao<Post> {

  @Inject CommentDao commentDao;

  @Override
  protected String getCollectionName() {
    return POSTS_COLLECTION_NAME;
  }

  @Override
  protected Class<Post> getDocumentClass() {
    return Post.class;
  }

  public Post findAndFetchComments(ObjectId oid) {

    List<Bson> pipeline = new ArrayList<Bson>();
    pipeline.add(Aggregates.match(Filters.eq("_id", oid)));
    pipeline.add(
        Aggregates.lookup(
            DaoProperties.COMMENTS_COLLECTION_NAME,
            "_id",
            COMMENTS_POSTS_ID_COLLUMN_NAME,
            "comments"));

    AggregateIterable<Post> results = collection.aggregate(pipeline);
    return results.first();
  }

  public Post save(Post post) {
    post = super.save(post);
    UpdateOptions options = new UpdateOptions();
    collection.updateOne(Filters.eq("_id", post.getOid()), Updates.unset("comments"), options);
    return find(post.getOid());
  }

  public Post saveWithComments(Post post) {
    if (post.getOid() == null) {
      post.setOid(new ObjectId());
    }
    try (ClientSession session = mongoClient.startSession()) {
      return session.withTransaction(
          () -> {
            collection.insertOne(session, post);
            if (post.getComments() != null && !post.getComments().isEmpty()) {
              post.getComments()
                  .forEach(
                      comment -> {
                        comment.setPostObjectId(post.getOid());
                        commentDao.getCollection().insertOne(session, comment);
                      });
            }
            return post;
          });
    }
  }
}
