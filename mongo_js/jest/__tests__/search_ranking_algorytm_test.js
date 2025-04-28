


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
  ,
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
        $project:
          {
            r_1: 1, 
            prop1: 1,
            prop2: 2,
            _id: 0,
            rank:
              {
                $cond: { if: { "$eq" : [ "$prop1", "A"] }, then: 25, else: 0 }
              }
          }
      }
     ,
     {
      $project:
        {
          r_1: 1, 
          rank:
            {
              $cond: { if: { "$eq" : [ "$prop2", 443] }, then: { $add: ["$rank", 50] }, else: "$rank" }
            }
        }
      }
     ,
      {
          $project: { r_1: 1, rank: 1}
      }
    ]
    ,
    expectedResults: [{ r_1: "t1", rank: 25 }, { r_1: "t8", rank: 25 }, {r_1: "t11", rank: 75}],
    testDescription: "pipeline that matches document based on two criteria with rank"
  }
  ,
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
        $project:
          {
            r_1: 1, 
            _id: 0,
            rank:
              {
                $add: [
                  {
                    $cond: { if: { "$eq" : [ "$prop1", "A"] }, then: 25, else: 0 }
                  }
                  ,
                  {
                    $cond: { if: { "$eq" : [ "$prop2", 443] }, then: 50, else: 0 }
                  }
                ]
              }
          }
      }
     ,
      {
          $project: { r_1: 1, rank: 1}
      }
    ]
    ,
    expectedResults: [{ r_1: "t1", rank: 25 }, { r_1: "t8", rank: 25 }, {r_1: "t11", rank: 75}],
    testDescription: "pipeline that matches document based on two criteria with rank with one aggregation stage that calculates rank"
  }
  ,
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
        $project:
          {
            r_1: 1, 
            prop1: 1,
            prop2: 2,
            _id: 0,
            rank:
              {
                $cond: { if: { "$eq" : [ "$prop1", "A"] }, then: 25, else: 0 }
              }
          }
      }
     ,
     {
      $project:
        {
          r_1: 1, 
          rank:
            {
              $cond: { if: { "$eq" : [ "$prop2", 443] }, then: { $add: ["$rank", 50] }, else: "$rank" }
            }
        }
      }
     ,
      {
          $project: { r_1: 1, rank: 1}
      }
      ,
      {
        $facet: {
          items: [
            {
              $sort: {
                rank: -1
              }
            }
            ,
            { $limit : 1 }
          ]
          ,
          maxRank: [
            {
              $group: {
                _id: "$rank",
                count: { $sum: 1 }
             }
            }
            ,
            {
              $sort: {
                _id: -1
              }
            }
            ,
            {
              $project: { count: 1, rank: "$_id", _id: 0 }
            }
            ,
            { $limit : 1 }
          ]
        }
      }
    ]
    ,
    expectedResults: [{ items:[{r_1: "t11", rank: 75}], maxRank: [{ rank: 75, count: 1}] }],
    testDescription: "pipeline that matches document based on two criteria with rank and returns document with highest rank"
  }
  ,
  {
    pipeline: [{
          $match: {
            $or: [
              { prop1 : { $eq: "A"} }
            ]
          }
      }
      ,
      {
        $project:
          {
            r_1: 1, 
            prop1: 1,
            prop2: 2,
            _id: 0,
            rank:
              {
                $cond: { if: { "$eq" : [ "$prop1", "A"] }, then: 25, else: 0 }
              }
            ,
            optionalRank:
            {
              $cond: { if: { "$eq" : [ "$prop2", 443] }, then: 25, else: 0 }
            }
          }
      }
      ,
      {
          $project: { r_1: 1, rank: 1, optionalRank: 1}
      }
      ,
      {
        $facet: {
          items: [
            {
              $sort: {
                rank: -1,
                optionalRank: -1
              }
            }
            ,
            { $limit : 1 }
          ]
          ,
          maxRank: [
            {
              $group: {
                _id: { 
                  rank: "$rank", 
                  optionalRank: "$optionalRank"
                },
                count: { $sum: 1 }
             }
            }
            ,
            {
              $sort: {
                "_id.rank": -1,
                "_id.optionalRank": -1
              }
            }
            ,
            {
              $project: { count: 1}
            }
            ,
            { $limit : 1 }
          ]
        }
      }
    ]
    ,
    expectedResults: [{ items:[{r_1: "t11", rank: 25, optionalRank: 25}], maxRank: [{ _id: { rank: 25, optionalRank: 25}, count: 1}] }],
    testDescription: "pipeline that matches document based on one criteria with rank and sort by optional criteria and returns document with highest rank"
  }
  ,
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
        $project:
          {
            r_1: 1, 
            prop1: 1,
            prop2: 2,
            _id: 0,
            rank:
              {
                $cond: { if: { "$eq" : [ "$prop1", "A"] }, then: 25, else: 0 }
              }
          }
      }
     ,
     {
      $project:
        {
          r_1: 1, 
          rank:
            {
              $cond: { if: { "$eq" : [ "$prop2", 443] }, then: { $add: ["$rank", 50] }, else: "$rank" }
            }
        }
      }
     ,
      {
          $project: { r_1: 1, rank: 1}
      }
      ,
      {
        $facet: {
          items: [
            {
              $sort: {
                rank: 1
              }
            }
            ,
            { $limit : 1 }
          ]
          ,
          maxRank: [
            {
              $group: {
                _id: "$rank",
                count: { $sum: 1 }
             }
            }
            ,
            {
              $sort: {
                _id: 1
              }
            }
            ,
            {
              $project: { count: 1, rank: "$_id", _id: 0 }
            }
            ,
            { $limit : 1 }
          ]
        }
      }
    ]
    ,
    expectedResults: [{ items:[{ r_1: "t1", rank: 25 }], maxRank: [{ rank: 25, count: 2}] }],
    testDescription: "pipeline that matches document based on two criteria with rank and returns document with lowests rank"
  }
  ,
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
        $replaceWith: {
        $setField: {
          field: "rank",
          input: "$$ROOT",
          value: {
            $cond: { if: { "$eq" : [ "$prop1", "A"] }, then: 25, else: 0 }
          }
        }
        }
      }
      ,
      { 
        $replaceWith: {
          $setField: {
            field: "rank",
            input: "$$ROOT",
            value: {
              $cond: { if: { "$eq" : [ "$prop2", 443] }, then: { $add: ["$rank", 50] }, else: "$rank" }
            }
          }
        }
      }
     ,
      {
          $project: { r_1: 1, rank: 1, _id: 0}
      }
      ,
      {
        $facet: {
          items: [
            {
              $sort: {
                rank: 1
              }
            }
            ,
            { $limit : 1 }
          ]
          ,
          maxRank: [
            {
              $group: {
                _id: "$rank",
                count: { $sum: 1 }
             }
            }
            ,
            {
              $sort: {
                _id: 1
              }
            }
            ,
            {
              $project: { count: 1, rank: "$_id", _id: 0 }
            }
            ,
            { $limit : 1 }
          ]
        }
      }
    ]
    ,
    expectedResults: [{ items:[{ r_1: "t1", rank: 25 }], maxRank: [{ rank: 25, count: 2}] }],
    testDescription: "pipeline that matches document based on two criteria with rank and returns document with lowests rank, using setField operator",
    expectedQueryPlanIndexes: [
      {
        jsonPath: "$.stages[0].*.queryPlanner.winningPlan..*.indexName",
        expectedValues: ['prop1_1', 'prop2_1']
      }
    ]
  },
  {
    pipeline: [
      {
          $match: {
            "singleKeyWords": {
              "$in": ["A", "B", "C", "D","E", "F", "G", "R", "S"]
            }
          }
      }
      ,
      { 
        $project: {
          _id: 0,
          r_1: 1,
          rank: { 
            $size: {
              $setIntersection: [ ["A", "B", "C", "D","E", "F", "G", "R", "S"], "$singleKeyWords"]
            } 
          }
        }
      }
      ,
      {
        $sort: {
          rank: -1
        }
      }
      ,
      { $limit : 3 }
    ]
    ,
    expectedResults: [{r_1: "t1", rank: 4}, {r_1: "t2", rank: 3}, {r_1: "t8", rank: 2}],
    testDescription: "pipeline that matches document based on intersection operator for tables and return object that has at least on element"
  }
  ,
  {
    pipeline: [
      {
          $match: {
            "r_1": "t1"
          }
      }
      ,
      { 
        $project: {
          _id: 0,
          r_1: 1,
          rank: { 
            $indexOfArray: [
              ["XXX", "YYY", "C", "D"],
              {
                $first: {
                  $setIntersection: [ ["XXX", "YYY", "C", "D"], "$singleKeyWords"]
                }
              }
            ]
          }
        }
      }
      ,
      {
        $sort: {
          rank: -1
        }
      }
      ,
      { $limit : 1 }
    ]
    ,
    expectedResults: [{r_1: "t1", rank: 2}],
    testDescription: "pipeline that returns for specific document offest of first keyword from looking phrase that exists in the property array"
  }
  ,
  {
    pipeline: [
      {
          $match: {
            "r_1": "t1"
          }
      }
      ,
      {
        "$set": {
          "ngrams": {
            "$reduce": {
              "input": { "$range": [0, { "$subtract": [{ "$size": "$singleKeyWords" }, 2] }] },
              "initialValue": [],
              "in": {
                "$concatArrays": [
                  "$$value",
                  [
                    {
                      "$slice": ["$singleKeyWords", "$$this", 3]
                    }
                  ]
                ]
              }
            }
          }
        }
      }
      ,
      { 
        $project: {
          _id: 0,
          r_1: 1,
          ngrams: 1
        }
      }
    ]
    ,
    expectedResults: [{r_1: "t1", ngrams: [["A", "B", "C"], ["B", "C", "D"]]}],
    testDescription: "pipeline that returns for specific document 3-ngram array"
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
                { r_1: "t1", prop1: "A", prop2: 12, singleKeyWords: ["A", "B", "C", "D"] },
                { r_1: "t2", prop1: "B", prop2: 13, singleKeyWords: ["E", "F", "G", "H"] },
                { r_1: "t3", prop1: "C", prop2: 14, singleKeyWords: ["I", "J", "K", "L"] },
                { r_1: "t4", prop1: "D", prop2: 15, singleKeyWords: ["M", "N"] },
                { r_1: "t5", prop1: "E", prop2: 16, singleKeyWords: ["O"] },
                { r_1: "t6", prop1: "F", prop2: 17, singleKeyWords: ["P"] },
                { r_1: "t7", prop1: "G", prop2: 18, singleKeyWords: ["Q", "R"] },
                { r_1: "t8", prop1: "A", prop2: 19, singleKeyWords: ["R", "S", "T"] },
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
     const expectedRecords = [{ r_1: "t1" }, { r_1: "t8" }, {r_1: "t11"}];

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
      console.log('current documents: ' + JSON.stringify(result));
      console.log('expected documents: ' + JSON.stringify(expectedRecords));
      // expect(result.every(elem => expectedTeams.includes(elem))).toBeTruthy();
      expect(expectedRecords).toEqual(result);
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