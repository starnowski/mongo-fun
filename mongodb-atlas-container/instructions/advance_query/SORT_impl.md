# Implementation Plan - Sort Operator

This document outlines the steps to implement and verify the Atlas Search `sort` operator as described in `SORT.md`.

## 1. Test Data Preparation
Prepare BSON files with varying titles, release dates, and plots to effectively test sorting and tie-breaking.

- [x] Create `src/test/resources/bson/search/sort_movie1.json` (Title: "A Poet's Life", Released: 2020-01-01, Plot: "A poet travels the world")
- [x] Create `src/test/resources/bson/search/sort_movie2.json` (Title: "B Poet's Life", Released: 2010-01-01, Plot: "A poet travels the world")
- [x] Create `src/test/resources/bson/search/sort_movie3.json` (Title: "C Poet's Life", Released: 2015-01-01, Plot: "A poet travels the world")
- [x] Create `src/test/resources/bson/search/sort_movie4.json` (Title: "D Poet's Life", Released: 2025-01-01, Plot: "A poet travels the world")

## 2. Search Index Configuration
The index must support sorting on string fields and date fields.

- [x] Define and create `sort_idx` in `SortOperatorTest`.
- [x] Ensure `title` is indexed with type `token`.
- [x] Ensure `released` is indexed (dynamic or explicit).

## 3. Implement SortOperatorTest.java
Create a new test class `com.github.starnowski.mongo.fun.SortOperatorTest` based on `MoviesSearchTest.java`.

- [x] Create `SortOperatorTest.java` skeleton.
- [x] Implement `ensureSearchIndex` for `sort_idx`.
- [x] Implement `waitForSearchIndexSync`.
- [x] Implement `shouldSortByScoreAscending` test case.
    - Query: `poet` in `plot`.
    - Sort: `{"unused": {"$meta": "searchScore", "order": 1}}`.
- [x] Implement `shouldSortByScoreDescendingWithTieBreaker` test case.
    - Query: `poet` in `plot`.
    - Sort: `{"unused": {"$meta": "searchScore", "order": -1}, "released": 1}`.
    - *Note: SORT.md text says descending but snippet says order: 1. Implementation will use -1 for descending.*
- [x] Implement `shouldSortByStringField` test case.
    - Query: `poet` in `plot`.
    - Sort: `{"title": 1}`.

## 4. Verification
- [x] Run `mvn clean install` to verify tests pass.
- [x] Run `mvn spotless:apply` to ensure code style compliance.
