package com.github.starnowski.mongo.fun.repositories;

import com.github.starnowski.mongo.fun.model.Comment;
import org.springframework.stereotype.Repository;

import static com.github.starnowski.mongo.fun.repositories.DaoProperties.COMMENTS_COLLECTION_NAME;

@Repository
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
