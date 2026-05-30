Prepare java tests class that demonstrate below behaviour of the character filter analyzer.

Name it CharacterFilterAnalyzerTest.
Make it similar to MoviesSearchTest.
Use similar constructions for tests below a couple of behavious for the character filter analyzer
Add more tests data (bson files) if you need.
Create separate search index if you need.
Code Summary: Custom Analyzers: Character Filters
Create a Custom Analyzer: Character Filter
In Lessons 4–6, we are building an index with a custom analyzer named custom_movie_analyzer.

In Lesson 4, we began to build the custom analyzer by adding a character filter with mappings that convert Roman numerals to decimals. In the video, we did this with the Visual Editor in Atlas.

Here’s an example of an index with a completed version of our custom analyzer, but in the MongoDB Shell:

    {
      "analyzer": "custom_movie_analyzer",
      "searchAnalyzer": "custom_movie_analyzer",
      "mappings": {
        "dynamic": false,
        "fields": {
          "title": {
            "type": "string"
          }
        }
      },
      "analyzers": [
        {
          "name": "custom_movie_analyzer",
          "charFilters": [
            {
              "mappings": {
                "I": "1",
                "II": "2",
                "III": "3",
                "IV": "4",
                "IX": "9",
                "V": "5",
                "VI": "6",
                "VII": "7",
                "VIII": "8",
                "X": "10"
              },
              "type": "mapping"
            }
          ],
          "tokenizer": {
            "type": "standard"
          },
          "tokenFilters": [
            {
              "type": "icuFolding"
            }
          ]
        }
      ]
    }