package com.github.starnowski.mongo.fun.mongodb.container.repositories;

import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;

@ApplicationScoped
public class ExampleDao extends AbstractDao<Document> {

  @Override
  protected String getCollectionName() {
    return "examples";
  }

  @Override
  protected Class<Document> getDocumentClass() {
    return Document.class;
  }
}
