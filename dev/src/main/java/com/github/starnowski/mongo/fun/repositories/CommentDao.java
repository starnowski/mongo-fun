package com.github.starnowski.mongo.fun.repositories;

import com.github.starnowski.mongo.fun.model.Comment;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Repository;

@Repository
public class CommentDao {
    public Comment save(Comment comment) {
        return null;
    }

    public Comment find(ObjectId oid) {
        return null;
    }
}
