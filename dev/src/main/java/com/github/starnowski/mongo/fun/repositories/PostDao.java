package com.github.starnowski.mongo.fun.repositories;

import com.github.starnowski.mongo.fun.model.Post;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Repository;

@Repository
public class PostDao {
    public Post save(Post post) {
        return null;
    }

    public Post find(ObjectId oid) {
        return null;
    }
}
