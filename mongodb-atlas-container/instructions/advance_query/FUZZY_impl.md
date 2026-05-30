# Implementation Plan - Fuzzy Operator Tests

This plan outlines the steps to implement integration tests for the Atlas Search `fuzzy` option in the `text` operator, as specified in `FUZZY.md`.

## 1. Data Preparation
Prepare BSON files in `src/test/resources/bson/search/` to support various fuzzy test cases.

- [x] Reuse or create BSON files:
    - `fuzzy_movie1.json`: Title "The Poet", Plot "A story about a lonely poet."
    - `fuzzy_movie2.json`: Title "The Poetry", Plot "Advanced poetry analysis."
    - `fuzzy_movie3.json`: Title "The Poem", Plot "A collection of short poems."

## 2. Test Class Infrastructure
Create the foundation for `FuzzyOperatorTest.java`.

- [x] Create `src/test/java/com/github/starnowski/mongo/fun/FuzzyOperatorTest.java`.
- [x] Define constants for Database, Collection, and Index names.
- [x] Implement `ensureSearchIndex` with appropriate mappings:
    - `plot`: string
    - `title`: string
- [x] Implement `waitForSearchIndexSync` helper method.

## 3. Test Case Implementation
Implement the specific test cases requested in `FUZZY.md`.

- [x] **Test Case 1: `maxEdits` option**
    - Search for query "P0et" in "plot" with `fuzzy: {"maxEdits": 1}`.
    - Verify that "The Poet" is returned (distance between "P0et" and "poet" is 1).
- [x] **Test Case 2: `prefixLength` option**
    - Search for query "Poetry" with `fuzzy: {"prefixLength": 4}`.
    - Verify that only words starting with "Poet" are matched.
- [x] **Test Case 3: `maxExpansions` option**
    - Search for query "Poe" with `fuzzy: {"maxExpansions": 1}`.
    - Verify the behavior of limiting expansions.

## 4. Verification
- [x] Run `mvn test -Dtest=FuzzyOperatorTest` to verify all test cases pass.
- [x] Ensure `spotless:apply` is run to maintain code style.
