
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
// npm run test-jest ./test/jest/__tests__/replace-root-tests.js

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
          { t_id: "t1", name: "Szymon Tarnowski" },
          { t_id: "t2", name: "Michael Anonim", kids: []},
          { t_id: "t3", name: "Kuba Doe", kids: [{name: "John", k_id: "d"}, {name: "Jack", k_id: "b"}, {name: "Alexandra", k_id: "c"}, {name: "Michael", k_id: "e"}] },
          { t_id: "t4", name: "Bill Clinton", kids: [{name: "Chelsea", k_id: "a"}]},
          { t_id: "t5", name: "Andrea Doe", kids: []}
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
    matchCollection = db.collection('replace-root-tests');
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
    expect(result[0].countResult).toEqual(5);
  });
    test("should change document root and set nested document", async () => {
      // GIVEN
      const expectedResult = [
            JSON.stringify({ _id: 0, count: 4 }),
            JSON.stringify({ _id: 1, count: 3 }),
            JSON.stringify({ _id: 2, count: 3 }),
            JSON.stringify({ _id: 3, count: 1 }),
            JSON.stringify({ _id: 'More then four', count: 1 })
          ]

      // WHEN
      var results = await matchCollection.aggregate([
                                                                                  {$unwind: "$kids"}
                                                                                  ,
                                                                                  {$replaceRoot: { newRoot: "$kids" }}
                                                                                  ,
                                                                                  {$sort:
                                                                                      {
                                                                                          k_id: 1
                                                                                      }
                                                                                  }
                                                                                  ]).toArray();


      // THEN
      console.log('results: ' + JSON.stringify(results));
      results = results.map(function (doc) { return JSON.stringify({ _id: doc._id, count: doc.count }) });
      console.log('results: ' + results);
      console.log(results);
      console.log('expectedResult: ' + expectedResult);
      console.log(expectedResult);
      expect(results.every(elem => expectedResult.includes(elem))).toBeTruthy();
    });
});