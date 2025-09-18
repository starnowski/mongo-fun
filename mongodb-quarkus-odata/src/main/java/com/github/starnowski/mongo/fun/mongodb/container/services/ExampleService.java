package com.github.starnowski.mongo.fun.mongodb.container.services;

import com.github.starnowski.mongo.fun.mongodb.container.repositories.ExampleDao;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.core.uri.parser.UriParserException;
import org.apache.olingo.server.core.uri.validator.UriValidationException;
import org.bson.Document;

import java.util.*;

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


    public List<Map<String, Object>> query(String filter) throws UriValidationException, UriParserException, ExpressionVisitException, ODataApplicationException {
        return exampleDao.query(filter).stream().map(doc -> (Map<String, Object>) new HashMap(doc)).toList();
    }

    public String explain(String filter) throws UriValidationException, UriParserException, ExpressionVisitException, ODataApplicationException {
        return exampleDao.explain(filter);
    }
}
