Prepare java tests class that demonstrate below behaviour of the sort operator.

Name it SortOperatorTest.
Make it similar to MoviesSearchTest.
Use similar constructions for tests below a couple of behavious for the sort operator.
Add more tests data (bson files) if you need.
Create separate search index if you need.

Sort by Score in Ascending Order
The following Atlas Search query sorts search results by the score field in ascending order.

db.movies.aggregate([
{
"$search": {
"text": {"query": "poet","path":"plot" },
"sort": { unused:{"$meta":"searchScore","order": 1 }},
},
},
{
"$project": {
"_id": 0,
"score": { $meta: "searchScore" },
"title": "$title",
},
},
]);
Sort by Score in Descending Order Using a TieBreaker Field
The following Atlas Search query sorts results by score in descending order. Results with identical scores are further sorted by the released field, which works like a tiebreaker.

db.movies.aggregate([
{
"$search": {
"text": {"query": "poet","path":"plot" },
"sort": { unused:{"$meta":"searchScore","order": 1 },
"released": 1},
},
},
{
"$project": {
"_id": 0,
"score": { $meta: "searchScore" },
"title": "$title",
"released": "$released",
},
},
]);
Update the Default Index to Include Another Field
The following code updates the default index and indexes the title field as a token type.

db.movies.updateSearchIndex("default", {
"mappings": {
"dynamic": true,
"fields": {
"title": [{ "type": "token" }]
}
}
});
After updating the index, you can use the following command to check the configuration of the index:

let { status, latestDefinition } = db.movies.getSearchIndexes('default')[0]

console.log({Status: status, mappings: latestDefinition})
Sort by a String Field
The following Atlas Search query sorts results by the title field. Setting it to 1 means the results will be sorted in ascending order.

db.movies.aggregate([
{
"$search": {
"text": {"query": "poet","path":"plot" },
"sort": { "title":1},
},
},
{
"$project": {
"_id": 0,
score: { $meta: "searchScore" },
"title": "$title",
},
},
]);