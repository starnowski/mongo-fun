
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
var pizzaCollection = null;
beforeAll( async () => {
  // Establish and verify connection
  await client.connect();
  db = await client.db("aggregation-tests");
});

describe("Basic mongo operations", () => {
  beforeAll(async () => {
    pizzaCollection = db.collection('pizzaCollection');
  });
  beforeEach(async () => {
      const query = { _id: {$exists: true} };
      await pizzaCollection.deleteMany(query);
  });
  test("should insert simple documents", async () => {

    const options = { ordered: true };
    const pizzaDocument = {
      name: "Neapolitan pizza",
      shape: "round",
      toppings: [ "San Marzano tomatoes", "mozzarella di bufala cheese" ],
    };
    const result = await pizzaCollection.insertOne(pizzaDocument);
    expect(result.insertedCount).toEqual(1);
  });
});