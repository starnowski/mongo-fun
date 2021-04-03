
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
  // Close clientc
  await client.close();
});


describe("Basic mongo operations", () => {
  beforeAll(async () => {
    matchCollection = db.collection('facets-tests');
    const query = { _id: {$exists: true} };
    await matchCollection.deleteMany(query);
    // Add data
    // this option prevents additional documents from being inserted if one fails
    const options = { ordered: true };
    const translators = [
          { t_id: "t1", name: "Szymon Tarnowski", languages: ["English", "Polish"], description: "I am amateur scuba diver and runner" },
          { t_id: "t2", name: "Michael Anonim", languages: ["Polish"], kids: [], description: "I am amateur runner" },
          { t_id: "t3", name: "Kuba Doe", languages: ["Polish", "English", "Russian"], kids: ["John", "Jack", "Alexandra", "Michael"], description: "I am runner and climber" },
          { t_id: "t4", name: "Bill Clinton", languages: ["English"], kids: ["Chelsea"], description: "I am former president" },
          { t_id: "t5", name: "Andrea Doe", languages: ["English", "German"], kids: [], description: "Beautician" },
          { t_id: "t6", name: "Judy Anonim", languages: ["English", "German"], kids: [], description: "I am Scrum master" },
          { t_id: "t7", name: "Konrad Anonim", languages: ["Polish"], kids: ["Jagoda"], description: "I am electrician" },
          { t_id: "t8", name: "Mikka Anonim", languages: ["Polish"], kids: ["Jill"], description: "I am electrician" },
          { t_id: "t9", name: "Daniel Doe", languages: ["Italian"], kids: ["Carmen", "Michael"], description: "I am amateur scuba diver" },
          { t_id: "t10", name: "Viki Doe", languages: ["English"], kids: ["Arnold", "Henry"], description: "I am model" }
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
    expect(result[0].countResult).toEqual(10);
  });
});