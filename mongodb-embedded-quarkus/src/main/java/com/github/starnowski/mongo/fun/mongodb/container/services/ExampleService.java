package com.github.starnowski.mongo.fun.mongodb.container.services;

import com.github.starnowski.mongo.fun.mongodb.container.repositories.ExampleDao;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;

import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class ExampleService {

    @Inject
    private ExampleDao exampleDao;

    public Map<String, Object> getById(UUID id) {
        return exampleDao.findByUIID(id);
    }

    public Map<String, Object> saveExample(Map<String, Object> payload) {
        return exampleDao.save(new Document(payload));
    }

    public Map<String, Object> saveAndUpdate(Map<String, Object> payload, Map<String, Object> params) {
        return exampleDao.saveAndUpdate(null, new Document(payload), params);
    }

    public Map<String, Object> saveAndUpdate(UUID id, Map<String, Object> payload, Map<String, Object> params) {
        return exampleDao.saveAndUpdate(id, new Document(payload), params);
    }

    public Map<String, Object> update(UUID id, Map<String, Object> payload, Map<String, Object> params) {
        return exampleDao.update(id, new Document(payload), params);
    }


}
