# Implementation Plan for `should` operator tests

Based on the requirements in `SHOULD.md`, this plan outlines the steps to implement integration tests for the Atlas Search `should` operator.

- [x] **Prepare Test Data**
    - [x] Create `src/test/resources/bson/search/should_movie_poet.json`:
        - Title: "The Poet"
        - Plot: "A story about a lonely poet."
    - [x] Create `src/test/resources/bson/search/should_movie_elizabeth.json`:
        - Title: "Elizabeth"
        - Plot: "Historical drama about Queen Elizabeth."
    - [x] Create `src/test/resources/bson/search/should_movie_both.json`:
        - Title: "Elizabeth the Poet"
        - Plot: "A fictional story where Queen Elizabeth is a poet."
- [x] **Create `ShouldOperatorTest.java`**
    - [x] Define the class in `com.github.starnowski.mongo.fun` package.
    - [x] Use `@SpringBootTest`, `@AutoConfigureMockMvc`, and `@ExtendWith(SpringMongoDataLoaderExtension.class)`.
    - [x] Inject `MongoClient`.
    - [x] Implement `ensureSearchIndex(MongoCollection<Document> collection)`:
        - Create an index named `should_plot_idx` with dynamic mapping or specific field mapping for `plot`.
    - [x] Implement `waitForSearchIndexSync(MongoCollection<Document> collection, String indexName)`:
        - Use `$searchMeta` to wait for all documents to be indexed.
- [x] **Implement Test Cases**
    - [x] **Test 1: Default `minimumShouldMatch` (implicitly 1)**
        - [x] Annotate with `@Test` and `@MongoSetup` using the three new BSON files.
        - [x] Execute `$search` aggregation with `compound` -> `should` containing queries for "poet" and "Elizabeth".
        - [x] Assert that at least 3 movies are returned (Poet, Elizabeth, Both).
    - [x] **Test 2: Explicit `minimumShouldMatch: 2`**
        - [x] Annotate with `@Test` and `@MongoSetup`.
        - [x] Execute `$search` aggregation with `compound` -> `should` (same queries) and `"minimumShouldMatch": 2`.
        - [x] Assert that only 1 movie is returned (Both).
    - [x] **Test 3: Error case with `minimumShouldMatch: 3`**
        - [x] Annotate with `@Test` and `@MongoSetup`.
        - [x] Execute `$search` aggregation with `compound` -> `should` (2 conditions) and `"minimumShouldMatch": 3`.
        - [x] Assert that a `com.mongodb.MongoCommandException` is thrown (or similar error indicating invalid `minimumShouldMatch`).
- [x] **Verification**
    - [x] Run the tests using Maven: `mvn test -Dtest=ShouldOperatorTest`.
    - [x] Ensure all 3 test cases pass as expected.
