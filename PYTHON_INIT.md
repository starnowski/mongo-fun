### Goal

Create the python project that would use MongoDB Python driver.
The idea is to test MongoDB vector search capabilities.
Try to create python project based on known good practices.
In the mongodb-atlas-container for MongoDB Atlas integration tests there was the mongodb/mongodb-atlas-local:7.0.11 docker
image used.
Used the same image in the integration tests in python project.
Create single tests that creates index.
Let's assume that for tests purpose we are going to use auto embedding (feature available in Mongo Atlas).
The MongoDB Atlas is responsible for creating embedding during adding document or query.

