
//Install mongo on ubuntu : https://docs.mongodb.com/manual/tutorial/install-mongodb-on-ubuntu/
//https://www.npmjs.com/package/mongodb
//https://developer.mozilla.org/en-US/docs/Learn/JavaScript/Asynchronous/Async_await

//How to create database in mongoDB
//https://www.w3schools.com/nodejs/nodejs_mongodb_create_db.asp

//How to create index
//https://docs.mongodb.com/drivers/node/current/fundamentals/indexes/

// Setup and TearDown
//https://jestjs.io/docs/setup-teardown

// Asynchronous testing
//https://jestjs.io/docs/asynchronous

// jsonPath
//https://www.npmjs.com/package/jsonpath

//--runTestsByPath
// npm run test-jest ./test/jest/__tests__/facets-tests-explain.js

const MongoClient = require('mongodb').MongoClient;
const assert = require('assert');
const jsonPath = require('jsonpath');

// Connection URI
const uri =
  "mongodb://localhost:27017/aggregation-tests?readPreference=primary&ssl=false";
// Create a new MongoClient
const client = new MongoClient(uri, {
  useNewUrlParser: true,
  useUnifiedTopology: true
});

var db = null;
var per1Collection = null;
beforeAll( async () => {
  // Establish and verify connection
  await client.connect();
  db = await client.db("performance-tests");
});

afterAll(async () => {
  // Close clientc
  await client.close();
});

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

