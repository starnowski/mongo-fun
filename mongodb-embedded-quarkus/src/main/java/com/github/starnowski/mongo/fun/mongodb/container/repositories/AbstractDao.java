package com.github.starnowski.mongo.fun.mongodb.container.repositories;

import com.mongodb.MongoClientSettings;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.types.ObjectId;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public abstract class AbstractDao<T> {

    @Inject
    protected MongoClient mongoClient;
    protected MongoCollection<T> collection;

    @PostConstruct
    public void init() {
        CodecRegistry pojoCodecRegistry =
                fromRegistries(
                        MongoClientSettings.getDefaultCodecRegistry(),
                        fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        this.collection =
                mongoClient.getDatabase("test").getCollection(getCollectionName(), getDocumentClass()).withCodecRegistry(pojoCodecRegistry).withWriteConcern(WriteConcern.W1);
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

    public MongoCollection<T> getCollection() {
        return collection;
    }

    public boolean deleteAll() {
        DeleteResult result = collection.deleteMany(Filters.exists(getIdPropertyName()));
        return result.getDeletedCount() > 0;
    }
}
