


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


describe("Arrays mongo operations", () => {
  beforeEach(async () => {
    arraysCollection = db.collection('arraysCollection');
    const query = { _id: {$exists: true} };
    await arraysCollection.deleteMany(query);
  });

    test("should return all documents with initila for fronted developers", async () => {
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
     const expectedTeams = [JSON.stringify({t_id: "t1", initialsFD: "JM"}), JSON.stringify({t_id: "t2", initialsFD: "J"}),
                JSON.stringify({t_id: "t3", initialsFD: "SJ"})];

      // WHEN
      var result = await arraysCollection.aggregate([{
                                                        $addFields: {
                                                            initialsFD: {
                                                                $function: {
                                                                    body: getNamesInitials.toString(),
                                                                    args: ["$developers.fronted"],
                                                                    lang: "js"
                                                                }
                                                            }
                                                        }
                                                    }
                                                    ,
                                                    {
                                                        $project: { t_id: 1, initialsFD: 1}
                                                    }
                                                      ]).toArray();

      // THEN
      console.log('result: ' + result);
      console.log(result);
      result = result.map(function (doc) { return JSON.stringify({ t_id: doc.t_id, initialsFD: doc.initialsFD }) });
      console.log('current teams: ' + result);
      console.log('expected teams: ' + expectedTeams);
      expect(result.every(elem => expectedTeams.includes(elem))).toBeTruthy();
      expect(result.length).toEqual(3);
    });

    test("should update all documents with initila for fronted developers", async () => {
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
      const expectedTeams = [JSON.stringify({t_id: "t1", initialsFD: "JM"}), JSON.stringify({t_id: "t2", initialsFD: "J"}),
                 JSON.stringify({t_id: "t3", initialsFD: "SJ"})];
 
       // WHEN
       await arraysCollection.updateMany({}, 
                                                      [{
                                                         $set: {
                                                             initialsFD: {
                                                                 $function: {
                                                                     body: getNamesInitials.toString(),
                                                                     args: ["$developers.fronted"],
                                                                     lang: "js"
                                                                 }
                                                             }
                                                         }
                                                     }
                                                       ]);
 
       // THEN
       var result = await arraysCollection.aggregate([
          {
              $project: { t_id: 1, initialsFD: 1}
          }
            ]).toArray();
       console.log('result: ' + result);
       console.log(result);
       result = result.map(function (doc) { return JSON.stringify({ t_id: doc.t_id, initialsFD: doc.initialsFD }) });
       console.log('current teams: ' + result);
       console.log('expected teams: ' + expectedTeams);
       expect(result.every(elem => expectedTeams.includes(elem))).toBeTruthy();
       expect(result.length).toEqual(3);
     });
});