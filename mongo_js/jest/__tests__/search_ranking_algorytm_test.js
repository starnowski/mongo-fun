


const MongoClient = require('mongodb').MongoClient;
const assert = require('assert');

let mongoUrl = null;
if (process.env.MONGO_HOST == null) {
  mongoUrl = "mongodb://localhost:27017/rank-algo-tests?readPreference=primary&ssl=false";
} else {
  mongoUrl = `mongodb://${process.env.MONGO_HOST}/rank-algo-tests?readPreference=primary&ssl=false`;
}
// Connection URI
// Create a new MongoClient
const client = new MongoClient(mongoUrl, {
  useNewUrlParser: true,
  useUnifiedTopology: true
});

var db = null;
var arraysCollection = null;

const testData = [
  {
    pipeline: [{
          $match: {
            $or: [
              { prop1 : { $eq: "A"} },
              { prop2 : { $eq: 443} }
            ]
          }
      }
      ,
      {
          $project: { r_1: 1, _id: 0}
      }
    ]
    ,
    expectedResults: [{ r_1: "t1" }, { r_1: "t8" }, {r_1: "t11"}],
    testDescription: "pipeline that matches document based on two criteria"
  }
];

beforeAll( async () => {
  // Establish and verify connection
  await client.connect();
  db = client.db("rank-algo-tests");
  db.createCollection('arraysCollection');
  arraysCollection = db.collection('arraysCollection');
  console.log("arraysCollection: " + arraysCollection);
  await arraysCollection.createIndex(
    { prop1: 1 } );
  await arraysCollection.createIndex(
      { prop2: 1 } );

  const query = { _id: {$exists: true} };
  await arraysCollection.deleteMany(query);
  const options = { ordered: true };
  const developersTeam = [
                { r_1: "t1", prop1: "A", prop2: 12 },
                { r_1: "t2", prop1: "B", prop2: 13 },
                { r_1: "t3", prop1: "C", prop2: 14 },
                { r_1: "t4", prop1: "D", prop2: 15 },
                { r_1: "t5", prop1: "E", prop2: 16 },
                { r_1: "t6", prop1: "F", prop2: 17 },
                { r_1: "t7", prop1: "G", prop2: 18 },
                { r_1: "t8", prop1: "A", prop2: 19 },
                { r_1: "t9", prop1: "S", prop2: 22 },
                { r_1: "t10", prop1: "K", prop2: 231 },
                { r_1: "t11", prop1: "A", prop2: 443 },
                { r_1: "t12", prop1: "B", prop2: 765 },
                { r_1: "t13", prop1: "C", prop2: 23 },
                { r_1: "t14", prop1: "D", prop2: 87 },
                { r_1: "t15", prop1: "F", prop2: 990 },
                { r_1: "t16", prop1: "AD", prop2: 2 },
                { r_1: "t17", prop1: "BC", prop2: 313 },
                { r_1: "t18", prop1: "TU", prop2: 54 },
                { r_1: "t19", prop1: "YA", prop2: 7 },
                { r_1: "t20", prop1: "TAJ", prop2: 12 }
              ];
  await arraysCollection.insertMany(developersTeam, options);
});

afterAll(async () => {
  const query = { _id: {$exists: true} };
  await arraysCollection.deleteMany(query);
  // Close client
  await client.close();
});

describe("Aggregation mongo operations", () => {
  beforeEach(async () => {
    console.log("Running tests on mongodb : " + mongoUrl);
  });

    test("should return all documents with initila for fronted developers", async () => {
     //GIVEN
     const expectedTeams = [{ r_1: "t1" }, { r_1: "t8" }, {r_1: "t11"}];

      // WHEN
      var result = await arraysCollection.aggregate([{
                                                        $match: {
                                                          $or: [
                                                            { prop1 : { $eq: "A"} },
                                                            { prop2 : { $eq: 443} }
                                                          ]
                                                        }
                                                    }
                                                    ,
                                                    {
                                                        $project: { r_1: 1, _id: 0}
                                                    }
                                                      ]).toArray();

      // THEN
      console.log('result: ' + result);
      console.log(result);
      // result = result.map(function (doc) { return JSON.stringify({ t_id: doc.t_id, initialsFD: doc.initialsFD }) });
      console.log('current teams: ' + result);
      console.log('expected teams: ' + expectedTeams);
      // expect(result.every(elem => expectedTeams.includes(elem))).toBeTruthy();
      expect(expectedTeams).toEqual(result);
    });


    testData.forEach(testCase => {
      test(`should return expected documents based on aggeregation pipeline: ${testCase.testDescription}`, async () => {
        //GIVEN
        const expectedTeams = testCase.expectedResults;
   
         // WHEN
         var result = await arraysCollection.aggregate(testCase.pipeline).toArray();
   
         // THEN
         console.log('result: ' + result);
         console.log(result);
         // result = result.map(function (doc) { return JSON.stringify({ t_id: doc.t_id, initialsFD: doc.initialsFD }) });
         console.log('current teams: ' + result);
         console.log('expected teams: ' + expectedTeams);
         // expect(result.every(elem => expectedTeams.includes(elem))).toBeTruthy();
         expect(expectedTeams).toEqual(result);
       });
    });

});