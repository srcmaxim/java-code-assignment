package com.fulfilment.application.monolith.stores;

import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@QuarkusTest
public class StoreEndpointTest {

  @Inject
  private LegacyStoreManagerGateway legacyStoreManagerGateway;

  @Test
  public void testCrudProduct() {
    // This test designed to run continuously, so DB state is the same after running tests
    final String path = "store";

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
        .body(Map.of(
            "name", "TONSTAD",
            "quantityProductsInStock", 10
        ))
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
        .body(Map.of(
            "name", "TONSTAD_UPD",
            "quantityProductsInStock", 101010
        ))
        .put(path + '/' + id)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(containsString("TONSTAD_UPD"));

    // Update TONSTAD_UPD:
    given()
        .when()
        .contentType(ContentType.JSON)
        .body(Map.of(
            "name", "TONSTAD_UPDATED",
            "quantityProductsInStock", 101010
        ))
        .patch(path + '/' + id)
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
        .body(Map.of(
            "name", "TONSTAD",
            "quantityProductsInStock", 10
        ))
        .patch(path + '/' + id)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(containsString("TONSTAD"));
  }

  /**
   * Messages to legacy system could be delivered once or never "at most once" guarantee.
   * Here we test if message is not delivered to legacy system.
   * New system should save state in any case.
   */
  @Test
  public void testAtMostOnceRequestDelivery_WhenLegacySystemFailed_ThenStillSaveDataInNewAppDb() {
    // Given: Legacy system failed to store Store
    var legacyStore = mock(LegacyStoreManagerGateway.class);
    doThrow(new WebApplicationException("test", 500)).when(legacyStore).createStoreOnLegacySystem(any());
    doThrow(new WebApplicationException("test", 500)).when(legacyStore).updateStoreOnLegacySystem(any());
    QuarkusMock.installMockForType(legacyStore, LegacyStoreManagerGateway.class);

    // This test designed to run continuously, so DB state is the same after running tests
    final String path = "store";


    // When: Create WORTUNG when legacy system failed:
    given()
        .when()
        .contentType(ContentType.JSON)
        .body(Map.of(
            "name", "WORTUNG",
            "quantityProductsInStock", 10
        ))
        .post(path)
        .then()
        .statusCode(500)
        .contentType(ContentType.JSON)
        .body(containsString("test"));

    // Then: List all, we should have WORTUNG even when call to remote service failed:
    var response = given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(containsString("WORTUNG"))
        .extract()
        .body()
        .as(new TypeRef<List<Map<String, Object>>>() {
        });

    // Here we get ID for previous request that failed
    int id = response.stream()
        .filter(store -> store.get("name").equals("WORTUNG"))
        .mapToInt(store -> (Integer) store.get("id"))
        .findFirst()
        .orElseThrow();

    // Check WORTUNG ID
    not(equalTo(-1)).matches(id);

    // When: Update WORTUNG when legacy system failed:
    given()
        .when()
        .contentType(ContentType.JSON)
        .body(Map.of(
            "name", "WORTUNG_UPDATED",
            "quantityProductsInStock", 101010
        ))
        .put(path + '/' + id)
        .then()
        .statusCode(500)
        .contentType(ContentType.JSON)
        .body(containsString("test"));

    // Then: Get WORTUNG_UPDATED, we should have WORTUNG_UPDATED even when call to remote service failed:
    given()
        .when()
        .get(path + '/' + id)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(containsString("WORTUNG_UPDATED"), containsString("101010"));

    // When: Update WORTUNG when legacy system failed:
    given()
        .when()
        .contentType(ContentType.JSON)
        .body(Map.of(
            "name", "WORTUNG",
            "quantityProductsInStock", 10
        ))
        .put(path + '/' + id)
        .then()
        .statusCode(500)
        .contentType(ContentType.JSON)
        .body(containsString("test"));

    // Then: Get WORTUNG, we should have WORTUNG_UPDATED even when call to remote service failed:
    given()
        .when()
        .get(path + '/' + id)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(containsString("WORTUNG"), containsString("10"));

    // Delete WORTUNG:
    given()
        .when()
        .delete(path + '/' + id)
        .then()
        .statusCode(204);

    verify(legacyStore, times(1)).createStoreOnLegacySystem(any());
    verify(legacyStore, times(2)).updateStoreOnLegacySystem(any()); // For PUT, PATCH
  }
}
