


https://github.com/quarkusio/quarkus/blob/main/docs/src/main/asciidoc/mongodb.adoc




#### OpenApi schema validator

https://github.com/networknt/json-schema-validator?utm_source=chatgpt.com


#### OpenApi version 3 types and formats

https://swagger.io/docs/specification/v3_0/data-models/data-types/#files



https://cloud.google.com/apigee/docs/api-platform/reference/policies/oas-validation-policy

### ODate
https://olingo.apache.org/doc/odata4/tutorials/sqo_f/tutorial_sqo_f.html



#### Setting ResultSet locally

```shell
mongod --dbpath /data/rs1 --replSet rs0 --port 27017
```

Explanation:

```
--dbpath → data directory (must exist)

--replSet rs0 → replica set name

--port 27017 → default MongoDB port
```

If /data/rs1 doesn’t exist:

```shell
mkdir -p /data/rs1
```

Step 2 — Connect using mongosh

In another terminal:

```shell
mongosh --port 27017
```


Step 3 — Initialize the Replica Set

Inside mongosh, run:

```javascript
rs.initiate()
```

That’s it for a single-node setup.

You should see something like:

```javascript
{
ok: 1
}
```


javascript
Step 4 — Verify Replica Set Status

```javascript
rs.status()
```

