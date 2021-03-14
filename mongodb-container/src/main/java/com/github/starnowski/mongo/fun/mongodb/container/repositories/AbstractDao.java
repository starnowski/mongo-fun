package com.github.starnowski.mongo.fun.mongodb.container.repositories;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import javax.annotation.PostConstruct;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public abstract class AbstractDao<T> {

    @Autowired
    protected MongoTemplate mongoTemplate;
    protected MongoCollection<T> collection;

    @PostConstruct
    public void init() {
        CodecRegistry pojoCodecRegistry =
                fromRegistries(
                        MongoClientSettings.getDefaultCodecRegistry(),
                        fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        this.collection =
                mongoTemplate.getDb().getCollection(getCollectionName(), getDocumentClass()).withCodecRegistry(pojoCodecRegistry);
    }

    public T save(T document) {
        collection.insertOne(document);
        return document;
    }

    public T find(String id) {

        return collection.find(new Document(getIdPropertyName(), new ObjectId(id))).first();
    }

    public T find(ObjectId oid) {

        return collection.find(new Document(getIdPropertyName(), oid)).first();
    }

    protected String getIdPropertyName() {
        return "_id";
    }

    abstract protected String getCollectionName();

    abstract protected Class<T> getDocumentClass();
}
