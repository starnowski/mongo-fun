package com.github.starnowski.mongo.fun;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

public class MongoDbContainer extends GenericContainer<MongoDbContainer> {

    public static final int MONGODB_PORT = 27017;
    public static final String DEFAULT_IMAGE_AND_TAG = "mongodb/mongodb-atlas-local:7.0.11";

    public MongoDbContainer() {
        this(DEFAULT_IMAGE_AND_TAG);
    }

    public MongoDbContainer(String image) {
        super(image);
        withReuse(true)
                .withExposedPorts(MONGODB_PORT, 27027)
                .withPrivilegedMode(true)
                .withCreateContainerCmdModifier(
                        cmd -> {
                            cmd.getHostConfig().withMemory(4 * 1024 * 1024 * 1024L);
                            cmd.getHostConfig().withShmSize(2 * 1024 * 1024 * 1024L);
                        })
                .withEnv("MONGOT_LOG_FILE", "/dev/stdout")
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(5)));
    }

    public Integer getPort() {
        return getMappedPort(MONGODB_PORT);
    }
}