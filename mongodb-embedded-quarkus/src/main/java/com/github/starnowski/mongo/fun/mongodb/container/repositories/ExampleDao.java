package com.github.starnowski.mongo.fun.mongodb.container.repositories;


public class ExampleDao extends AbstractDao<Object> {

    @Override
    protected String getCollectionName() {
        return "examples";
    }

    @Override
    protected Class<Object> getDocumentClass() {
        return Object.class;
    }
}