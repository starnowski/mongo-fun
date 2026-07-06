package com.github.starnowski.mongo.fun;

import static com.mongodb.ExplainVerbosity.QUERY_PLANNER;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import com.github.starnowski.jamolingo.junit5.SpringMongoDataLoaderExtension;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Assertions;
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

  protected void waitForSearchIndexSync(
      MongoCollection<Document> collection, String indexName, String path)
      throws InterruptedException {
    long collectionCount = collection.countDocuments();
    await()
        .atMost(30, SECONDS)
        .pollInterval(1, SECONDS)
        .until(
            () -> {
              List<Document> results = new ArrayList<>();
              collection
                  .aggregate(
                      List.of(
                          Document.parse(
                              """
                                                {
                                                  "$searchMeta": {
                                                    "index": "%s",
                                                    "exists": {
                                                      "path": "%s"
                                                    },
                                                    "count": {
                                                      "type": "total"
                                                    }
                                                  }
                                                }
                                                """
                                  .formatted(indexName, path))))
                  .into(results);

              if (!results.isEmpty()) {
                Document countDoc = results.get(0).get("count", Document.class);
                return countDoc != null && countDoc.getLong("total") == collectionCount;
              }
              return false;
            });
  }

  protected void runTest(
      String searchQuery,
      Map<String, Integer> expectedIdsWithScoreIndex,
      MongoCollection<Document> collection) {

    List<Bson> pipeline =
        List.of(
            Document.parse(searchQuery),
            Document.parse(
                """
                                          {
                                            "$set": {
                                              "score": { "$meta": "searchScore" }
                                            }
                                          }
                                          """));

    // WHEN
    List<Document> results = new ArrayList<>();
    TestHelper.runAssertion(
        20,
        1,
        () -> {
          results.clear();
          collection.aggregate(pipeline).into(results);
          System.out.println("Results ->" + results + "<-");
          System.out.println("Query explain QUERY_PLANNER ->");
          System.out.println(collection.aggregate(pipeline).explain(QUERY_PLANNER).toJson());
          System.out.println("<-");
          // THEN
          Assertions.assertEquals(
              expectedIdsWithScoreIndex,
              convertResultsToDocumentAndScoreIndex(results),
              "Expected to find documents with expected order for query: " + searchQuery);
        });
  }

  protected Map<String, Integer> convertResultsToDocumentAndScoreIndex(List<Document> results) {
    List<Double> scores =
        results.stream()
            .map(d -> d.getDouble("score"))
            .distinct()
            .sorted(Comparator.reverseOrder())
            .toList();
    return results.stream()
        .collect(
            Collectors.toMap(d -> d.getString("_id"), d -> scores.indexOf(d.getDouble("score"))));
  }
}
