


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


const getNamesInitials = function (names){
    // console.log("Executing for names: [" + names + "]");
    var result = "";
    names.forEach(function(value, index) {
        // console.log('Name:', value);
        result += value.charAt(0);
      });
    return result;
}


describe("Mongo operations on properties with dot character", () => {
  beforeEach(async () => {
    console.log("Running tests on mongodb : " + mongoUrl);
    arraysCollection = db.collection('arraysCollection');
    const query = { _id: {$exists: true} };
    await arraysCollection.deleteMany(query);
  });

     test("should update specific documents with field that has dot", async () => {
      //GIVEN
      // Add data
      // this option prevents additional documents from being inserted if one fails
      const options = { ordered: true };
      const developersTeam = [
               { t_id: "t1", top: { "nested.prop1": { "prop1": 1, prop2: 2 }, "prop_level2": { prop3: 13} } }
             ];
      await arraysCollection.insertMany(developersTeam, options);
      const expectedRecords = [{ t_id: "t1", top: { "nested.prop1": { "prop1": 47, prop2: 2 }, "prop_level2": { prop3: 13} } }];
 
       // WHEN
       await arraysCollection.updateMany({}, 
                                                      [{
                                                         $set: {
                                                             "tmpField": {
                                                              $getField: {
                                                                field: "nested.prop1",
                                                                input: "$$CURRENT.top"
                                                              }
                                                             }
                                                         }
                                                      },
                                                      {
                                                        $set: {
                                                            "tmpField.prop1": 47
                                                            }
                                                        }
                                                        ,
                                                        { $set : {
                                                          "top": {
                                                              $setField: {
                                                                  field: "nested.prop1",
                                                                  input: "$$CURRENT.top",
                                                                  value: "$tmpField"
                                                                  }
                                                          }
                                                        }
                                                      },
                                                        {
                                                          $unset: "tmpField"
                                                        }
                                                       ]);
 
       // THEN
       var result = await arraysCollection.aggregate([
          {
              $project: { _id: 0}
          }
            ]).toArray();
       console.log('result: ' + result);
       console.log(JSON.stringify(result));
      console.log('current documents: ' + JSON.stringify(result));
      console.log('expected documents: ' + JSON.stringify(expectedRecords));
      expect(expectedRecords).toEqual(result);
     });

    test("should update specific documents with field that has dot and also do query based on nested property", async () => {
      //GIVEN
      // Add data
      // this option prevents additional documents from being inserted if one fails
      const options = { ordered: true };
      const developersTeam = [
        { t_id: "t1", top: { "nested.prop1": { "prop1": 1, prop2: 2 }, "prop_level2": { prop3: 13} } },
        { t_id: "t2", top: { "nested.prop1": { "prop1": 100, prop2: 276 }, "prop_level2": { prop3: 13} } }
             ];
      await arraysCollection.insertMany(developersTeam, options);
      const expectedRecords = [
        { t_id: "t1", top: { "nested.prop1": { "prop1": 47, prop2: 2 }, "prop_level2": { prop3: 13} } },
        { t_id: "t2", top: { "nested.prop1": { "prop1": 100, prop2: 276 }, "prop_level2": { prop3: 13} } }
      ];
 
       // WHEN
       await arraysCollection.aggregate(
                                                      [
                                                        {
                                                          $match: {
                                                            $eq: [
                                                              {
                                                                $getField: {
                                                                  field: "prop1",
                                                                  input: {
                                                                    $getField: {
                                                                      field: "nested.prop1",
                                                                      input: "$$CURRENT.top"
                                                                    }
                                                                  }
                                                                }
                                                              },
                                                              1
                                                            ]
                                                          }
                                                        },
                                                        {
                                                         $set: {
                                                             "tmpField": {
                                                              $getField: {
                                                                field: "nested.prop1",
                                                                input: "$$CURRENT.top"
                                                              }
                                                             }
                                                         }
                                                      },
                                                      {
                                                        $set: {
                                                            "tmpField.prop1": 47
                                                            }
                                                        }
                                                        ,
                                                        { $set : {
                                                          "top": {
                                                              $setField: {
                                                                  field: "nested.prop1",
                                                                  input: "$$CURRENT.top",
                                                                  value: "$tmpField"
                                                                  }
                                                          }
                                                        }
                                                      },
                                                        {
                                                          $unset: "tmpField"
                                                        }
                                                        ,
                                                        {
                                                          $merge: {
                                                            into: "arraysCollection",
                                                            on: "_id",
                                                            whenMatched: "replace",
                                                            whenNotMatched: "fail"
                                                          }
                                                        }
                                                       ]);
 
       // THEN
       var result = await arraysCollection.aggregate([
          {
              $project: { _id: 0}
          }
            ]).toArray();
       console.log('result: ' + result);
       console.log(JSON.stringify(result));
      console.log('current documents: ' + JSON.stringify(result));
      console.log('expected documents: ' + JSON.stringify(expectedRecords));
      expect(expectedRecords).toEqual(result);
     });
});