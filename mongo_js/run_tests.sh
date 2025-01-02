#!/bin/bash

set -e

# Start the docker-compose services in detached mode
docker-compose up -d

# Function to check if MongoDB is ready
is_mongo_ready() {
  docker exec mongodb mongosh --eval "db.stats()"
  docker exec mongodb mongosh --eval "db.stats()" > /dev/null 2>&1
}

# Function to check if the Node.js container is running
# is_node_running() {
#   docker inspect -f '{{.State.Running}}' nodejs
#   docker inspect -f '{{.State.Running}}' nodejs 2>/dev/null | grep -q "true"
# }


# Wait for MongoDB to start
checkCount=1
timeoutInSeconds=180
printf "Waiting for MongoDB to start..."
while ! is_mongo_ready; do
  [[ $checkCount -ne $timeoutInSeconds ]] || break
  checkCount=$(( checkCount+1 ))
  printf "."
  sleep 1
done
printf "\nMongoDB is ready.\n"


# Wait for Nodejs to start
# printf "Waiting for Nodejs to start..."
# while ! is_node_running; do
#   printf "."
#   sleep 1
# done
# printf "\nNodejs is ready.\n"

# Execute build and tests on the Node.js container
printf "Running build and tests on Node.js container...\n"
TEST_OUTPUT=$(docker exec nodejs sh -c "npm install && npm run test-jest")

# Print test results
printf "\nTest Results:\n"
printf "%s\n" "$TEST_OUTPUT"

# Stop and remove containers
printf "Stopping and removing containers...\n"
docker-compose down

# Output test result
printf "\nFinal Test Result:\n"
printf "%s\n" "$TEST_OUTPUT"