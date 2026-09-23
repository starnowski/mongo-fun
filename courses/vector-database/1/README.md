# Create Embeddings for your Data

### Create a Event Trigger to Update Vector Embeddings
The following code creates an Atlas trigger that inserts embeddings in any document that has been inserted, updated, or replaced.

```
exports = async function(changeEvent) {
    
    const doc = changeEvent.fullDocument;

    const url = 'https://api.openai.com/v1/embeddings';
    
    const openai_key = context.values.get("openAI_secret");
    try {
        console.log(`Processing document with id: ${doc._id}`);

        
        let response = await context.http.post({
            url: url,
             headers: {
                'Authorization': [`Bearer ${openai_key}`],
                'Content-Type': ['application/json']
            },
            body: JSON.stringify({
                
                input: doc.plot,
                model: context.values.get("model")
            })
        });

        
        let responseData = EJSON.parse(response.body.text());

        if(response.statusCode === 200) {
            console.log("Successfully received embedding.");

            const embedding = responseData.data[0].embedding;

            const collection = context.services.get("cluster0").db("sample_mflix").collection("movies");

            const result = await collection.updateOne(
                { _id: doc._id },
                { $set: { plot_embedding: embedding }}
            );

            if(result.modifiedCount === 1) {
                console.log("Successfully updated the document.");
            } else {
                console.log("Failed to update the document.");
            }
        } else {
            console.log(`Failed to receive embedding. Status code: ${response.statusCode}`);
        }

    } catch(err) {
        console.error(err);
    }
};
```

### Create Vector Embeddings
To create vector embeddings, use a function that makes an API request to the text embedding model of your choice. The text embedding model will create embeddings based on the text it receives. Here’s an example:

```
def get_embeddings(text, model, api_key):
    url = 'https://api.openai.com/v1/embeddings'
    headers = {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' +  api_key
    }
    data = {
        'input': text,
        'model': model,
        'options': { 'wait_for_model': True }
    }

    response = requests.post(url, headers=headers, data=json.dumps(data))
    responseData = response.json()

    return responseData['data'][0]['embedding']

```
