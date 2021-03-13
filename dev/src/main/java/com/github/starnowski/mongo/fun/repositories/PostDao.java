package com.github.starnowski.mongo.fun.repositories;

import com.github.starnowski.mongo.fun.model.Post;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@Repository
public class PostDao {

    @Autowired
    private MongoTemplate mongoTemplate;
    private MongoCollection<Post> collection;

    @PostConstruct
    public void init() {
        CodecRegistry pojoCodecRegistry =
                fromRegistries(
                        MongoClientSettings.getDefaultCodecRegistry(),
                        fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        this.collection =
                mongoTemplate.getDb().getCollection("posts", Post.class).withCodecRegistry(pojoCodecRegistry);
    }

    public Post save(Post post) {
        collection.insertOne(post);
        return post;
    }

    public Post find(String id) {

        return collection.find(new Document("_id", new ObjectId(id))).first();
    }

    public Post find(ObjectId oid) {

        return collection.find(new Document("_id", oid)).first();
    }
}
