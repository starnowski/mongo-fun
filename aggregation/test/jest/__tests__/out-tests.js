
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
// npm run test-jest ./test/jest/__tests__/out-tests.js

//https://docs.mongodb.com/manual/reference/operator/aggregation/out/

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
var matchCollection = null;
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
          { t_id: "t3", name: "Kuba Doe", salary: 65.225, job: "software tester",description: "I am runner and climber" },
          { t_id: "t4", name: "Bill Clinton", salary: 2088.225, job: "president", description: "I am former president" },
          { t_id: "t5", name: "Andrea Doe", salary: 80.225, job: "beautician", description: "Beautician" },
          { t_id: "t6", name: "Judy Anonim", salary: 99.225, job: "scrum master",description: "I am Scrum master" },
          { t_id: "t7", name: "Konrad Anonim", salary: 76.225, job: "electrician",description: "I am electrician" },
          { t_id: "t8", name: "Mikka Anonim", salary: 2.334, job: "receptionist" ,description: "I am electrician" },
          { t_id: "t9", name: "Daniel Doe", salary: 55.225, job: "scuba diver",description: "I am amateur scuba diver" },
          { t_id: "t10", name: "Viki Doe", salary: 199.225, job: "model",description: "I am model" },
          { t_id: "t11", name: "Mike Johnson", salary: 79.10, job: "artist",description: "I am artist" },
          { t_id: "t12", name: "Van Diesel", alary: 12999.225, job: "actor",description: "I am world start actor, playing in action movies" }
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

describe("Out operations", () => {
  beforeAll(async () => {
    matchCollection = db.collection('out-tests');
    const query = { _id: {$exists: true} };
    await matchCollection.deleteMany(query);
    // Add data
    // this option prevents additional documents from being inserted if one fails
    const options = { ordered: true };
    await matchCollection.insertMany(records, options);
  });
  test("should count all documents", async () => {
    // WHEN
    const result = await matchCollection.aggregate([{ $match: { _id: { $exists: true } }},
                                                    { $count: "countResult" }
                                                    ]).toArray();

    // THEN
    console.log('result: ' + result);
    console.log(result);
    expect(result[0].countResult).toEqual(12);
  });
    test("should export all document from out-tests to out-tests-2", async () => {
      // GIVEN
      const outputCollection = db.collection('out-tests');
      const expectedRecords = await matchCollection.aggregate([
                                                                                  {$match: { _id: { $exists: true } }}
                                                                                  ,
                                                                                  {$project:
                                                                                      {
                                                                                          _id: 0
                                                                                      }
                                                                                  }
                                                                                  ]).toArray();

      // WHEN
      await matchCollection.aggregate([
                                    {$out: "out-tests-2"}
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
      console.log('exported results: ' + JSON.stringify(results));
      assertJsonArraysEquals(results, expectedRecords);
    });
});