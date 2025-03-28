


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
        "$match": {
          "testCase": 1
        }
      }
      ,
      {
        "$set": {
          "ngrams": {
            "$reduce": {
              "input": { "$range": [0, { "$subtract": [{ "$size": "$keywords" }, { "$subtract": ["$ngramT", 1]}] }] },
              "initialValue": [],
              "in": {
                "$concatArrays": [
                  "$$value",
                  [
                    {
                      "$slice": ["$keywords", "$$this", "$ngramT"]
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
    expectedResults: [{r_1: "t1", ngrams: [["ala", "ma", "kota", "oraz"], ["ma", "kota", "oraz", "malego"], ["kota", "oraz", "malego", "psa"]]},
    {r_1: "t2", ngrams: [["E", "F"], ["F", "G"], ["G", "H"]]},
    {r_1: "t3", ngrams: [["I", "J", "K"], ["J", "K", "L"]]},
    {r_1: "t4", ngrams: [["M"], ["N"]]}
  ],
    testDescription: "pipeline that returns correct ngrams"
  }
  ,
  {
    pipeline: [
      {
        "$match": {
          "testCase": 2
        }
      }
      ,
      // https://www.mongodb.com/docs/v5.0/reference/operator/aggregation/first-array-element/
      {
        "$set": {
          "firstNgram": {
            "$first": {
              "$setIntersection": [ [
                  ["ma", "kota", "oraz", "malego"],
                  ["kota", "oraz", "malego", "zolwia"],
                  ["oraz", "malego", "zolwia", "ktory"],
                  ["malego", "zolwia", "ktory", "jest"],
                  ["zolwia", "ktory", "jest", "zielony"]
                ]
                , "$keywords"
              ]
            }
          }
        }
      }
      ,
      //https://www.mongodb.com/docs/manual/reference/operator/aggregation/indexOfArray/#mongodb-expression-exp.-indexOfArray
      {
        "$set": {
          "searchOffest": {
            "$indexOfArray": [
                [
                  ["ma", "kota", "oraz", "malego"],
                  ["kota", "oraz", "malego", "zolwia"],
                  ["oraz", "malego", "zolwia", "ktory"],
                  ["malego", "zolwia", "ktory", "jest"],
                  ["zolwia", "ktory", "jest", "zielony"]
                ]
                ,
                "$firstNgram"
              ]
          }
          ,
          "dataOffset": {
            "$indexOfArray": [
                "$keywords"
                ,
                "$firstNgram"
              ]
          }

        }
      }
      ,
      { 
        $project: {
          _id: 0,
          r_1: 1,
          searchOffest: 1,
          dataOffset: 1
        }
      }
    ]
    ,
    expectedResults: [{r_1: "t11", searchOffest: 0, dataOffset: 1},
    {r_1: "t12", searchOffest: 1, dataOffset: 2},
    {r_1: "t13", searchOffest: 4, dataOffset: 0}
  ],
    testDescription: "pipeline that returns correct ngrams"
  }
  ,
  // final version (without lookup)
  {
    pipeline: [
      {
        "$match": {
          "testCase": 3
          //TODO uncomment this fragment if you want to search documents based on below criteria
          ,
          "keywords": {"$in": [	
            "ma",
            "kota",
            "oraz",
            "malego",
            "zolwia",
            "ktory",
            "jest",
            "zielony"]
          }
        }
      }
      ,
      // keywordCount
      {
        "$set": {
          "keywordCount": {
            "$size": {
              "$setIntersection": [ 
                [	
                "ma",
                "kota",
                "oraz",
                "malego",
                "zolwia",
                "ktory",
                "jest",
                "zielony"
                ]
                , "$keywords"
              ]
            }
          }
        }
      }
      ,
      //firstMaxNgram
      // https://www.mongodb.com/docs/v5.0/reference/operator/aggregation/first-array-element/
      {
        "$set": {
          "intersection": {
                "$setIntersection": [ 
                  "$keywords",
                  [
                    "ma kota oraz malego",
                    "kota oraz malego zolwia",
                    "oraz malego zolwia ktory",
                    "malego zolwia ktory jest",
                    "zolwia ktory jest zielony",
                  
                    "ma kota oraz",
                    "kota oraz malego",
                    "oraz malego zolwia",
                    "malego zolwia ktory",
                    "zolwia ktory jest",
                    "ktory jest zielony",
                  
                    "ma kota",
                    "kota oraz",
                    "oraz malego",
                    "malego zolwia",
                    "zolwia ktory",
                    "ktory jest",
                    "jest zielony",
                  
                    "ma",
                    "kota",
                    "oraz",
                    "malego",
                    "zolwia",
                    "ktory",
                    "jest",
                    "zielony"
                  ]
                ]
          }
        }
      }
      ,
      {
        "$set": {
          "firstMaxNgram": {
            "$switch": {
              "branches": [
                    // 4 -  ngram
                    { 
                      "case": { 
                        "$gt": [{ "$size": { "$setIntersection": ["$intersection", [
                          "ma kota oraz malego",
                          "kota oraz malego zolwia",
                          "oraz malego zolwia ktory",
                          "malego zolwia ktory jest",
                          "zolwia ktory jest zielony"]] } }, 0]
                      }, 
                      "then": {
                        "keyword": {
                          "$first": {
                                  "$filter": {
                                    "input": [
                                      "ma kota oraz malego",
                                      "kota oraz malego zolwia",
                                      "oraz malego zolwia ktory",
                                      "malego zolwia ktory jest",
                                      "zolwia ktory jest zielony"],
                                    "as": "item",
                                    "cond": { "$in": ["$$item", "$intersection"] }
                                  }
                            }
                        },
                        "level": 4
                      }
                  }
                  ,
                  // 3 - Ngram
                  { 
                    "case": { 
                      "$gt": [{ "$size": { "$setIntersection": ["$intersection", [
                        "ma kota oraz",
                        "kota oraz malego",
                        "oraz malego zolwia",
                        "malego zolwia ktory",
                        "zolwia ktory jest",
                        "ktory jest zielony"]] } }, 0]
                    }
                    , 
                    "then": {
                      "keyword": {
                        "$first": {
                                "$filter": {
                                  "input": [
                                    "ma kota oraz",
                                    "kota oraz malego",
                                    "oraz malego zolwia",
                                    "malego zolwia ktory",
                                    "zolwia ktory jest",
                                    "ktory jest zielony"],
                                  "as": "item",
                                  "cond": { "$in": ["$$item", "$intersection"] }
                                }
                        }
                      }
                      ,
                      "level": 3
                    }
                  }
                ,
                // 2 - Ngram
                { 
                  "case": { 
                    "$gt": [{ "$size": { "$setIntersection": ["$intersection", [
                      "ma kota",
                      "kota oraz",
                      "oraz malego",
                      "malego zolwia",
                      "zolwia ktory",
                      "ktory jest",
                      "jest zielony"]] } }, 0]
                  }
                  , 
                  "then": {
                    "keyword": {
                      "$first": {
                              "$filter": {
                                "input": [
                                  "ma kota",
                                  "kota oraz",
                                  "oraz malego",
                                  "malego zolwia",
                                  "zolwia ktory",
                                  "ktory jest",
                                  "jest zielony"],
                                "as": "item",
                                "cond": { "$in": ["$$item", "$intersection"] }
                              }
                      }
                    }
                    ,
                    "level": 2
                  }
                }
                ,
                // 1 - Ngram
                { 
                  "case": { 
                    "$gt": [{ "$size": { "$setIntersection": ["$intersection", [
                      "ma",
                      "kota",
                      "oraz",
                      "malego",
                      "zolwia",
                      "ktory",
                      "jest",
                      "zielony"]] } }, 0]
                  }
                  , 
                  "then": {
                    "keyword": {
                      "$first": {
                              "$filter": {
                                "input": [
                                  "ma",
                                  "kota",
                                  "oraz",
                                  "malego",
                                  "zolwia",
                                  "ktory",
                                  "jest",
                                  "zielony"],
                                "as": "item",
                                "cond": { "$in": ["$$item", "$intersection"] }
                              }
                      }
                    }
                    ,
                    "level": 1
                  }
                }
              ],
              "default": {"level": 0, "keyword": null}
            }
          }
        }
      }
      ,
      {
        "$set": {
          "keywords": {
              "$switch": {
              "branches": [
                {
                  "case": { "$eq": ["$firstMaxNgram.level", 4] },
                  "then": {
                    "$filter": {
                      "input": "$keywords",
                      "as": "item",
                      "cond": { $regexMatch: { input: "$$item", regex: "^(\\S+\\s){3}\\S+$" } }
                    }
                  }
                }
                ,
                {
                  "case": { "$eq": ["$firstMaxNgram.level", 3] },
                  "then": {
                    "$filter": {
                      "input": "$keywords",
                      "as": "item",
                      "cond": { $regexMatch: { input: "$$item", regex: "^(\\S+\\s){2}\\S+$" } }
                    }
                  }
                }
                ,
                {
                  "case": { "$eq": ["$firstMaxNgram.level", 2] },
                  "then": {
                    "$filter": {
                      "input": "$keywords",
                      "as": "item",
                      "cond": { $regexMatch: { input: "$$item", regex: "^(\\S+\\s){1}\\S+$" } }
                    }
                  }
                }
                ,
                {
                  "case": { "$eq": ["$firstMaxNgram.level", 1] },
                  "then": {
                    "$filter": {
                      "input": "$keywords",
                      "as": "item",
                      "cond": { $regexMatch: { input: "$$item", regex: "^\\S+$" } }
                    }
                  }
                }
              ],
              "default": null
            }
          }
        }
      }
      ,
      // https://www.mongodb.com/docs/manual/reference/operator/aggregation/indexOfArray/#mongodb-expression-exp.-indexOfArray
      {
        "$set": {
          "searchOffest": {
            "$switch": {
              "branches": [
                {
                  "case": { "$eq": ["$firstMaxNgram.level", 4] },
                  "then": {
                    "$indexOfArray": [
                      [
                        "ma kota oraz malego",
                        "kota oraz malego zolwia",
                        "oraz malego zolwia ktory",
                        "malego zolwia ktory jest",
                        "zolwia ktory jest zielony"
                      ]
                      ,
                      "$firstMaxNgram.keyword"
                    ]
                  }
                }
                ,
                {
                  "case": { "$eq": ["$firstMaxNgram.level", 3] },
                  "then": {
                    "$indexOfArray": [
                      [
                        "ma kota oraz",
                        "kota oraz malego",
                        "oraz malego zolwia",
                        "malego zolwia ktory",
                        "zolwia ktory jest",
                        "ktory jest zielony"
                      ]
                      ,
                      "$firstMaxNgram.keyword"
                    ]
                  }
                }
                ,
                {
                  "case": { "$eq": ["$firstMaxNgram.level", 2] },
                  "then": {
                    "$indexOfArray": [
                      [
                        "ma kota",
                        "kota oraz",
                        "oraz malego",
                        "malego zolwia",
                        "zolwia ktory",
                        "ktory jest",
                        "jest zielony"
                      ]
                      ,
                      "$firstMaxNgram.keyword"
                    ]
                  }
                }
                ,
                {
                  "case": { "$eq": ["$firstMaxNgram.level", 1] },
                  "then": {
                    "$indexOfArray": [
                      [
                        "ma",
                        "kota",
                        "oraz",
                        "malego",
                        "zolwia",
                        "ktory",
                        "jest",
                        "zielony"
                      ]
                      ,
                      "$firstMaxNgram.keyword"
                    ]
                  }
                }
              ],
              "default": null
            }
          }
          ,
          "dataOffset": {
            "$switch": {
              "branches": [
                  {
                    "case": {"$gt": ["$firstMaxNgram.level", 0]},
                    "then": {
                      "$indexOfArray": [
                        "$keywords"
                        ,
                        "$firstMaxNgram.keyword"
                      ]
                    }
                  }
              ],
              "default": null
            }
          }

        }
      }
      ,
      { 
        $project: {
          _id: 0,
          r_1: 1,
          searchOffest: 1,
          dataOffset: 1,
          "firstMaxNgram.level": 1,
          keywordCount: 1
        }
      }
    ]
    ,
    expectedResults: [{r_1: "t21", searchOffest: 0, dataOffset: 1, firstMaxNgram: {level: 4}, keywordCount: 4},
    {r_1: "t22", searchOffest: 1, dataOffset: 2, firstMaxNgram: {level: 4}, keywordCount: 4},
    {r_1: "t23", searchOffest: 4, dataOffset: 0, firstMaxNgram: {level: 4}, keywordCount: 4},
    {r_1: "t24", searchOffest: 5, dataOffset: 1, firstMaxNgram: {level: 3}, keywordCount: 3},
    {r_1: "t25", searchOffest: 6, dataOffset: 2, firstMaxNgram: {level: 1}, keywordCount: 1}
  ],
    testDescription: "pipeline that returns correct max ngram, keyword count and offset values"
  }

  ,

  {
    pipeline: [
      {
        "$match": {
          "testCase": 3
        }
      }
      ,
      // keywordCount
      {
        "$set": {
          "keywordCount": {
            "$size": {
              "$setIntersection": [ 
                [	
                "ma",
                "kota",
                "oraz",
                "malego",
                "zolwia",
                "ktory",
                "jest",
                "zielony"
                ]
                , "$keywords"
              ]
            }
          }
        }
      }
      ,
      //firstMaxNgram
      // https://www.mongodb.com/docs/v5.0/reference/operator/aggregation/first-array-element/
      {
        "$set": {
          "intersection": {
                "$setIntersection": [ 
                  "$keywords",
                  [
                    "ma kota oraz malego",
                    "kota oraz malego zolwia",
                    "oraz malego zolwia ktory",
                    "malego zolwia ktory jest",
                    "zolwia ktory jest zielony",
                  
                    "ma kota oraz",
                    "kota oraz malego",
                    "oraz malego zolwia",
                    "malego zolwia ktory",
                    "zolwia ktory jest",
                    "ktory jest zielony",
                  
                    "ma kota",
                    "kota oraz",
                    "oraz malego",
                    "malego zolwia",
                    "zolwia ktory",
                    "ktory jest",
                    "jest zielony",
                  
                    "ma",
                    "kota",
                    "oraz",
                    "malego",
                    "zolwia",
                    "ktory",
                    "jest",
                    "zielony"
                  ]
                ]
          }
        }
      }
      ,
      {
        "$set": {
          "firstMaxNgram": {
            "$switch": {
              "branches": [
                    // 4 -  ngram
                    { 
                      "case": { 
                        "$gt": [{ "$size": { "$setIntersection": ["$intersection", [
                          "ma kota oraz malego",
                          "kota oraz malego zolwia",
                          "oraz malego zolwia ktory",
                          "malego zolwia ktory jest",
                          "zolwia ktory jest zielony"]] } }, 0]
                      }, 
                      "then": {
                        "keyword": {
                          "$first": {
                                  "$filter": {
                                    "input": [
                                      "ma kota oraz malego",
                                      "kota oraz malego zolwia",
                                      "oraz malego zolwia ktory",
                                      "malego zolwia ktory jest",
                                      "zolwia ktory jest zielony"],
                                    "as": "item",
                                    "cond": { "$in": ["$$item", "$intersection"] }
                                  }
                            }
                        },
                        "level": 4
                      }
                  }
                  ,
                  // 3 - Ngram
                  { 
                    "case": { 
                      "$gt": [{ "$size": { "$setIntersection": ["$intersection", [
                        "ma kota oraz",
                        "kota oraz malego",
                        "oraz malego zolwia",
                        "malego zolwia ktory",
                        "zolwia ktory jest",
                        "ktory jest zielony"]] } }, 0]
                    }
                    , 
                    "then": {
                      "keyword": {
                        "$first": {
                                "$filter": {
                                  "input": [
                                    "ma kota oraz",
                                    "kota oraz malego",
                                    "oraz malego zolwia",
                                    "malego zolwia ktory",
                                    "zolwia ktory jest",
                                    "ktory jest zielony"],
                                  "as": "item",
                                  "cond": { "$in": ["$$item", "$intersection"] }
                                }
                        }
                      }
                      ,
                      "level": 3
                    }
                  }
                ,
                // 2 - Ngram
                { 
                  "case": { 
                    "$gt": [{ "$size": { "$setIntersection": ["$intersection", [
                      "ma kota",
                      "kota oraz",
                      "oraz malego",
                      "malego zolwia",
                      "zolwia ktory",
                      "ktory jest",
                      "jest zielony"]] } }, 0]
                  }
                  , 
                  "then": {
                    "keyword": {
                      "$first": {
                              "$filter": {
                                "input": [
                                  "ma kota",
                                  "kota oraz",
                                  "oraz malego",
                                  "malego zolwia",
                                  "zolwia ktory",
                                  "ktory jest",
                                  "jest zielony"],
                                "as": "item",
                                "cond": { "$in": ["$$item", "$intersection"] }
                              }
                      }
                    }
                    ,
                    "level": 2
                  }
                }
                ,
                // 1 - Ngram
                { 
                  "case": { 
                    "$gt": [{ "$size": { "$setIntersection": ["$intersection", [
                      "ma",
                      "kota",
                      "oraz",
                      "malego",
                      "zolwia",
                      "ktory",
                      "jest",
                      "zielony"]] } }, 0]
                  }
                  , 
                  "then": {
                    "keyword": {
                      "$first": {
                              "$filter": {
                                "input": [
                                  "ma",
                                  "kota",
                                  "oraz",
                                  "malego",
                                  "zolwia",
                                  "ktory",
                                  "jest",
                                  "zielony"],
                                "as": "item",
                                "cond": { "$in": ["$$item", "$intersection"] }
                              }
                      }
                    }
                    ,
                    "level": 1
                  }
                }
              ],
              "default": {"level": 0, "keyword": null}
            }
          }
        }
      }
      ,
      {
        "$set": {
          "keywords": {
              "$switch": {
              "branches": [
                {
                  "case": { "$eq": ["$firstMaxNgram.level", 4] },
                  "then": {
                    "$filter": {
                      "input": "$keywords",
                      "as": "item",
                      "cond": { $regexMatch: { input: "$$item", regex: "^(\\S+\\s){3}\\S+$" } }
                    }
                  }
                }
                ,
                {
                  "case": { "$eq": ["$firstMaxNgram.level", 3] },
                  "then": {
                    "$filter": {
                      "input": "$keywords",
                      "as": "item",
                      "cond": { $regexMatch: { input: "$$item", regex: "^(\\S+\\s){2}\\S+$" } }
                    }
                  }
                }
                ,
                {
                  "case": { "$eq": ["$firstMaxNgram.level", 2] },
                  "then": {
                    "$filter": {
                      "input": "$keywords",
                      "as": "item",
                      "cond": { $regexMatch: { input: "$$item", regex: "^(\\S+\\s){1}\\S+$" } }
                    }
                  }
                }
                ,
                {
                  "case": { "$eq": ["$firstMaxNgram.level", 1] },
                  "then": {
                    "$filter": {
                      "input": "$keywords",
                      "as": "item",
                      "cond": { $regexMatch: { input: "$$item", regex: "^\\S+$" } }
                    }
                  }
                }
              ],
              "default": null
            }
          }
        }
      }
      ,
      // https://www.mongodb.com/docs/manual/reference/operator/aggregation/indexOfArray/#mongodb-expression-exp.-indexOfArray
      {
        "$set": {
          "searchOffest": {
            "$switch": {
              "branches": [
                {
                  "case": { "$eq": ["$firstMaxNgram.level", 4] },
                  "then": {
                    "$indexOfArray": [
                      [
                        "ma kota oraz malego",
                        "kota oraz malego zolwia",
                        "oraz malego zolwia ktory",
                        "malego zolwia ktory jest",
                        "zolwia ktory jest zielony"
                      ]
                      ,
                      "$firstMaxNgram.keyword"
                    ]
                  }
                }
                ,
                {
                  "case": { "$eq": ["$firstMaxNgram.level", 3] },
                  "then": {
                    "$indexOfArray": [
                      [
                        "ma kota oraz",
                        "kota oraz malego",
                        "oraz malego zolwia",
                        "malego zolwia ktory",
                        "zolwia ktory jest",
                        "ktory jest zielony"
                      ]
                      ,
                      "$firstMaxNgram.keyword"
                    ]
                  }
                }
                ,
                {
                  "case": { "$eq": ["$firstMaxNgram.level", 2] },
                  "then": {
                    "$indexOfArray": [
                      [
                        "ma kota",
                        "kota oraz",
                        "oraz malego",
                        "malego zolwia",
                        "zolwia ktory",
                        "ktory jest",
                        "jest zielony"
                      ]
                      ,
                      "$firstMaxNgram.keyword"
                    ]
                  }
                }
                ,
                {
                  "case": { "$eq": ["$firstMaxNgram.level", 1] },
                  "then": {
                    "$indexOfArray": [
                      [
                        "ma",
                        "kota",
                        "oraz",
                        "malego",
                        "zolwia",
                        "ktory",
                        "jest",
                        "zielony"
                      ]
                      ,
                      "$firstMaxNgram.keyword"
                    ]
                  }
                }
              ],
              "default": null
            }
          }
          ,
          "dataOffset": {
            "$switch": {
              "branches": [
                  {
                    "case": {"$gt": ["$firstMaxNgram.level", 0]},
                    "then": {
                      "$indexOfArray": [
                        "$keywords"
                        ,
                        "$firstMaxNgram.keyword"
                      ]
                    }
                  }
              ],
              "default": null
            }
          }

        }
      }
      ,
      { 
        "$project": {
          _id: 0,
          r_1: 1,
          searchOffest: 1,
          dataOffset: 1,
          "firstMaxNgram.level": 1,
          keywordCount: 1
        }
      }
      ,
      {
        "$set": {
            "rank": {
              "$switch": {
                "branches": [
                  {
                    "case": { "$gt": ["$firstMaxNgram.level", 0]},
                    "then": {
                      "$add": [
                          {
                            "$multiply": ["$firstMaxNgram.level", 100]
                          }
                          ,
                          {
                            "$multiply": ["$keywordCount", 10]
                          }
                          ,
                          {
                            "$multiply": ["$searchOffest", -10]
                          }
                          ,
                          {
                            "$multiply": ["$dataOffset", -1]
                          }
                        ]
                    }
                  }
                ],
                "default": 0
              }
            }
        }
      }
    ]
    ,
    expectedResults: [{r_1: "t21", searchOffest: 0, dataOffset: 1, firstMaxNgram: {level: 4}, keywordCount: 4, rank: 439},
    {r_1: "t22", searchOffest: 1, dataOffset: 2, firstMaxNgram: {level: 4}, keywordCount: 4, rank: 428},
    {r_1: "t23", searchOffest: 4, dataOffset: 0, firstMaxNgram: {level: 4}, keywordCount: 4, rank: 400},
    {r_1: "t24", searchOffest: 5, dataOffset: 1, firstMaxNgram: {level: 3}, keywordCount: 3, rank: 279},
    {r_1: "t25", searchOffest: 6, dataOffset: 2, firstMaxNgram: {level: 1}, keywordCount: 1, rank: 48},
    {r_1: "t26", searchOffest: null, dataOffset: null, firstMaxNgram: {level: 0}, keywordCount: 0, rank: 0}
  ],
    testDescription: "pipeline that returns correct max ngram, keyword count and offset values and rank"
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
                { r_1: "t1", testCase: 1, keywords: ["ala", "ma", "kota", "oraz", "malego", "psa"], ngramT: 4 },
                { r_1: "t2", testCase: 1, keywords: ["E", "F", "G", "H"], ngramT: 2 },
                { r_1: "t3", testCase: 1, keywords: ["I", "J", "K", "L"], ngramT: 3 },
                { r_1: "t4", testCase: 1, keywords: ["M", "N"], ngramT: 1 },

                { r_1: "t11", testCase: 2, 
                  keywords: [
                    ["ala", "ma", "kota", "oraz"],
                    ["ma", "kota", "oraz", "malego"],
                    ["kota", "oraz", "malego", "psa"]
                  ] // search - 0, dataset - 1
                },
                { r_1: "t12", testCase: 2, 
                  keywords: [
                    ["bob", "mial", "kota", "oraz"],
                    ["mial", "kota", "oraz", "malego"],
                    ["kota", "oraz", "malego", "zolwia"]
                  ]//search - 1, dataset - 2
                },
                { r_1: "t13", testCase: 2, keywords: [["zolwia", "ktory", "jest", "zielony"]] }//search - 4, dataset - 0
                ,
                { r_1: "t21", testCase: 3, 
                  keywords: [
                    "ala ma kota oraz",
                    "ma kota oraz malego",
                    "kota oraz malego psa",
                    "ala ma kota",
                    "ma kota oraz",
                    "kota oraz malego",
                    "oraz malego psa",
                    "ala ma",
                    "ma kota",
                    "kota oraz",
                    "oraz malego",
                    "malego psa",
                    "ala",
                    "ma",
                    "kota",
                    "oraz",
                    "malego",
                    "psa"
					
                  ]
                },
                { r_1: "t22", testCase: 3, 
                  keywords: [
                    "bob mial kota oraz",
                    "mial kota oraz malego",
                    "kota oraz malego zolwia",
                    "bob mial kota",
                    "mial kota oraz",
                    "kota oraz malego",
                    "oraz malego zolwia",
                    "bob mial",
                    "mial kota",
                    "kota oraz",
                    "oraz malego",
                    "malego zolwia",
                    "bob",
                    "mial",
                    "kota",
                    "oraz",
                    "malego",
                    "zolwia"
                  ]
                },
                { r_1: "t23", testCase: 3, 
                  keywords: [
                    "zolwia ktory jest zielony",
                    "zolwia ktory jest",
                    "ktory jest zielony",
                    "zolwia ktory",
                    "ktory jest",
                    "jest zielony",
                    "zolwia",
                    "ktory",
                    "jest",
                    "zielony"
                  ] 
                },
                { r_1: "t24", testCase: 3, 
                  keywords: [
                    "pajaka ktory jest zielony",
                    "pajaka ktory jest",
                    "ktory jest zielony",
                    "pajaka ktory",
                    "ktory jest",
                    "jest zielony",
                    "pajaka",
                    "ktory",
                    "jest",
                    "zielony"
                  ] 
                },
                { r_1: "t25", testCase: 3, 
                  keywords: [
                    "pajaka co jest zolty",
                    "pajaka co jest",
                    "co jest zolty",
                    "pajaka co",
                    "co jest",
                    "jest zolty",
                    "pajaka",
                    "co",
                    "jest",
                    "zolty"
                  ] 
                },
                { r_1: "t26", testCase: 3, 
                  keywords: [
                    "pajaka co byl zolty",
                    "pajaka co byl",
                    "co byl zolty",
                    "pajaka co",
                    "co byl",
                    "byl zolty",
                    "pajaka",
                    "co",
                    "byl",
                    "zolty"
                  ] 
                }
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