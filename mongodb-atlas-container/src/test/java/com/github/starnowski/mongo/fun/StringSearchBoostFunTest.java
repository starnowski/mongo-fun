package com.github.starnowski.mongo.fun;

import com.github.starnowski.jamolingo.junit5.MongoDocument;
import com.github.starnowski.jamolingo.junit5.MongoSetup;
import com.github.starnowski.jamolingo.junit5.SpringMongoDataLoaderExtension;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = {SearchDemoApplication.class})
@AutoConfigureMockMvc
@ExtendWith(SpringMongoDataLoaderExtension.class)
public class StringSearchBoostFunTest {

  @Autowired protected MongoClient mongoClient;

  private static final String DATABASE_NAME = "testdb_should";
  private static final String COLLECTION_NAME = "products";
  private static final String INDEX_NAME = "string_search_boots";

    private static java.util.stream.Stream<Arguments> provideSearchTests() {
        return java.util.stream.Stream.of(
                Arguments.of(
                        """
            {
              "$search": {
                "index": "%s",
                "compound": {
                  "should": [
                    {"text":{ "query":"F16XXX0001FIGHTER", "path":"title" }}
                  ]
                }
              }
            }
            """),
                Arguments.of(
                        """
            {
              "$search": {
                "index": "%s",
                "compound": {
                  "should": [
                    {"phrase":{ "query":"F16XXX0001FIGHTER", "path":"title" }}
                  ]
                }
              }
            }
            """),
                Arguments.of(
                        """
            {
              "$search": {
                "index": "%s",
                "compound": {
                  "should": [
                    {"autocomplete":{ "query":"F16", "path":"title" }}
                  ]
                }
              }
            }
            """)
        );
    }

    @ParameterizedTest
    @MethodSource("provideSearchTests")
  @MongoSetup(
      mongoDocuments = {
        @MongoDocument(
            database = DATABASE_NAME,
            collection = COLLECTION_NAME,
            bsonFilePath = "bson/search/string_search_1.json")
      })
  public void shouldReturnDocumentBasedOnQuery(String query) throws InterruptedException {
    // GIVEN
    MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
    MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
    ensureSearchIndex(collection);
    waitForSearchIndexSync(collection, INDEX_NAME);

    List<Bson> pipeline =
        List.of(
            Document.parse(
                    query
                    .formatted(INDEX_NAME)),
            Document.parse(
                """
            {
              "$project": {
                "_id": 0,
                "score": { "$meta": "searchScore" },
                "title": "$title",
                "plot": "$plot"
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
          // THEN
          // Should return only 1 document: "F16XXX0001FIGHTER"
          Assertions.assertEquals(
              1, results.size(), "Expected exactly 1 document, but found " + results.size());
          Assertions.assertEquals("F16XXX0001FIGHTER", results.get(0).getString("title"));
          Assertions.assertTrue(results.get(0).getDouble("score") > 0);
        });
  }

  private void waitForSearchIndexSync(MongoCollection<Document> collection, String indexName)
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
                        "path": "title"
                      },
                      "count": {
                        "type": "total"
                      }
                    }
                  }
                  """
                                  .formatted(indexName))))
                  .into(results);

              if (!results.isEmpty()) {
                Document countDoc = results.get(0).get("count", Document.class);
                return countDoc != null && countDoc.getLong("total") == collectionCount;
              }
              return false;
            });
  }

  private void ensureSearchIndex(MongoCollection<Document> collection) {
    try {
      Document indexDefinition =
          Document.parse(
              """
                      {
                        "mappings": {
                          "dynamic": false,
                          "fields": {
                            "title": [
                              {
                                "type": "string",
                                "analyzer": "lucene.standard"
                              },
                              {
                                "type": "autocomplete",
                                "minGrams": 2,
                                "maxGrams": 20,
                                "tokenization": "edgeGram"
                              }
                            ]
                          }
                        }
                      }
          """);
      collection.createSearchIndex(INDEX_NAME, indexDefinition);

      // Wait for index to be ready
      await()
          .atMost(30, SECONDS)
          .pollInterval(1, SECONDS)
          .until(
              () -> {
                for (Document index : collection.listSearchIndexes()) {
                  if (INDEX_NAME.equals(index.getString("name"))
                      && "READY".equals(index.getString("status"))) {
                    return true;
                  }
                }
                return false;
              });
    } catch (Exception e) {
      // Index might already exist
        e.printStackTrace();
    }
  }
}
