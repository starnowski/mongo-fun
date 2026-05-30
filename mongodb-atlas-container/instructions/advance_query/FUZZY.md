Prepare java tests class that demonstrate below behaviour of the fuzzy operator.

Name it FuzzyOperatorTest.
Make it similar to MoviesSearchTest.
Use similar constructions for tests below a couple of behavious for the fuzzy operator.
Add more tests data (bson files) if you need.
Create separate search index if you need.

Write a Search Query with Fuzzy Search
The following query uses the fuzzy option and sets maxEdits to 1:

db.movies.aggregate([
{
"$search": {
"text": {
"query": "P0et",
"path": "plot",
"fuzzy": {"maxEdits":1},
},
},
},
{
"$project": {
"_id": 0,
"score": { "$meta": "searchScore" },
"title": "$title",
"plot": "$plot",
},
},
]);


Add also examples that contains tests cases for prefixLength and maxExpansions