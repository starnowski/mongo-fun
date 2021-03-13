package com.github.starnowski.mongo.fun.repositories;

import com.github.starnowski.mongo.fun.model.Post;
import org.springframework.stereotype.Repository;

@Repository
public class PostDao extends AbstractDao<Post> {

    @Override
    protected String getCollectionName() {
        return "posts";
    }

    @Override
    protected Class<Post> getDocumentClass() {
        return Post.class;
    }
}
