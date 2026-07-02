package com.github.starnowski.mongo.fun;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import com.github.starnowski.jamolingo.junit5.SpringMongoDataLoaderExtension;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {SearchDemoApplication.class})
@AutoConfigureMockMvc
@ExtendWith(SpringMongoDataLoaderExtension.class)
public class AbstractItTest {

  @Autowired protected MongoClient mongoClient;

  protected void ensureSearchIndexReady(
      String indexName, String indexDef, MongoCollection<Document> collection) {
    try {
      Document indexDefinition = Document.parse(indexDef);
      collection.createSearchIndex(indexName, indexDefinition);

      // Wait for index to be ready
      await()
          .atMost(30, SECONDS)
          .pollInterval(1, SECONDS)
          .until(
              () -> {
                for (Document index : collection.listSearchIndexes()) {
                  if (indexName.equals(index.getString("name"))
                      && "READY".equals(index.getString("status"))) {
                    return true;
                  }
                }
                return false;
              });
    } catch (Exception e) {
      // Index might already exist
    }
  }
}
