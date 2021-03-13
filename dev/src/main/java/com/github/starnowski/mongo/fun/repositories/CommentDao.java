package com.github.starnowski.mongo.fun.repositories;

import com.github.starnowski.mongo.fun.model.Comment;
import org.springframework.stereotype.Repository;

@Repository
public class CommentDao extends AbstractDao<Comment> {

    @Override
    protected String getCollectionName() {
        return "comments";
    }

    @Override
    protected Class<Comment> getDocumentClass() {
        return Comment.class;
    }
}
