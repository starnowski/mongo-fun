


const MongoClient = require('mongodb').MongoClient;
const assert = require('assert');
const jp = require('jsonpath');

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
    pipeline: [
      {
        $facet: {
          original: [
            { $match: {}}
          ]
          ,
          col1: [
            {
              $lookup:
                {
                  from: "col1",
                  localField: "r_1",
                  foreignField: "r_1",
                  as: "arrayCol1"
                }
            }
          ]
          ,
          col2: [
            {
              $lookup:
                {
                  from: "col2",
                  localField: "r_1",
                  foreignField: "r_1",
                  as: "arrayCol2"
                }
            }
          ]
        }
      }
      ,
      {
        $project: {
          merged: { $concatArrays: ["$original", "$col1", "$col2"] }
        }
      }
      ,
      {
        $unwind: "$merged"
      }
      ,
      { $replaceRoot: { newRoot: "$merged" } }
      ,
      {
        $group: {
          _id: "$r_1",
          merged: { $mergeObjects: "$$ROOT" }
        }
      }
    ]
    ,
    expectedResults: [{ items:[{ r_1: "t1", rank: 25 }], maxRank: [{ rank: 25, count: 2}] }],
    testDescription: "pipeline that execute facets with lookup and merge result into one document",
    expectedQueryPlanIndexes: [
      {
        jsonPath: "$.stages[0].*.queryPlanner.winningPlan..*.indexName",
        expectedValues: ['prop1_1', 'prop2_1']
      }
    ]
  }
];

beforeAll( async () => {
  // Establish and verify connection
  await client.connect();
  db = client.db("rank-algo-tests");
  db.createCollection('arraysCollection');
  db.createCollection('col1');
  db.createCollection('col2');
  arraysCollection = db.collection('arraysCollection');
  console.log("arraysCollection: " + arraysCollection);
  await arraysCollection.createIndex(
    { prop1: 1 } );
  await arraysCollection.createIndex(
      { prop2: 1 } );

  const query = { _id: {$exists: true} };
  await arraysCollection.deleteMany(query);
  const options = { ordered: true };
  var col1 = db.collection('col1');
  var col2 = db.collection('col2');
  await col1.deleteMany(query);
  await col2.deleteMany(query);

  const mainRecords = [
                { r_1: "t1", prop1: "A"},
                { r_1: "t2", prop1: "B"}
              ];
  await arraysCollection.insertMany(mainRecords, options);

  const col1Records = [
                { r_1: "t1", prop2: "A"},
                { r_1: "t1", prop2: "AA"},
                { r_1: "t2", prop2: "B"},
                { r_1: "t2", prop2: "BB"}
              ];
  await col1.insertMany(col1Records, options);

  const col2Records = [
                { r_1: "t1", prop3: "A"},
                { r_1: "t1", prop3: "AA"},
                { r_1: "t2", prop3: "B"},
                { r_1: "t2", prop3: "BB"}
              ];
  await col2.insertMany(col2Records, options);
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

    testData.forEach(testCase => {
      test(`should return expected documents based on aggeregation pipeline: ${testCase.testDescription}`, async () => {
        //GIVEN
        const expectedRecords = testCase.expectedResults;
   
         // WHEN
         var result = await arraysCollection.aggregate(testCase.pipeline).toArray();
   
         // THEN
         console.log('result: ' + result);
         console.log(result);
         // result = result.map(function (doc) { return JSON.stringify({ t_id: doc.t_id, initialsFD: doc.initialsFD }) });
         console.log('current documents: ' + JSON.stringify(result));
         console.log('expected documents: ' + JSON.stringify(expectedRecords));
         // expect(result.every(elem => expectedTeams.includes(elem))).toBeTruthy();
         expect(expectedRecords).toEqual(result);
       });
    });

    testData.forEach(testCase => {
      if (testCase.expectedQueryPlanIndexes) {
      test(`should return expected explain plan definition based on aggeregation pipeline: ${testCase.testDescription}`, async () => {
        //GIVEN
        const expectedRecords = testCase.expectedResults;
   
         // WHEN
         const result = await arraysCollection.aggregate(testCase.pipeline).explain("executionStats");
   
         // THEN
         console.log('result: ' + result);
         console.log(result);
         // result = result.map(function (doc) { return JSON.stringify({ t_id: doc.t_id, initialsFD: doc.initialsFD }) });
         console.log('current documents: ' + JSON.stringify(result));
         console.log('expected documents: ' + JSON.stringify(expectedRecords));
         // expect(result.every(elem => expectedTeams.includes(elem))).toBeTruthy();
        //  expect(expectedRecords).toEqual(result);
        testCase.expectedQueryPlanIndexes.forEach(expectedJsonValues => {
          const jsonValues = jp.query(result, expectedJsonValues.jsonPath);
          expect(jsonValues).toEqual(expectedJsonValues.expectedValues);
        });
       });
      }
    });
    

});