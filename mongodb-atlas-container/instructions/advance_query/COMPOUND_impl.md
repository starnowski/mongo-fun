# Implementation Plan - Compound Operator Tests

This plan outlines the steps to implement integration tests for the Atlas Search `compound` operator, as specified in `COMPUND.md`.

## 1. Data Preparation
Prepare BSON files in `src/test/resources/bson/search/` to support various test cases.

- [x] Create `compound_movies.json` with a diverse set of movies covering different years, genres, plots, and cast members.
    - Movie 1: Title "Star Wars", Year 1977, Plot "Space opera", Genres ["Action", "Sci-Fi"], Cast ["Mark Hamill", "Harrison Ford"]
    - Movie 2: Title "Star Wars: Episode I - The Phantom Menace", Year 1999, Plot "Jedi knights", Genres ["Action", "Adventure"], Cast ["Liam Neeson", "Ewan McGregor"]
    - Movie 3: Title "The Poet", Year 1934, Plot "A poet falls in love with Elizabeth", Genres ["Drama", "Romance"], Cast ["Unknown"]
    - Movie 4: Title "Elizabeth", Year 1998, Plot "The early years of Elizabeth I", Genres ["Biography", "Drama", "History"], Cast ["Cate Blanchett"]
    - Movie 5: Title "Documentary on Earth", Year 1995, Plot "Earth's nature", Genres ["Documentary"], Cast ["David Attenborough"]
    - Movie 6: Title "Action Movie 2000", Year 2000, Plot "Earth is in danger", Genres ["Action"], Cast ["John Leguizamo"]

## 2. Test Class Infrastructure
Create the foundation for `CompoundOperatorTest.java`.

- [x] Create `src/test/java/com/github/starnowski/mongo/fun/CompoundOperatorTest.java`.
- [x] Define constants for Database, Collection, and Index names.
- [x] Implement `ensureSearchIndex` with appropriate mappings:
    - `plot`: string
    - `title`: string
    - `genres`: string
    - `year`: number (int64)
    - `cast`: string
- [x] Implement `waitForSearchIndexSync` helper method.

## 3. Test Case Implementation
Implement the specific test cases requested in `COMPUND.md`.

- [x] **Test Case 1: `must` and `mustNot` clauses**
    - Search for plot words "poet" AND "Elizabeth" (must).
    - Exclude genres "History" OR "Documentary" (mustNot).
    - Verify that "The Poet" (if it fits) or relevant movies are returned, and "Elizabeth" (History genre) is excluded.
- [x] **Test Case 2: `must` and `should` clauses**
    - `should` match plot words "poet" OR "Elizabeth".
    - `must` match year 1934.
    - Verify that only "The Poet" is returned.
- [x] **Test Case 3: `must`, `mustNot`, `should`, and `filter` clauses**
    - `filter`: year between 1992 and 2000.
    - `must`: have "genres" field AND plot contains "earth".
    - `mustNot`: genres "Documentary", "Drama", or "Comedy".
    - `should`: cast includes "John Leguizamo", "Gillian Anderson", or "Paula Marshall".
    - `minimumShouldMatch`: 1.
    - Verify that "Action Movie 2000" is returned.
- [x] **Test Case 4: Nested `compound` query**
    - Nested `should` with two `compound` clauses:
        1. `must`: title "Star Wars" AND cast "Liam Neeson".
        2. `must`: genre "Action" AND `mustNot`: cast "Liam Neeson".
    - Verify results include "Star Wars: Episode I" (matches first) and "Action Movie 2000" (matches second).

## 4. Verification
- [x] Run `mvn test -Dtest=CompoundOperatorTest` to verify all test cases pass.
- [x] Ensure `spotless:apply` is run to maintain code style.
