package com.fulfilment.application.monolith.locations.adapters.restapi;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

@QuarkusTest
public class LocationEndpointTest {

  @Test
  public void testSimpleGetLocation() {
    final String path = "location";

    // Get by identification, should have all 6 locations the database has initially:
    given()
        .when()
        .get(path + "/ZWOLLE-001")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(containsString("ZWOLLE-001"), containsString("1"), containsString("100"));

    given()
        .when()
        .get(path + "/VETSBY-001")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(containsString("VETSBY-001"), containsString("1"), containsString("90"));

    // Get by identification, should fail with 404 on identifier validation:
    given()
        .when()
        .get(path + "/00000-AAA")
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .body(containsString("400"), containsString("Location should be in form PLACE-number, example: 'ZWOLLE-001'"));

    // Get by identification, should fail with 404 if no location found:
    given()
        .when()
        .get(path + "/UKRAINE-001")
        .then()
        .statusCode(404)
        .contentType(ContentType.JSON)
        .body(containsString("404"), containsString("Location not found"));
  }
}
