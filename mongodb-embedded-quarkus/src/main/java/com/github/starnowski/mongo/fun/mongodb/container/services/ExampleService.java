package com.github.starnowski.mongo.fun.mongodb.container.services;

import com.github.starnowski.mongo.fun.mongodb.container.repositories.ExampleDao;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;

@ApplicationScoped
public class ExampleService {

    @Inject
    private ExampleDao exampleDao;

    public Map<String, Object> saveExample(Map<String, Object> payload) {
        //TODO
        return null;
    }


}
