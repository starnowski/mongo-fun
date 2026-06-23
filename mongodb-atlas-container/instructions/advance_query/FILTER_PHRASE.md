Prepare java tests class that demonstrate below behaviour of filtering based on string field that sometime can store lower or upper case.


Name it FilterStringPhraseTest.
Make it similar to MoviesSearchTest.

Add tests documents like:


{
 _id: "filterStringPhraseTest_1",
 "type": "groccery"
}

{
_id: "filterStringPhraseTest_2",
"type": "Groccery"
}

When searching by "Groccery", GROCCERY" or "groccery" should return both documents.