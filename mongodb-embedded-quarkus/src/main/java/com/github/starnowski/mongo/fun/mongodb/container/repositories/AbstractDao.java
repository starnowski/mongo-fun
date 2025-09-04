package com.github.starnowski.mongo.fun.mongodb.container.repositories;

import com.github.starnowski.mongo.fun.mongodb.container.codec.OffsetDateTimeCodec;
import com.mongodb.MongoClientSettings;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
                        CodecRegistries.fromCodecs(new OffsetDateTimeCodec()),
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

    public T saveAndUpdate(T document, Map<String, Object> params) {
//        try (ClientSession session = mongoClient.startSession()) {
//            session.startTransaction();
//
//            try {
//
//                InsertOneResult insertedDoc = collection.insertOne(session, document);
//
//                // Step 2: UpdateMany with aggregation pipeline
//                collection.updateMany(
//                        session,
//                        Filters.eq("_id", insertedDoc.getInsertedId()),
//                        List.of(
//                                new Document("$set", new Document("queryParams", params))
//                        )
//                );
//
//                // Step 3: Commit
//                session.commitTransaction();
//                System.out.println("Transaction committed successfully.");
//                return collection.find(Filters.eq("_id", insertedDoc.getInsertedId())).first();
//            } catch (Exception e) {
//                session.abortTransaction();
//                e.printStackTrace();
//                throw new RuntimeException(e);
//            }
//        }
        UUID id = UUID.randomUUID();
        collection.updateMany(
                Filters.eq("_id", id),
                List.of(
                        new Document("$set", document),
                        new Document("$set", new Document("queryParams", params))
                ),
                new UpdateOptions().upsert(true)
        );
        return collection.find(Filters.eq("_id", id)).first();
    }
}
