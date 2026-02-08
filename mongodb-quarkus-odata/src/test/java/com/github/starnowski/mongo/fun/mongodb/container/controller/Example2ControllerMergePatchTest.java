package com.github.starnowski.mongo.fun.mongodb.container.controller;

import static io.restassured.RestAssured.given;

import com.github.starnowski.mongo.fun.mongodb.container.test.MongoDatabaseSetupExtension;
import com.mongodb.client.MongoClient;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.UUID;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.JSONAssert;

@QuarkusTest
@ExtendWith(MongoDatabaseSetupExtension.class)
class Example2ControllerMergePatchTest extends AbstractExample2ControllerTest {

  @Inject private MongoClient mongoClient;

  @Test
  public void shouldUpdateObjectWhenEditingNullObjectOnPathWithMergePatchSpecification()
      throws IOException, JSONException {
    // GIVEN
    UUID uuid = UUID.randomUUID();

    // Convert to JSON string
    String json =
        """
                    {
                        "plainString": "test1"
                    }
                """;
    System.out.println("Request payload:");
    System.out.println(json);

    given()
        .body(json)
        .contentType(ContentType.JSON)
        .when()
        .post("/examples2/{id}", uuid)
        .then()
        .statusCode(200)
        .extract();

    // WHEN
    ExtractableResponse<Response> mergeResponse =
        given()
            .body(
                """
                        {
                            "nestedObject": {
                                "tokens": ["last token"]
                            }
                        }
                        """) // doubleValue
            // { "op": "add", "path": "/nestedObject/tokens/-", "value": "last token" }
            .contentType("application/merge-patch+json")
            .when()
            .patch("/examples2/{id}", uuid)
            .then()
            .statusCode(200)
            .extract();
    JSONAssert.assertEquals(
        """
                {
                    "plainString": "test1",
                    "nestedObject": {
                        "tokens": ["last token"]
                    }
                }
                """,
        mergeResponse.asString(),
        true);
  }
}
