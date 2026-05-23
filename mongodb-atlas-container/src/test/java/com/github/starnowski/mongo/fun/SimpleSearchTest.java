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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.stream.XMLStreamException;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {SearchDemoApplication.class})
@AutoConfigureMockMvc
@ExtendWith(SpringMongoDataLoaderExtension.class)
public class SimpleSearchTest {

  @Autowired protected MongoClient mongoClient;

  @ParameterizedTest
  @MethodSource("provideSearchTests")
  @MongoSetup(
      mongoDocuments = {
        @MongoDocument(
            database = "testdb",
            collection = "Items",
            bsonFilePath = "bson/search/search1.json"),
        @MongoDocument(
            database = "testdb",
            collection = "Items",
            bsonFilePath = "bson/search/search2.json")
      })
  public void shouldReturnExpectedDocumentsBasedOnSearchOperator(
      String search, Set<String> expectedPlainStrings)
      throws XMLStreamException, InterruptedException {
    // GIVEN
    MongoDatabase database = mongoClient.getDatabase("testdb");
    MongoCollection<Document> collection = database.getCollection("Items");
    ensureSearchIndex(collection);

    List<Bson> pipeline = List.of(Document.parse(search));
    System.out.println(new Document("pipeline", pipeline).toJson());

    // THEN
    List<Document> results = new ArrayList<>();
    await()
        .atMost(5, SECONDS)
        .pollInterval(1, SECONDS)
        .until(
            () -> {
              results.clear();
              collection.aggregate(pipeline).into(results);
              return results.size() == expectedPlainStrings.size();
            });

    Assertions.assertEquals(expectedPlainStrings.size(), results.size());
    Set<String> actual =
        results.stream()
            .map(d -> d.get("plainString"))
            .filter(Objects::nonNull)
            .map(s -> (String) s)
            .collect(Collectors.toSet());
    Assertions.assertEquals(expectedPlainStrings, actual);
  }

  private void ensureSearchIndex(MongoCollection<Document> collection) {
    try {
      collection.createSearchIndex(
          "atlas_search_index", new Document("mappings", new Document("dynamic", true)));
      // Wait for index to be ready
      await()
          .atMost(30, SECONDS)
          .pollInterval(1, SECONDS)
          .until(
              () -> {
                for (Document index : collection.listSearchIndexes()) {
                  if ("atlas_search_index".equals(index.getString("name"))
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

  private static java.util.stream.Stream<Arguments> provideSearchTests() {
    return java.util.stream.Stream.of(
        Arguments.of(
            """
                        { "$search": { "index": "atlas_search_index", "queryString": { "query": "database AND search", "defaultPath": "plainString" }}}
                        """,
            Set.of("database search")),
        Arguments.of(
            """
                        { "$search": { "index": "atlas_search_index", "queryString": { "query": "search", "defaultPath": "plainString" }}}
                        """,
            Set.of("database search", "only search")),
        Arguments.of(
            """
                        {"$search":{"index":"atlas_search_index","queryString":{"query":"database OR \\"only search\\"","defaultPath":"plainString"}}}
                        """,
            Set.of("database search", "only search")));

    // "database OR \"only search\""
  }
}
