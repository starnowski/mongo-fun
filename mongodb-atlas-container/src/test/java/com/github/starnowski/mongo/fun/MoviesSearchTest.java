package com.github.starnowski.mongo.fun;

import com.github.starnowski.jamolingo.junit5.MongoDocument;
import com.github.starnowski.jamolingo.junit5.MongoSetup;
import com.github.starnowski.jamolingo.junit5.SpringMongoDataLoaderExtension;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {SearchDemoApplication.class})
@AutoConfigureMockMvc
@ExtendWith(SpringMongoDataLoaderExtension.class)
public class MoviesSearchTest {

  @Autowired protected MongoClient mongoClient;

  @ParameterizedTest
  @CsvSource({
    "The Lion King, The Lion King, 1994",
    "Godfather, The Godfather, 1972",
    "Pulp Fiction, Pulp Fiction, 1994"
  })
  @MongoSetup(
      mongoDocuments = {
        @MongoDocument(
            database = "testdb",
            collection = "movies",
            bsonFilePath = "bson/search/movie1.json"),
        @MongoDocument(
            database = "testdb",
            collection = "movies",
            bsonFilePath = "bson/search/movie2.json"),
        @MongoDocument(
            database = "testdb",
            collection = "movies",
            bsonFilePath = "bson/search/movie3.json")
      })
  public void shouldReturnMovieByTitle(String searchQuery, String expectedTitle, int expectedYear)
      throws InterruptedException {
    // GIVEN
    MongoDatabase database = mongoClient.getDatabase("testdb");
    MongoCollection<Document> collection = database.getCollection("movies");
    ensureSearchIndex(collection);
    waitForSearchIndexSync(collection, "plot_title_idx");

    List<Bson> pipeline =
        List.of(
            Document.parse(
                """
            {
              "$search": {
                "index": "plot_title_idx",
                "text": {
                  "query": "%s",
                  "path": "title"
                }
              }
            }
            """
                    .formatted(searchQuery)),
            Document.parse(
                """
            {
              "$project": {
                "title": 1,
                "year": 1,
                "plot": 1
              }
            }
            """),
            Document.parse(
                """
            {
              "$limit": 1
            }
            """));

    // WHEN
    List<Document> results = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      results.clear();
      collection.aggregate(pipeline).into(results);
      if (!results.isEmpty()) {
        break;
      }
      Thread.sleep(1000);
    }

    // THEN
    Assertions.assertFalse(results.isEmpty(), "Expected to find " + expectedTitle);
    Document movie = results.get(0);
    Assertions.assertEquals(expectedTitle, movie.getString("title"));
    Assertions.assertEquals(expectedYear, movie.getInteger("year"));
  }

  private void waitForSearchIndexSync(MongoCollection<Document> collection, String indexName)
      throws InterruptedException {
    long collectionCount = collection.countDocuments();
    while (true) {
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
        if (countDoc != null && countDoc.getLong("total") == collectionCount) {
          break;
        }
      }
      Thread.sleep(1000);
    }
  }

  private void ensureSearchIndex(MongoCollection<Document> collection) {
    String indexName = "plot_title_idx";
    try {
      Document indexDefinition =
          Document.parse(
              """
          {
            "mappings": {
              "dynamic": false,
              "fields": {
                "plot": { "type": "string" },
                "title": { "type": "string" }
              }
            }
          }
          """);
      collection.createSearchIndex(indexName, indexDefinition);
      // Wait for index to be ready
      while (true) {
        boolean ready = false;
        for (Document index : collection.listSearchIndexes()) {
          if (indexName.equals(index.getString("name"))
              && "READY".equals(index.getString("status"))) {
            ready = true;
            break;
          }
        }
        if (ready) break;
        Thread.sleep(1000);
      }
    } catch (Exception e) {
      // Index might already exist
    }
  }
}
