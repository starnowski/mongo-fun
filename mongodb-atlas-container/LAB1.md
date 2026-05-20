Below you have example of atlas search index and aggregation pipeline that use such index in query.
You goal is do create java tests class similar to com.github.starnowski.mongo.fun.SimpleSearchTest.

Create few documents in bson file that would match criteria just like "bson/search/search1.json" and "bson/search/search2.json".

<index>
const indexName = "plot_title_idx";
const indexDefinition = {
  mappings: {
    dynamic: false,
    fields: {
      plot: { type: "string" },
      title: { type: "string" },
    },
  },
};
</index>

<pipeline>
db.movies.aggregate([
  {
    $search: {
      index: "plot_title_idx",
      text: {
        query: "The Lion King",
        path: "title"
      }
    }
  },
  {
    $project: {
      title: 1,
      year: 1,
      plot: 1,
    }
  },
  {
    $limit: 1
  }
])
</pipeline>