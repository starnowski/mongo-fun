
//Install mongo on ubuntu : https://docs.mongodb.com/manual/tutorial/install-mongodb-on-ubuntu/
//https://www.npmjs.com/package/mongodb
//https://developer.mozilla.org/en-US/docs/Learn/JavaScript/Asynchronous/Async_await

//How to create database in mongoDB
//https://www.w3schools.com/nodejs/nodejs_mongodb_create_db.asp

// Setup and TearDown
//https://jestjs.io/docs/setup-teardown

// Asynchronous testing
//https://jestjs.io/docs/asynchronous

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
  // Close client
  await client.close();
});


describe("Basic mongo operations", () => {
  beforeAll(async () => {
    matchCollection = db.collection('matchCollection');
    const query = { _id: {$exists: true} };
    await matchCollection.deleteMany(query);
    // Add data
    // this option prevents additional documents from being inserted if one fails
    const options = { ordered: true };
    const translators = [
          { t_id: "t1", name: "Jill", town: "Kanto", languages: ["English", "Spanish"] },
          { t_id: "t2", name: "Joe", town: "Kanto", languages: ["Russian"] },
          { t_id: "t3", name: "Joe", town: "Harris", languages: ["Germany", "Russian"] },
          { t_id: "t4", name: "Leon", town: "Galar", languages: ["English", "Germany", "Spanish"] },
          { t_id: "t5", name: "Anonim", town: "Gal", languages: ["Italian", "English", "Russian"] }
        ];
    await matchCollection.insertMany(translators, options);
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
  test("should find all documents for $all operator", async () => {
    //GIVEN
    const expectedTranslatorIds = ["t1", "t4"];

    // WHEN
    var result = await matchCollection.aggregate([{ $match: { languages: { $all: [ "English" , "Spanish" ]} }}
                                                    ]).toArray();

    // THEN
    console.log('result: ' + result);
    console.log(result);
    result = result.map(function (doc) { return doc.t_id })
    expect(result.every(elem => expectedTranslatorIds.includes(elem))).toBeTruthy();
    expect(result.length).toEqual(2);
  });
    test("should find all documents that do not contains Spanish or Germany", async () => {
      //GIVEN
      const expectedTranslatorIds = ["t2", "t5"];

      // WHEN
      var result = await matchCollection.aggregate([{ $match: { $and: [ { "languages": { $not: { $in: ["Spanish", "Germany"] } } } ] }}
                                                      ]).toArray();

      // THEN
      console.log('result: ' + result);
      console.log(result);
      result = result.map(function (doc) { return doc.t_id })
      expect(result.every(elem => expectedTranslatorIds.includes(elem))).toBeTruthy();
      expect(result.length).toEqual(2);
    });
});