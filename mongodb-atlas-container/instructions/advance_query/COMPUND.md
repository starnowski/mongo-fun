Prepare java tests class that demonstrate below behaviour of the compound operator.

Name it CompoundOperatorTest.
Make it similar to MoviesSearchTest.
Use similar constructions for tests below there are couple functionalities for the compound operator.
Add more tests data (bson files) if you need.
Create separate search index if you need.

Code Summary: Creating Compound Queries with Multiple Clauses
Use the Compound Operator with Multiple Clauses: must and mustNot
The following example is an Atlas Search query that uses the compound operator with the must and mustNot clauses. The query defines constants that are substituted in the final query for better readability.

const plot_words = [
{
"text": {
"query": "poet",
"path": "plot"
}
},
{
"text": {
"query": "Elizabeth",
"path": "plot"
}
}
];

const plot_words = [
{
"text": {
"query": "poet",
"path": "plot"
}
},
{
"text": {
"query": "Elizabeth",
"path": "plot"
}
}
];

const genres_to_avoid = [
{
"text": {
"query": "History",
"path": "genres"
}
},
{
"text": {
"query": "Documentary",
"path": "genres"
}
}
];

db.movies.aggregate([
{
"$search": {
"compound": {
"must": plot_words,
"mustNot": genres_to_avoid
}
}
},
{
"$project": {
"_id": 0,
"score": { "$meta": "searchScore" },
"title": "$title",
"plot": "$plot",
"genres": "$genres",
}
}
]);
Use the Compound Operator with Multiple Clauses: must and should
The following example is an Atlas Search query that uses the compound operator with the must and should clauses. Because minimumShouldMatch is not explicitly set, it defaults to 0 in this case.

db.movies.aggregate([
{
"$search": {
"compound": {
"should": [
{ "text": { "query":"poet", "path":"plot" } },
{ "text": { "query":"Elizabeth", "path":"plot" } }
],
"must": [
{ "equals": { "value": 1934, "path": "year" } }
]
}
}
},
{
"$project": {
"_id": 0,"score":{ "$meta": "searchScore" },
"year":"$year", "title":"$title","plot":"$plot",
}
}
]);
Use the Compound Operator with Multiple Clauses: must, mustNot, should, and filter
The following example is an Atlas Search query that uses the compound operator with the must, mustNot, should, and filter clauses. The query defines constants that are substituted in the final query for better readability.

const only_these_years = [
{
"range": {
"gte": 1992,
"lte": 2000,
"path": "year"
}
}
];

const have_genre_and_earth = [
{
"exists": {
"path": "genres"
}
},
{
"text": {
"query": "earth",
"path": "plot"
}
}
];

const genres_to_avoid = [
{
"text": {
"query": "Documentary",
"path": "genres"
}
},
{
"text": {
"query": "Drama",
"path": "genres"
}
},
{
"text": {
"query": "Comedy",
"path": "genres"
}
}
];

const include_one_of_these_actors = [
{
"phrase": {
"query": "John Leguizamo",
"path": "cast"
}
},
{
"phrase": {
"query": "Gillian Anderson",
"path": "cast"
}
},
{
"phrase": {
"query": "Paula Marshall",
"path": "cast"
}
}
];

db.movies.aggregate([
{
"$search": {
"compound": {
"filter": only_these_years,
"must": have_genre_and_earth,
"mustNot": genres_to_avoid,
"should": include_one_of_these_actors,
"minimumShouldMatch": 1,
}
}
},
{
"$project": {
"_id": 0, "score":{ "$meta": "searchScore" },
"year": "$year", "title": "$title",
"plot": "$plot", "genres": "$genres",
"cast": "$cast"
}
},
]);
Write a Nested Compound Query
The following is an example of a nested compound query in Atlas Search:

const star_wars = {
"phrase": {
"query": "Star Wars",
"path": "title"
}
};

const actor_liam_neeson = {
"phrase": {
"query": "Liam Neeson",
"path": "cast"
}
};

const action_movies = {
"text": {
"query": "Action",
"path": "genres"
}
};


const action_movies = {
"text": {
"query": "Action",
"path": "genres"
}
};


const projected_fields = {
"_id": 0,
"score": { "$meta": "searchScore" },
"year": "$year",
"title": "$title",
"plot": "$plot",
"genres": "$genres",
"cast": "$cast"
};


db.movies.aggregate([
{
"$search": {
"compound": {
"should": [
{ "compound": {
"must": [ star_wars, actor_liam_neeson ] }
},
{ "compound": {
"mustNot": [ actor_liam_neeson ],
"must": [ action_movies ] }
}
],
"minimumShouldMatch": 1
}
}
},
{
"$project": projected_fields
}
]);
