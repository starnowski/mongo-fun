package com.github.starnowski.mongo.fun.mongodb.container.test;

public @interface MongoDocument {
    String collection();

    String bsonFilePath();
}