describe("Performance - create single index operations", () => {
  beforeAll(async () => {
    per1Collection = db.collection('performance-single-index-tests');
    const query = { _id: {$exists: true} };
    await per1Collection.deleteMany(query);
    // Add data
    // this option prevents additional documents from being inserted if one fails
    const options = { ordered: true };
    const translators = [
          { t_id: "t1", name: "Szymon Tarnowski", languages: ["English", "Polish"], salary: 233.225, job: "software engineer", description: "I am amateur scuba diver and runner" },
          { t_id: "t2", name: "Michael Anonim", languages: ["Polish"], kids: [], salary: 133.225, job: "accountant",description: "I am amateur runner" },
          { t_id: "t3", name: "Kuba Doe", languages: ["Polish", "English", "Russian"], kids: ["John", "Jack", "Alexandra", "Michael"], salary: 65.225, job: "software tester",description: "I am runner and climber" },
          { t_id: "t4", name: "Bill Clinton", languages: ["English"], kids: ["Chelsea"], salary: 2088.225, job: "president", description: "I am former president" },
          { t_id: "t5", name: "Andrea Doe", languages: ["English", "German"], kids: [], salary: 80.225, job: "beautician", description: "Beautician" },
          { t_id: "t6", name: "Judy Anonim", languages: ["English", "German"], kids: [], salary: 99.225, job: "scrum master",description: "I am Scrum master" },
          { t_id: "t7", name: "Konrad Anonim", languages: ["Polish"], kids: ["Jagoda"], salary: 76.225, job: "electrician",description: "I am electrician" },
          { t_id: "t8", name: "Mikka Anonim", languages: ["Polish"], kids: ["Jill"], salary: 2.334, job: "receptionist" ,description: "I am electrician" },
          { t_id: "t9", name: "Daniel Doe", languages: ["Italian"], kids: ["Carmen", "Michael"], salary: 55.225, job: "scuba diver",description: "I am amateur scuba diver" },
          { t_id: "t10", name: "Viki Doe", languages: ["English"], kids: ["Arnold", "Henry"], salary: 199.225, job: "model",description: "I am model" },
          { t_id: "t11", name: "Mike Johnson", languages: ["German"], kids: ["Arnold", "Billy"], salary: 79.10, job: "artist",description: "I am artist" },
          { t_id: "t12", name: "Van Diesel", languages: ["English"], kids: ["Berny", "Carl", "Natasha"], salary: 12999.225, job: "actor",description: "I am world start actor, playing in action movies" }
        ];
    await per1Collection.insertMany(translators, options);

    const result = await per1Collection.createIndex({ name: 1 });
    console.log(`Index created: ${result}`);
  });
  test("should count all documents", async () => {
    // WHEN
    const result = await per1Collection.aggregate([{ $match: { _id: { $exists: true } }},
                                                    { $count: "countResult" }
                                                    ]).toArray();

    // THEN
    console.log('result: ' + result);
    console.log(result);
    expect(result[0].countResult).toEqual(12);
  });

  test("should contain created indexes", async () => {
    // WHEN
    const result = await db.collection('performance-single-index-tests').indexes();

    // THEN
    var json = JSON.stringify(result);
    console.log('listed indexes: ' + json);
    index = jsonPath.query(result, "$..key.name");
    console.log(index);
    expect(index).toEqual([1]);
  });

    test("should return correct facet with paginated result and count", async () => {
      // GIVEN
      const expectedFacetResult = [
            JSON.stringify({ t_id: "t1" }),
            JSON.stringify({ t_id: "t10" }),
            JSON.stringify({ t_id: "t11" }),
            JSON.stringify({ t_id: "t12" }),
            JSON.stringify({ t_id: "t2" })
          ];
      const expectedCountFacetResult = [
            JSON.stringify({ number_of_records: 12 })
          ];


      // WHEN
      var result = await per1Collection.aggregate([
                                                        {$project: {
                                                            _id: 0,
                                                            t_id: 1
                                                        }}
                                                        ,
                                                        {
                                                            $facet: {
                                                                results: [
                                                                    {$sort: { "t_id": 1 }},
                                                                    {$limit: 5}
                                                                ]
                                                                ,
                                                                count: [
                                                                    {$count: "number_of_records"}
                                                                ]
                                                            }
                                                        }
                                                      ]).toArray();

      // THEN
      console.log('query result: ' + result);
      const resultObject = result.map(function (doc) { return JSON.stringify({ results: doc.results, count: doc.count }) });
      console.log('resultObject: ' + resultObject);
      console.log('expectedFacetResult: ' + expectedFacetResult);
      console.log(expectedFacetResult);
      const ids = result[0].results.map(function (doc) { return JSON.stringify({ t_id: doc.t_id }) });
      console.log('result with limits: ' + ids);
      assertJsonArraysEquals(ids, expectedFacetResult);
      const count = result[0].count.map(function (doc) { return JSON.stringify({ number_of_records: doc.number_of_records }) });
      assertJsonArraysEquals(count, expectedCountFacetResult);
    });

    test("should return correct facet with paginated ids and count", async () => {
      // GIVEN
      const expectedFacetResult = [
            "t1", "t10", "t11"
          ];
      const expectedCountFacetResult = [
            JSON.stringify({ number_of_records: 12 })
          ];


      // WHEN
      var result = await per1Collection.aggregate([
                                                        {$project: {
                                                            _id: 0,
                                                            t_id: 1
                                                        }}
                                                        ,
                                                        {
                                                            $facet: {
                                                                results: [
                                                                    {$sort: { "t_id": 1 }},
                                                                    {$limit: 3}
                                                                ]
                                                                ,
                                                                count: [
                                                                    {$count: "number_of_records"}
                                                                ]
                                                            }
                                                        }
                                                        ,
                                                        {$project: {
                                                            count: 1,
                                                            results: {
                                                                "$map": {
                                                                    "input": "$results",
                                                                    "as": "result",
                                                                    "in": "$$result.t_id"
                                                                }
                                                            }
                                                        }}
                                                      ]).toArray();

      // THEN
      console.log('query result: ' + result);
      const resultObject = result.map(function (doc) { return JSON.stringify({ results: doc.results, count: doc.count }) });
      console.log('resultObject: ' + resultObject);
      console.log('expectedFacetResult: ' + expectedFacetResult);
      console.log(expectedFacetResult);
      const ids = result[0].results;
      console.log('result with limits: ' + ids);
      expect(ids).toEqual(expectedFacetResult);
      const count = result[0].count.map(function (doc) { return JSON.stringify({ number_of_records: doc.number_of_records }) });
      assertJsonArraysEquals(count, expectedCountFacetResult);
    });
});