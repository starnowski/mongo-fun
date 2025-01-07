


const MongoClient = require('mongodb').MongoClient;
const assert = require('assert');

let mongoUrl = null;
if (process.env.MONGO_HOST == null) {
  mongoUrl = "mongodb://localhost:27017/aggregation-tests?readPreference=primary&ssl=false";
} else {
  mongoUrl = `mongodb://${process.env.MONGO_HOST}/aggregation-tests?readPreference=primary&ssl=false`;
}
// Connection URI
// Create a new MongoClient
const client = new MongoClient(mongoUrl, {
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


const getUnicodeNormalizeName = function (developer){
    return developer.normalize("NFC");
}
//Change to true if tests should be executed
const shouldRunTest = false;
//const shouldRunTest = true;

describe("Server side mongo operations", () => {
  beforeEach(async () => {
    console.log("Running tests on mongodb : " + mongoUrl);
    arraysCollection = db.collection('arraysCollection');
    const query = { _id: {$exists: true} };
    await arraysCollection.deleteMany(query);
  });

    (shouldRunTest ? test : test.skip)("should return normalize form", async () => {
     //GIVEN
     // Add data
     // this option prevents additional documents from being inserted if one fails
     const options = { ordered: true };
     const developersTeam = [
              { t_id: "t1", developer : "Mike"},
            ];
     await arraysCollection.insertMany(developersTeam, options);
     const expectedTeams = [JSON.stringify({t_id: "t1", name: "Mike"})];

      // WHEN
      var result = await arraysCollection.aggregate([{
                                                        $addFields: {
                                                            name: {
                                                                $function: {
                                                                    body: getUnicodeNormalizeName.toString(),
                                                                    args: ["$developer"],
                                                                    lang: "js"
                                                                }
                                                            }
                                                        }
                                                    }
                                                      ]).toArray();

      // THEN
      console.log('result: ' + result);
      console.log(result);
      result = result.map(function (doc) { return JSON.stringify(doc) });
      console.log('current teams: ' + result);
      console.log('expected teams: ' + expectedTeams);
      expect(result.every(elem => expectedTeams.includes(elem))).toBeTruthy();
      expect(result.length).toEqual(3);
    });
});