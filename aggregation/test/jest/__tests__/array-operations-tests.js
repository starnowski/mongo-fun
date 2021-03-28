
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
var arraysCollection = null;
beforeAll( async () => {
  // Establish and verify connection
  await client.connect();
  db = await client.db("aggregation-tests");
});

afterAll(async () => {
  // Close client
  await client.close();
});


describe("Arrays mongo operations", () => {
  beforeEach(async () => {
    arraysCollection = db.collection('arraysCollection');
    const query = { _id: {$exists: true} };
    await arraysCollection.deleteMany(query);
  });

    test("should return all documents where fronted developers are also backend developers", async () => {
     //GIVEN
     // Add data
     // this option prevents additional documents from being inserted if one fails
     const options = { ordered: true };
     const developersTeam = [
              { t_id: "t1", developers : { fronted: ["Jake", "Mike"], backend: ["Mike", "Daniel", "Bill"] } },
              { t_id: "t2", developers : { fronted: ["John"], backend: ["Mika", "Bill"] } },
              { t_id: "t3", developers : { fronted: ["Simon", "Jake"], backend: ["Mika", "Jake", "Simon"] } },
            ];
     await arraysCollection.insertMany(developersTeam, options);
     const expectedTeams = [JSON.stringify({t_id: "t1", fullstackDevelopers: ["Mike"]}),
                JSON.stringify({t_id: "t3", fullstackDevelopers: ["Simon", "Jake"]})];

      // WHEN
      var result = await arraysCollection.aggregate([{ $match: { _id: {$exists: true} }},
                                                    {$project: { t_id: 1, fullstackDevelopers: {$setIntersection: [ "$developers.fronted", "$developers.backend" ] } }},
                                                    { $where: "this.fullstackDevelopers.length > 0" }
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