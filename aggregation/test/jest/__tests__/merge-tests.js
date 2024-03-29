
//Install mongo on ubuntu : https://docs.mongodb.com/manual/tutorial/install-mongodb-on-ubuntu/
//https://www.npmjs.com/package/mongodb
//https://developer.mozilla.org/en-US/docs/Learn/JavaScript/Asynchronous/Async_await

//How to create database in mongoDB
//https://www.w3schools.com/nodejs/nodejs_mongodb_create_db.asp

// Setup and TearDown
//https://jestjs.io/docs/setup-teardown

// Asynchronous testing
//https://jestjs.io/docs/asynchronous

// Run single test
// npm run test-jest ./test/jest/__tests__/merge-tests.js

//https://docs.mongodb.com/manual/reference/operator/aggregation/merge/

const MongoClient = require('mongodb').MongoClient;
const assert = require('assert');

// Connection URI
const uri =
  "mongodb://localhost:27017/aggregation-tests?readPreference=primary&ssl=false";
// Create a new MongoClient
const client = new MongoClient(uri, {
  useNewUrlParser: true,
  useUnifiedTopology: true
});

var db = null;
var inputCollection = null;
var outputCollection = null;
beforeAll( async () => {
  // Establish and verify connection
  await client.connect();
  db = await client.db("aggregation-tests");
});

afterAll(async () => {
  // Close clientc
  await client.close();
});

    const records = [
          { t_id: "t1", name: "Szymon Tarnowski", salary: 233.225, job: "software engineer", description: "I am amateur scuba diver and runner" },
          { t_id: "t2", name: "Michael Anonim", salary: 133.225, job: "accountant",description: "I am amateur runner" },
          { t_id: "t3", name: "Kuba Doe", salary: 65.225, job: "software tester",description: "I am runner and climber" }
        ];

function assertJsonArraysEquals(resultArray, expectedArray)
{
      result = resultArray.map(function (doc) { return JSON.stringify(doc) });
      console.log('resultArray: ' + result);
      console.log(result);
      const expectedJsonObjectArray = expectedArray.map(function (doc) { return JSON.stringify(doc) });
      console.log('expectedArray: ' + expectedJsonObjectArray);
      console.log(expectedJsonObjectArray);
      expect(result.every(elem => expectedJsonObjectArray.includes(elem))).toBeTruthy();
}

function mapToObject(queryResults)
{
    return queryResults.map(function (doc) { return { t_id: doc.t_id, name: doc.name, salary: doc.salary, job: doc.job, description: doc.description } });
}

describe("Merge operations", () => {

  beforeAll(async () => {
    inputCollection = db.collection('inputCollection');
    outputCollection = db.collection('outputCollection');
    await outputCollection.createIndex( { "name": 1 }, { unique: true } );
  });
  beforeEach(async () => {
    const query = { _id: {$exists: true} };
    await inputCollection.deleteMany(query);
    await outputCollection.deleteMany(query);
  });

    test("should export all document from inputCollection to outputCollection", async () => {
      // GIVEN
    const records = [
          { t_id: "t1", name: "Szymon Tarnowski", salary: 233.225, job: "software engineer", description: "I am amateur scuba diver and runner" },
          { t_id: "t2", name: "Michael Anonim", salary: 133.225, job: "accountant",description: "I am amateur runner" },
          { t_id: "t3", name: "Kuba Doe", salary: 65.225, job: "software tester",description: "I am runner and climber" }
        ];
    const options = { ordered: true };
    await inputCollection.insertMany(records, options);
    records.forEach((record) => {delete record._id});

    // WHEN
    await inputCollection.aggregate([
                                                                                  {
                                                                                    $merge: {
                                                                                        into: "outputCollection",
                                                                                        on: "name",
                                                                                        whenMatched: "merge",
                                                                                        whenNotMatched: "insert"
                                                                                    }
                                                                                  }
                                                                                  ]).toArray();

      // THEN
      const results = await outputCollection.aggregate([
                                                    {$match: { _id: { $exists: true } }}
                                                    ,
                                                    {$project:
                                                        {
                                                            _id: 0
                                                        }
                                                    }
                                                    ]).toArray();
      console.log('query results : ' + results);
      assertJsonArraysEquals(mapToObject(results), records);
    });
    test("should export all converted output from inputCollection to outputCollection", async () => {
      // GIVEN
    const records = [
          { t_id: "t1", name: "Szymon Tarnowski", salary: 233.225, job: {title: "software engineer"} },
          { t_id: "t2", name: "Michael Anonim", salary: 133.225, job: {title: "accountant"} },
          { t_id: "t3", name: "Kuba Doe", salary: 65.225, job: {title: "software tester"} }
        ];
    const expectedRecords = [
          { t_id: "t1", name: "Szymon Tarnowski", salary: 233.225, job: "software engineer" },
          { t_id: "t2", name: "Michael Anonim", salary: 133.225, job: "accountant" },
          { t_id: "t3", name: "Kuba Doe", salary: 65.225, job: "software tester" }
        ];
    const options = { ordered: true };
    await inputCollection.insertMany(records, options);
    records.forEach((record) => {delete record._id});

    // WHEN
    await inputCollection.aggregate([
                                                            {
                                                                $replaceRoot: { newRoot: { $mergeObjects: [ "$$ROOT", { job: "$job.title"} ] } }
                                                            }
                                                            ,
                                                                                  {
                                                                                    $merge: {
                                                                                        into: "outputCollection",
                                                                                        on: "name",
                                                                                        whenMatched: "merge",
                                                                                        whenNotMatched: "insert"
                                                                                    }
                                                                                  }
                                                                                  ]).toArray();

      // THEN
      var results = await outputCollection.aggregate([
                                                    {$match: { _id: { $exists: true } }}
                                                    ,
                                                    {$project:
                                                        {
                                                            _id: 0
                                                        }
                                                    }
                                                    ]).toArray();
      console.log('query results : ' + results);
      assertJsonArraysEquals(mapToObject(results), mapToObject(expectedRecords));
    });
});