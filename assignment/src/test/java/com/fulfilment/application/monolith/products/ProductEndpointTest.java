package com.fulfilment.application.monolith.products;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

@QuarkusTest
public class ProductEndpointTest {

  @Test
  public void testCrudProduct() {
    // This test designed to run continuously, so DB state is the same after running tests
    final String path = "product";

    // List all, should have all 3 products the database has initially:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(containsString("TONSTAD"), containsString("KALLAX"), containsString("BESTÅ"));

    // Delete the TONSTAD:
    given()
        .when()
        .delete(path + "/1")
        .then()
        .statusCode(204);

    // List all, TONSTAD should be missing now:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(not(containsString("TONSTAD")), containsString("KALLAX"), containsString("BESTÅ"));

    // Create TONSTAD again:
    var id = given()
        .when()
        .contentType(ContentType.JSON)
        .body(new HashMap<String, Object>() {{
          put("name", "TONSTAD");
          put("description", null);
          put("price", null);
          put("stock", 10);
        }})
        .post(path)
        .then()
        .statusCode(201)
        .contentType(ContentType.JSON)
        .body(containsString("TONSTAD"))
        .extract()
        .response()
        .path("id");

    assertThat(id).isNotNull().asString().asInt().isPositive();

    // List all, should have all 3 products the database:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(containsString("TONSTAD"), containsString("KALLAX"), containsString("BESTÅ"));

    // Update TONSTAD:
    given()
        .when()
        .contentType(ContentType.JSON)
        .body(new HashMap<String, Object>() {{
          put("name", "TONSTAD_UPDATED");
          put("description", null);
          put("price", null);
          put("stock", 101010);
        }})
        .put(path + '/' + id)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(containsString("TONSTAD_UPDATED"));

    // Get TONSTAD_UPDATED:
    given()
        .when()
        .get(path + '/' + id)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(containsString("TONSTAD_UPDATED"), containsString("101010"));


    // List all, should have all 3 products the database:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(containsString("TONSTAD_UPDATED"), containsString("KALLAX"), containsString("BESTÅ"));

    // Update TONSTAD to the initial state:
    given()
        .when()
        .contentType(ContentType.JSON)
        .body(new HashMap<String, Object>() {{
          put("name", "TONSTAD");
          put("description", null);
          put("price", null);
          put("stock", 101010);
        }})
        .put(path + '/' + id)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(containsString("TONSTAD"));
  }
}
