Prepare java tests class that demonstrate below behaviour of the should operator.

Name it ShouldOperatorTest.
Make it similar to MoviesSearchTest.
Use similar constructions for tests below there are three behavious for the should operator.
Add more tests data (bson files) if you need.
Create separate search index if you need.

Here’s an example of an Atlas Search query containing the compound operator and a should clause. There is more than one condition in the array, and minimumShouldMatch is implicitly set to 1 because it’s not specified.

db.movies.aggregate([
{
"$search": {
"compound": {
"should": [
{"text":{ "query":"poet", "path":"plot" }},
{"text":{ "query":"Elizabeth", "path":"plot" }},
],
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
Here’s the same query, but with minimumShouldMatch explicitly set to 2.

db.movies.aggregate([
{
"$search": {
"compound": {
"should": [
{"text":{ "query":"poet", "path":"plot" }},
{"text":{ "query":"Elizabeth", "path":"plot" }},
],
"minimumShouldMatch": 2
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
Here’s the same query, but with minimumShouldMatch explicitly set to 3. Because there are only two conditions, this will return an error.

db.movies.aggregate([
{
"$search": {
"compound": {
"should": [
{ "text": {"query": "poet", "path":"plot"} },
{ "text": {"query": "Elizabeth", "path":"plot"} },
],
"minimumShouldMatch": 3
}
}
}
]);
