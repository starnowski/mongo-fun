package com.github.starnowski.mongo.fun;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

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
public class MultiOptionSearchTest {

  @Autowired protected MongoClient mongoClient;

  private static final String INDEX_NAME = "title_folding_index";

  @ParameterizedTest
  @CsvSource({
    "Pokemon The First Movie, Pokèmon: The First Movie - Mewtwo Strikes Back",
    "Pokèmon, Pokèmon: The First Movie - Mewtwo Strikes Back"
  })
  @MongoSetup(
      mongoDocuments = {
        @MongoDocument(
            database = "testdb",
            collection = "movies_token",
            bsonFilePath = "bson/search/token_movie_pokemon1.json"),
        @MongoDocument(
            database = "testdb",
            collection = "movies_token",
            bsonFilePath = "bson/search/token_movie_pokemon2.json"),
        @MongoDocument(
            database = "testdb",
            collection = "movies_token",
            bsonFilePath = "bson/search/token_movie_the_first_movie.json"),
        @MongoDocument(
            database = "testdb",
            collection = "movies_token",
            bsonFilePath = "bson/search/token_movie_movie_movie.json"),
        @MongoDocument(
            database = "testdb",
            collection = "movies_token",
            bsonFilePath = "bson/search/token_movie_the_forty_first.json"),
        @MongoDocument(
            database = "testdb",
            collection = "movies_token",
            bsonFilePath = "bson/search/token_movie_the_first_grader.json")
      })
  public void shouldFindMoviesByTitleWithFoldingByUsingAlternativeAnalyzerForField(
      String query, String expectedString) throws InterruptedException {
    // GIVEN
    MongoDatabase database = mongoClient.getDatabase("testdb");
    MongoCollection<Document> collection = database.getCollection("movies_token");
    ensureSearchIndex(collection);
    waitForSearchIndexSync(collection, INDEX_NAME);

    List<Bson> pipeline =
        List.of(
            Document.parse(
                """
                            {
                              "$search": {
                                "index": "%s",
                                "text": {
                                  "query": "%s",
                                  "path": "title"
                                }
                              }
                            }
                            """
                    .formatted(INDEX_NAME, query)),
            Document.parse(
                """
                            {
                              "$project": {
                                "_id": 0,
                                "title": 1
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
          Assertions.assertFalse(
              results.isEmpty(), "Expected to find '%s'".formatted(expectedString));
          boolean found =
              results.stream().anyMatch(doc -> doc.getString("title").equals(expectedString));
          Assertions.assertTrue(
              found, "Expected to find '%s' in results: ".formatted(expectedString) + results);
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
            "analyzer": "diacriticFolder",
            "searchAnalyzer": "diacriticFolder",
            "analyzers": [
              {
                "name": "diacriticFolder",
                "charFilters": [],
                "tokenizer": {
                  "type": "standard"
                },
                "tokenFilters": [
                  {
                    "type": "icuFolding"
                  }
                ]
              }
            ],
            "mappings": {
              "dynamic": false,
              "fields": {
                "title": {
                  "type": "string"
                }
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
    }
  }
}
