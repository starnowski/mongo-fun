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
public class CharacterFilterAnalyzerTest {

  @Autowired protected MongoClient mongoClient;

  private static final String INDEX_NAME = "custom_movie_analyzer_idx";

  @ParameterizedTest
  @CsvSource({
    "1, The Lion King I, 1994",
    "2, The Lion King II, 1998",
    "3, The Lion King III, 2004"
  })
  @MongoSetup(
      mongoDocuments = {
        @MongoDocument(
            database = "testdb",
            collection = "movies_char",
            bsonFilePath = "bson/search/char_movie_i.json"),
        @MongoDocument(
            database = "testdb",
            collection = "movies_char",
            bsonFilePath = "bson/search/char_movie_ii.json"),
        @MongoDocument(
            database = "testdb",
            collection = "movies_char",
            bsonFilePath = "bson/search/char_movie_iii.json")
      })
  public void shouldFindMovieByDecimalNumber(
      String searchQuery, String expectedTitle, int expectedYear) throws InterruptedException {
    // GIVEN
    MongoDatabase database = mongoClient.getDatabase("testdb");
    MongoCollection<Document> collection = database.getCollection("movies_char");
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
                    .formatted(INDEX_NAME, searchQuery)),
            Document.parse(
                """
            {
              "$project": {
                "title": 1,
                "year": 1
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
    TestHelper.runAssertion(
        20,
        1,
        () -> {
          results.clear();
          collection.aggregate(pipeline).into(results);
          Assertions.assertFalse(
              results.isEmpty(), "Expected to find " + expectedTitle + " for query " + searchQuery);
          Document movie = results.get(0);
          Assertions.assertEquals(expectedTitle, movie.getString("title"));
          Assertions.assertEquals(expectedYear, movie.getInteger("year"));
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
            "analyzer": "custom_movie_analyzer",
            "searchAnalyzer": "custom_movie_analyzer",
            "mappings": {
              "dynamic": false,
              "fields": {
                "title": {
                  "type": "string"
                }
              }
            },
            "analyzers": [
              {
                "name": "custom_movie_analyzer",
                "charFilters": [
                  {
                    "mappings": {
                      "I": "1",
                      "II": "2",
                      "III": "3",
                      "IV": "4",
                      "IX": "9",
                      "V": "5",
                      "VI": "6",
                      "VII": "7",
                      "VIII": "8",
                      "X": "10"
                    },
                    "type": "mapping"
                  }
                ],
                "tokenizer": {
                  "type": "standard"
                },
                "tokenFilters": [
                  {
                    "type": "icuFolding"
                  }
                ]
              }
            ]
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
