import pytest
from testcontainers.mongodb import MongoDbContainer
from pymongo import MongoClient

@pytest.fixture(scope="session")
def mongodb_container():
    """
    Manages the lifecycle of the MongoDB Atlas local Docker container.
    Using scope='session' so the container is only started once per test session.
    """
    # Use the specific Atlas Local image as requested
    with MongoDbContainer("mongodb/mongodb-atlas-local:7.0.11") as container:
        yield container

@pytest.fixture(scope="session")
def mongo_client(mongodb_container):
    """
    Provides an authenticated and connected MongoClient instance to the running container.
    """
    client = mongodb_container.get_connection_client()
    # Alternatively, you could use MongoClient(mongodb_container.get_connection_url())
    yield client
