package com.github.starnowski.mongo.fun.mongodb.container.configurations;

import com.github.starnowski.mongo.fun.mongodb.container.MongoDbContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class MongoDbContainerConfiguration {

    @Autowired
    private ConfigurableApplicationContext configurableApplicationContext;

    @Bean
    public MongoDbContainer getMongoDbContainer()
    {
        MongoDbContainer mongoDbContainer = new MongoDbContainer();
        mongoDbContainer.start();
        return mongoDbContainer;
    }

    @PostConstruct
    public void mongoDbInitializer()
    {
        MongoDbContainer mongoDbContainer = getMongoDbContainer();
        TestPropertyValues values = TestPropertyValues.of(
                "spring.data.mongodb.host=" + mongoDbContainer.getContainerIpAddress(),
                "spring.data.mongodb.port=" + mongoDbContainer.getPort()

        );
        values.applyTo(configurableApplicationContext);
    }
}
