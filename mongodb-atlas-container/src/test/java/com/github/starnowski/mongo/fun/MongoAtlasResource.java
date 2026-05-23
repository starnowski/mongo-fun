package com.github.starnowski.mongo.fun;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.wait.strategy.Wait;

@Configuration
public class MongoAtlasResource {

  @Autowired private ConfigurableApplicationContext configurableApplicationContext;

  @Bean
  public MongoDbContainer getMongoDbContainer() {
    MongoDbContainer mongoAtlasContainer = new MongoDbContainer();

    mongoAtlasContainer.start();
    Wait.forLogMessage(".*Starting TCP server.*", 2)
        .withStartupTimeout(Duration.ofSeconds(30))
        .waitUntilReady(mongoAtlasContainer);
    Wait.forLogMessage(".*Starting message server.*", 2)
        .withStartupTimeout(Duration.ofSeconds(30))
        .waitUntilReady(mongoAtlasContainer);
    TestPropertyValues values =
        TestPropertyValues.of(
            "spring.data.mongodb.uri="
                + String.format(
                    "mongodb://%s:%d/?directConnection=true",
                    mongoAtlasContainer.getHost(), mongoAtlasContainer.getMappedPort(27017)));

    values.applyTo(configurableApplicationContext);
    return mongoAtlasContainer;
  }
}
