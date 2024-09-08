package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

@QuarkusTest
public class WarehouseEndpointTest {

  @Test
  public void testSimpleListWarehouses() {

    final String path = "warehouse";

    // List all, should have all 3 warehouses the database has initially:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("MWH.012"), containsString("MWH.023"));
  }

  @Test
  public void testSimpleArchivingAndCreatingWarehouses() {
    final String path = "warehouse";

    // List all, should have all 3 warehouses the database has initially:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(
            containsString("MWH.001"),
            containsString("MWH.012"),
            containsString("MWH.023"));

    // Archive the MWH.001:
    given().when().delete(path + "/MWH.001").then().statusCode(204);

    // List all, MWH.001 should be missing now:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(
            not(containsString("MWH.001")),
            containsString("MWH.012"),
            containsString("MWH.023"));

    // Create MWH.001 again:
    given()
        .when()
        .contentType(ContentType.JSON)
        .body(Map.of(
            "businessUnitCode", "MWH.001",
            "location", "ZWOLLE-001",
            "capacity", 100,
            "stock", 10
        ))
        .post(path)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(containsString("MWH.001"));

    // When: Create MWH.123 in ZWOLLE-001 location:
    given()
        .when()
        .contentType(ContentType.JSON)
        .body(Map.of(
            "businessUnitCode", "MWH.123",
            "location", "ZWOLLE-001",
            "capacity", 100,
            "stock", 10
        ))
        .post(path)
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .body(containsString("Can not create Warehouse; Number of warehouses at Location reached maximum [location=ZWOLLE-001, maxNumberOfWarehouses=1]"));

    // List all, should not have new warehouse:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(not(containsString("MWH.123")));

    // When: Create MWH.123 in ZWOLLE-002 location and Warehouse.capacity > maxCapacity:
    given()
        .when()
        .contentType(ContentType.JSON)
        .body(Map.of(
            "businessUnitCode", "MWH.123",
            "location", "ZWOLLE-002",
            "capacity", 51,
            "stock", 10
        ))
        .post(path)
        .then()
        .statusCode(400)
        .contentType(ContentType.JSON)
        .body(containsString("Can not create Warehouse; Capacity of warehouses exceeds max capacity at Location [capacity=51, maxCapacity=50]"));

    // When: Create MWH.123 in ZWOLLE-002 location:
    given()
        .when()
        .contentType(ContentType.JSON)
        .body(Map.of(
            "businessUnitCode", "MWH.123",
            "location", "ZWOLLE-002",
            "capacity", 50,
            "stock", 10
        ))
        .post(path)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(containsString("MWH.123"));

    // List all, should have new warehouse:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(containsString("MWH.123"));

    // Archive the MWH.123:
    given().when().delete(path + "/MWH.123").then().statusCode(204);

    // List all, MWH.123 should be missing now:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(
            not(containsString("MWH.123")),
            containsString("MWH.001"),
            containsString("MWH.012"),
            containsString("MWH.023"));
  }
}
