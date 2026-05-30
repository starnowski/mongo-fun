# Implementation Plan - Character Filter Analyzer Test

This plan outlines the steps to implement the `CharacterFilterAnalyzerTest` as described in `CHARACTER.md`.

## Tasks

- [x] Create test data BSON files in `src/test/resources/bson/search/`
    - [x] `char_movie_i.json`: Title "The Lion King I"
    - [x] `char_movie_ii.json`: Title "The Lion King II"
    - [x] `char_movie_iii.json`: Title "The Lion King III"
- [x] Create `src/test/java/com/github/starnowski/mongo/fun/CharacterFilterAnalyzerTest.java`
    - [x] Add `@SpringBootTest` and other necessary annotations.
    - [x] Implement `ensureSearchIndex` method with the custom analyzer `custom_movie_analyzer` as defined in `CHARACTER.md`.
    - [x] Implement `waitForSearchIndexSync` method.
    - [x] Implement `shouldFindMovieByDecimalNumber` test method.
        - [x] Use `@ParameterizedTest` and `@CsvSource`.
        - [x] Map "1" to "The Lion King I", "2" to "The Lion King II", etc.
        - [x] Use `@MongoSetup` to load the new BSON files.
- [ ] Verify implementation by running the new test.
    - [ ] `mvn test -Dtest=CharacterFilterAnalyzerTest` (Attempted, failed due to Docker environment issues with Testcontainers)
