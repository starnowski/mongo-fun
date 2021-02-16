package com.github.starnowski.mongo.fun.mongodb.container;

import com.github.starnowski.mongo.fun.mongodb.container.configurations.MongoDbContainerConfiguration;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.ListDatabasesIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static java.lang.String.format;

@SpringBootTest
@ContextConfiguration
public class MongoClientTest {

    @Autowired
    private MongoDbContainer mongoDbContainer;

    @Test
    public void testShouldListDatabasesAndReturnAtLeastOneDatabase()
    {
        // given
        String host = mongoDbContainer.getContainerIpAddress();
        Integer port = mongoDbContainer.getPort();
        String uri = format("mongodb://%1$s:%2$s@%3$s:%4$s", MongoDbContainerConfiguration.MONGO_ADMIN_NAME, MongoDbContainerConfiguration.MONGO_ADMIN_PASSWORD, host, port);
        ConnectionString connectionString = new ConnectionString(uri);
        MongoClientSettings settings = MongoClientSettings.builder().applyConnectionString(connectionString).build();
        MongoClient mongoClient = MongoClients.create(settings);

        // when
        ListDatabasesIterable<Document> databases = mongoClient.listDatabases();

        // then
        Assertions.assertNotNull(databases.first());
    }
}
