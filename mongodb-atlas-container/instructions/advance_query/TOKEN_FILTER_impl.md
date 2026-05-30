# Implementation Plan - Token Filter Search Test

This plan outlines the steps to implement the `TokenFilterSearchTest` as described in `TOKEN_FILTER.md`.

## Tasks

- [x] Create test data BSON files in `src/test/resources/bson/search/`
    - [x] `token_movie_pokemon1.json`: Title "Pokèmon: The First Movie - Mewtwo Strikes Back"
    - [x] `token_movie_pokemon2.json`: Title "Pokèmon the Movie: Diancie and the Cocoon of Destruction"
    - [x] `token_movie_the_first_movie.json`: Title "The First Movie"
    - [x] `token_movie_movie_movie.json`: Title "Movie Movie"
    - [x] `token_movie_the_forty_first.json`: Title "The Forty-first"
    - [x] `token_movie_the_first_grader.json`: Title "The First Grader"
- [x] Create `src/test/java/com/github/starnowski/mongo/fun/TokenFilterSearchTest.java`
    - [x] Add `@SpringBootTest` and other necessary annotations.
    - [x] Implement `ensureSearchIndex` method with the `title_folding_index` index and `diacriticFolder` analyzer as defined in `TOKEN_FILTER.md`.
    - [x] Implement `waitForSearchIndexSync` method.
    - [x] Implement `shouldFindMoviesByTitleWithFolding` test method.
        - [x] Use `@MongoSetup` to load the new BSON files.
        - [x] Search for "Pokemon The First Movie".
        - [x] Assert that "Pokèmon: The First Movie - Mewtwo Strikes Back" is found.
- [x] Verify implementation by running the new test.
    - [x] `mvn test -Dtest=TokenFilterSearchTest`
