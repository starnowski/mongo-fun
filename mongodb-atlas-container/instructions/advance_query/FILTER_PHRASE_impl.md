# Implementation Plan - Filter Phrase Search Test

This plan outlines the steps implemented for `FilterStringPhraseTest` as described in `FILTER_PHRASE.md`.

## Tasks

- [x] Create test data BSON files in `src/test/resources/bson/search/`
    - [x] `filter_phrase_1.json`: Type "groccery"
    - [x] `filter_phrase_2.json`: Type "Groccery"
- [x] Create `src/test/java/com/github/starnowski/mongo/fun/FilterStringPhraseTest.java`
    - [x] Add `@SpringBootTest` and other necessary annotations.
    - [x] Implement `ensureSearchIndex` method for explicit string mapping of the `type` field.
    - [x] Implement `waitForSearchIndexSync` method.
    - [x] Implement `shouldReturnBothDocumentsWhenSearchingByTypeUsingPhrase` test method (runs case-insensitively using `phrase` query).
    - [x] Implement `shouldReturnBothDocumentsWhenSearchingByTypeUsingCompoundFilter` test method (runs case-insensitively using compound filter with `phrase` query).
        - [x] Use `@MongoSetup` to load the BSON files.
        - [x] Search for "Groccery", "GROCCERY", and "groccery" and assert that both documents are found.
- [x] Verify implementation by running the new test.
    - [x] `mvn test -Dtest=FilterStringPhraseTest`
