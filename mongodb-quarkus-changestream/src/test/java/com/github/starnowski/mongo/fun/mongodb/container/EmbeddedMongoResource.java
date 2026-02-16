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
import org.awaitility.Awaitility;
import org.bson.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class EmbeddedMongoResource
        implements QuarkusTestResourceLifecycleManager {

    private MongodExecutable mongodExecutable;
    private MongodProcess mongodProcess;

    @Override
    public Map<String, String> start() {
        try {
            int port = 27018;
            String rsName = "rs0";
            String uri = "mongodb://localhost:" + port;

            MongodStarter starter = MongodStarter.getDefaultInstance();

            MongodConfig mongodConfig = MongodConfig.builder()
                    .version(Version.Main.V6_0)
                    .net(new de.flapdoodle.embed.mongo.config.Net(
                            port, Network.localhostIsIPv6()))
                    .replication(new Storage(null, rsName, 0)) // --replSet
                    .cmdOptions(MongoCmdOptions.builder()
                            .useNoJournal(false)
                            .build())
                    .build();

            mongodExecutable = starter.prepare(mongodConfig);
            mongodProcess = mongodExecutable.start();

            try (MongoClient client = MongoClients.create(uri)) {

                // 1️⃣ Initiate replica set
                client.getDatabase("admin").runCommand(
                        new Document("replSetInitiate",
                                new Document("_id", rsName)
                                        .append("members", List.of(
                                                new Document("_id", 0)
                                                        .append("host", "localhost:" + port)
                                        )))
                );

                // 2️⃣ Wait for PRIMARY
                Awaitility.await()
                        .atMost(30, TimeUnit.SECONDS)
                        .until(() -> {
                            Document status = client
                                    .getDatabase("admin")
                                    .runCommand(new Document("replSetGetStatus", 1));

                            return status.getList("members", Document.class)
                                    .stream()
                                    .anyMatch(m ->
                                            "PRIMARY".equals(m.getString("stateStr")));
                        });

                // 3️⃣ Extra safety: wait for sessions to work
                Awaitility.await()
                        .atMost(10, TimeUnit.SECONDS)
                        .until(() -> {
                            client.startSession().close();
                            return true;
                        });
            }

            Map<String, String> config = new HashMap<>();
            config.put("quarkus.mongodb.connection-string", uri + "/?replicaSet=" + rsName);
            return config;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        if (mongodProcess != null) mongodProcess.stop();
        if (mongodExecutable != null) mongodExecutable.stop();
    }
}

