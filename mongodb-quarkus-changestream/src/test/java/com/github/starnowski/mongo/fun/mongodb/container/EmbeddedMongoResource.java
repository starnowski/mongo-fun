package com.github.starnowski.mongo.fun.mongodb.container;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongoCmdOptions;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Storage;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.bson.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmbeddedMongoResource implements QuarkusTestResourceLifecycleManager {

    private MongodExecutable mongodExecutable;
    private MongodProcess mongodProcess;

    @Override
    public Map<String, String> start() {
        try {
            MongodStarter starter = MongodStarter.getDefaultInstance();
            int port = 27018; // You can choose any available port
            MongodConfig mongodConfig = MongodConfig.builder()
                    .version(Version.Main.V6_0)
                    .net(new de.flapdoodle.embed.mongo.config.Net(port, Network.localhostIsIPv6()))
                    .replication(new Storage(null, "rs0", 0))
                    .cmdOptions(MongoCmdOptions.builder().useNoJournal(false).build())
                    .build();
            mongodExecutable = starter.prepare(mongodConfig);
            mongodProcess = mongodExecutable.start();

            String connectionString = "mongodb://localhost:" + port;
            System.out.println("Initializing replica set...");
            try (MongoClient mongoClient = MongoClients.create(connectionString)) {
                mongoClient.getDatabase("admin").runCommand(new Document("replSetInitiate",
                        new Document("_id", "rs0")
                                .append("members", List.of(new Document("_id", 0).append("host", "localhost:" + port)))));
                System.out.println("Replica set initialized.");
            } catch (Exception e) {
                System.out.println("Replica set initialization failed or already initialized: " + e.getMessage());
            }

            Map<String, String> config = new HashMap<>();
//            config.put("quarkus.mongodb.connection-string", connectionString + "/?replicaSet=rs0");
            config.put("quarkus.mongodb.connection-string", connectionString);
            return config;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        if (mongodProcess != null) {
            mongodProcess.stop();
        }
        if (mongodExecutable != null) {
            mongodExecutable.stop();
        }
    }
}
