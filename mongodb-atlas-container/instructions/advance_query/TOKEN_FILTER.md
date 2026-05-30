Prepare java tests class that demonstrate below behaviour of the token filter.

Name it TokenFilterSearchTest.
Make it similar to CharacterFilterAnalyzerTest.
Use similar constructions for tests below a couple of behavious for the token filter.
Add more tests data (bson files) if you need.
Create separate search index if you need.

Add tests class similar to CharacterFilterAnalyzerTest, that is going to create below search filter.

<search_index>
{
"collectionName": "movies",
"database": "sample_mflix",
"name": "title_folding_index",
"type": "search",
"definition": {
"analyzer": "diacriticFolder",
"searchAnalyzer": "diacriticFolder",
"analyzers": [

		{
                "name": "diacriticFolder",
                "charFilters": [],
                "tokenizer": {
                  "type": "standard"
                },
                "tokenFilters": [
                  {
                    "type": "icuFolding"
                  }
                ]
              }
		
		],
        "mappings": {
            "dynamic": false,
            "fields": {
                "title": {
                    "type": "string"
                }
            }
        }
    }
}
</search_index>
The aggregation pipeline used in test is going to be
<aggregation_pipeline>
db.movies.aggregate([
{
$search: {
index: "title_folding_index",
text: { query: "Pokemon The First Movie", path: "title" },
},
},
{ $project: { _id: 0, title: 1 } },
]);
</aggregation_pipeline>

For test purpose add below tests data
<test_data>
[
{ title: 'Pokèmon: The First Movie - Mewtwo Strikes Back' },
{ title: 'Pokèmon: The First Movie - Mewtwo Strikes Back' },
{ title: 'The First Movie' },
{ title: 'Pokèmon the Movie: Diancie and the Cocoon of Destruction' },
{ title: 'Movie Movie' },
{ title: 'The Forty-first' },
{ title: 'The First Grader' }
]
</test_data>