package com.github.starnowski.mongo.fun.services;

import com.github.starnowski.mongo.fun.model.Post;
import com.github.starnowski.mongo.fun.repositories.PostDao;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PostService {

    @Autowired
    private PostDao postDao;

    public Post save(Post post) {
        post = postDao.save(post);
        return post;
    }

    public Post find(ObjectId oid) {
        return postDao.find(oid);
    }
}
