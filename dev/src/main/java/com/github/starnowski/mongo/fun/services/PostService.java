package com.github.starnowski.mongo.fun.services;

import com.github.starnowski.mongo.fun.model.Post;
import com.github.starnowski.mongo.fun.repositories.CommentDao;
import com.github.starnowski.mongo.fun.repositories.PostDao;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static java.util.stream.Collectors.toList;

@Service
public class PostService {

    @Autowired
    private PostDao postDao;

    @Autowired
    private CommentDao commentDao;

    public Post save(Post post) {
        Post result = postDao.save(post);
        if (post.getComments() != null) {
            post.setComments(post.getComments().stream()
                    .map(comment ->
                    {
                        comment.withPostObjectId(result.getOid());
                        return commentDao.save(comment);
                    }).collect(toList()));
        }
        return result;
    }

    public Post find(ObjectId oid) {
        return postDao.findAndFetchComments(oid);
    }
}
