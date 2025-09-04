package com.github.starnowski.mongo.fun.mongodb.container.services;

import com.github.starnowski.mongo.fun.mongodb.container.repositories.ExampleDao;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;

import java.util.Map;

@ApplicationScoped
public class ExampleService {

    @Inject
    private ExampleDao exampleDao;

    public Map<String, Object> saveExample(Map<String, Object> payload) {
        return exampleDao.save(new Document(payload));
    }

    public Map<String, Object> saveAndUpdate(Map<String, Object> payload, Map<String, Object> params) {
        return exampleDao.saveAndUpdate(new Document(payload), params);
    }


}
