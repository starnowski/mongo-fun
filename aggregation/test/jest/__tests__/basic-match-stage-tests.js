
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
          { t_id: "t1", personalName: { firstName: "Jill", lastName: "Kanto" }, languages: ["English", "Spanish"], description: "I am English native speaker and also know Spanish" },//9 words
          { t_id: "t2", personalName: { firstName: "Joe", lastName: "Kanto" }, languages: ["Russian"], description: "I am Russian language only" },//5 words
          { t_id: "t3", personalName: { firstName: "Joe", lastName: "Harris" }, languages: ["German", "Russian"], description: "Knows Russian and German language" },//5 words
          { t_id: "t4", personalName: { firstName: "Leon", lastName: "Galar" }, languages: ["English", "German", "Spanish"], description: "I know three language which are Russian and German and Spanish" },//11 words
          { t_id: "t5", personalName: { firstName: "Anonim", lastName: "Gal" }, languages: ["Italian", "English", "Russian"], description: "Knows Russian and Italian and English language" },//7 words
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
    test("should find all documents that do not contains Spanish or German", async () => {
      //GIVEN
      const expectedTranslatorIds = ["t2", "t5"];

      // WHEN
      var result = await matchCollection.aggregate([{ $match: { "languages": { $not: { $in: ["Spanish", "German"] } } }}
                                                      ]).toArray();

      // THEN
      console.log('result: ' + result);
      console.log(result);
      result = result.map(function (doc) { return doc.t_id })
      expect(result.every(elem => expectedTranslatorIds.includes(elem))).toBeTruthy();
      expect(result.length).toEqual(2);
    });
    test("should find all documents that do not contains Spanish or German with usage of $nin operator", async () => {
      //GIVEN
      const expectedTranslatorIds = ["t2", "t5"];

      // WHEN
      var result = await matchCollection.aggregate([{ $match: { "languages": { $nin: ["Spanish", "German"] } }}
                                                      ]).toArray();

      // THEN
      console.log('result: ' + result);
      console.log(result);
      result = result.map(function (doc) { return doc.t_id })
      expect(result.every(elem => expectedTranslatorIds.includes(elem))).toBeTruthy();
      expect(result.length).toEqual(2);
    });
    test("should find all documents by searching by nested fields", async () => {
      //GIVEN
      const expectedTranslatorIds = ["t3"];

      // WHEN
      var result = await matchCollection.aggregate([{ $match: { $and: [ {"personalName.firstName": "Joe"}, {"personalName.lastName": { $eq: "Harris" }} ] }}
                                                      ]).toArray();

      // THEN
      console.log('result: ' + result);
      console.log(result);
      result = result.map(function (doc) { return doc.t_id })
      expect(result.every(elem => expectedTranslatorIds.includes(elem))).toBeTruthy();
      expect(result.length).toEqual(1);
    });
    test("should return all documents with additional filed that contains number of words in description", async () => {
      //GIVEN
      const expectedTranslatorResults = [JSON.stringify({t_id: "t1", numberOfWords: 9}),
            JSON.stringify({t_id: "t2", numberOfWords: 5}),
            JSON.stringify({t_id: "t3", numberOfWords: 5}),
            JSON.stringify({t_id: "t4", numberOfWords: 11}),
            JSON.stringify({t_id: "t5", numberOfWords: 7})];

      // WHEN
      var result = await matchCollection.aggregate([{ $match: { _id: {$exists: true} }},
                                                    {$addFields: { numberOfWords: {$size: { $split: [ "$description", " " ] } } }}
                                                      ]).toArray();

      // THEN
      console.log('result: ' + result);
      console.log(result);
      result = result.map(function (doc) { return JSON.stringify({ t_id: doc.t_id, numberOfWords: doc.numberOfWords }) });
      console.log('current results: ' + result);
      console.log('expected results: ' + expectedTranslatorResults);
      expect(result.every(elem => expectedTranslatorResults.includes(elem))).toBeTruthy();
      expect(result.length).toEqual(5);
    });
});