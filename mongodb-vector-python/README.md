# mongodb-vector-python

This project tests MongoDB's vector search capabilities using the official Python driver (`pymongo`). It includes an automated integration testing setup that leverages a local MongoDB Atlas container.

## Testing Setup

The project uses `pytest` and `testcontainers` to automate integration testing.

### Prerequisites

- **Docker Desktop** must be installed and running on your machine (to run the Atlas container).
- **uv** must be installed to manage Python dependencies and run the tests.

### Running the Tests

To run the integration tests, execute the following command from the root of this module:

```bash
uv run pytest
```

### What the Tests Cover

- **MongoDB Atlas Local (`tests/conftest.py`)**: Uses `testcontainers` to automatically spin up a local instance of MongoDB Atlas using the `mongodb/mongodb-atlas-local:7.0.11` Docker image. This provides a fresh, isolated database environment for every test session.
- **Vector Search Index Creation (`tests/test_vector_search_index.py`)**: Verifies that we can correctly create a `vectorSearch` index designed for auto-embedding workflows. It uses PyMongo's `SearchIndexModel` to configure the index and polls the database until the new index reaches a `READY` and queryable state.
