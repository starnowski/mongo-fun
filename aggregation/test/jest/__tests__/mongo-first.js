//https://jestjs.io/docs/mongodb


//const {MongoClient} = require('mongodb');
//
//describe('insert', () => {
//  let connection;
//  let db;
//
//  beforeAll(async () => {
//    connection = await MongoClient.connect(global.__MONGO_URI__, {
//      useNewUrlParser: true,
//    });
//    db = await connection.db(global.__MONGO_DB_NAME__);
//  });
//
//  afterAll(async () => {
//    await connection.close();
//    await db.close();
//  });
//
//  test('should insert a doc into collection', async () => {
//    const users = db.collection('users');
//
//    const mockUser = {_id: 'some-user-id', name: 'John'};
//    await users.insertOne(mockUser);
//
//    const insertedUser = await users.findOne({_id: 'some-user-id'});
//    expect(insertedUser).toEqual(mockUser);
//  });
//});

//Install mongo on ubuntu : https://docs.mongodb.com/manual/tutorial/install-mongodb-on-ubuntu/
//https://www.npmjs.com/package/mongodb
//https://developer.mozilla.org/en-US/docs/Learn/JavaScript/Asynchronous/Async_await


const MongoClient = require('mongodb').MongoClient;
const assert = require('assert');

// Connection URI
const uri =
  "mongodb://localhost:27017/?readPreference=primary&ssl=false";
// Create a new MongoClient
const client = new MongoClient(uri, {
  useNewUrlParser: true,
  useUnifiedTopology: true
});
//client.connect();
//const db = client.db("admin").command({ ping: 1 });
//const db = client.db("admin");


describe("Basic mongo operations", () => {
  test("should insert simple documents", async () => {

    await client.connect();
    // Establish and verify connection
    const db = await client.db("admin");
    const pizzaCollection = db.collection('pizzaCollection');
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