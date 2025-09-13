package com.github.starnowski.mongo.fun.mongodb.container.test;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(MongoDatabaseSetupExtension.class)
public @interface MongoSetup {

    MongoDocument[] mongoDocuments();
}
