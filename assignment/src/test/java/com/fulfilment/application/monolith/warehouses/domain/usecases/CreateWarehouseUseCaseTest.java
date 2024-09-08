package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.locations.domain.models.Location;
import com.fulfilment.application.monolith.locations.domain.ports.LocationResolverOperation;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class CreateWarehouseUseCaseTest {

  @InjectMock
  WarehouseStore warehouseStore;

  @InjectMock
  LocationResolverOperation locationResolverOperation;

  @Inject
  CreateWarehouseUseCase createWarehouseUseCase;

  @Test
  public void testCreateWarehouseNoBusinessUnitCode() {
    // Given
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = null; // Missing business unit code

    // When & Then
    WebApplicationException exception = Assertions.assertThrows(WebApplicationException.class, () -> {
      createWarehouseUseCase.create(warehouse);
    });

    Assertions.assertEquals(400, exception.getResponse().getStatus());
    Assertions.assertTrue(exception.getMessage().contains("No Warehouse businessUnitCode provided"));
  }

  @Test
  public void testCreateWarehouseNoLocation() {
    // Given
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.001";
    warehouse.location = null; // Missing location

    // When & Then
    WebApplicationException exception = Assertions.assertThrows(WebApplicationException.class, () -> {
      createWarehouseUseCase.create(warehouse);
    });

    Assertions.assertEquals(400, exception.getResponse().getStatus());
    Assertions.assertTrue(exception.getMessage().contains("No Warehouse location provided"));
  }

  @Test
  public void testCreateWarehouseBusinessUnitCodeExists() {
    // Given
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.001";
    warehouse.location = "Location1";

    // Mock existing warehouse
    Mockito.when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(new Warehouse());

    // When & Then
    WebApplicationException exception = Assertions.assertThrows(WebApplicationException.class, () -> {
      createWarehouseUseCase.create(warehouse);
    });

    Assertions.assertEquals(400, exception.getResponse().getStatus());
    Assertions.assertTrue(exception.getMessage().contains("Warehouse with [businessUnitCode=MWH.001] already exists"));
  }

  @Test
  public void testCreateWarehouseInvalidLocation() {
    // Given
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.001";
    warehouse.location = "InvalidLocation";

    // Mock location resolver to return null for invalid location
    Mockito.when(locationResolverOperation.resolveByIdentifier("InvalidLocation")).thenReturn(null);

    // When & Then
    WebApplicationException exception = Assertions.assertThrows(WebApplicationException.class, () -> {
      createWarehouseUseCase.create(warehouse);
    });

    Assertions.assertEquals(400, exception.getResponse().getStatus());
    Assertions.assertTrue(exception.getMessage().contains("Location for warehouse [location=InvalidLocation] doesn't exist"));
  }

  @Test
  public void testCreateWarehouseExceedsMaxNumberOfWarehouses() {
    // Given
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.001";
    warehouse.location = "ValidLocation";
    Location location = new Location("ValidLocation", 5, 100_000_000);

    // Mock location and warehouse count
    Mockito.when(locationResolverOperation.resolveByIdentifier("ValidLocation")).thenReturn(location);
    Mockito.when(warehouseStore.countByLocation("ValidLocation")).thenReturn(5L); // Maximum reached

    // When & Then
    WebApplicationException exception = Assertions.assertThrows(WebApplicationException.class, () -> {
      createWarehouseUseCase.create(warehouse);
    });

    Assertions.assertEquals(400, exception.getResponse().getStatus());
    Assertions.assertTrue(exception.getMessage().contains("Number of warehouses at Location reached maximum"));
  }

  @Test
  public void testCreateWarehouseExceedsMaxCapacity() {
    // Given
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.001";
    warehouse.location = "ValidLocation";
    warehouse.capacity = 150;
    Location location = new Location("ValidLocation", 100_000_000, 100);

    // Mock location and capacity
    Mockito.when(locationResolverOperation.resolveByIdentifier("ValidLocation")).thenReturn(location);
    Mockito.when(locationResolverOperation.lockByIdentifier("ValidLocation")).thenReturn(location);

    // When & Then
    WebApplicationException exception = Assertions.assertThrows(WebApplicationException.class, () -> {
      createWarehouseUseCase.create(warehouse);
    });

    Assertions.assertEquals(400, exception.getResponse().getStatus());
    Assertions.assertTrue(exception.getMessage().contains("Capacity of warehouses exceeds max capacity at Location"));
  }

  @Test
  public void testCreateWarehouseSuccessful() {
    // Given
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.001";
    warehouse.location = "ValidLocation";
    warehouse.capacity = 50;
    warehouse.stock = 10;
    Location location = new Location("ValidLocation", 10, 100);

    // Mock location and warehouse count
    Mockito.when(locationResolverOperation.resolveByIdentifier("ValidLocation")).thenReturn(location);
    Mockito.when(locationResolverOperation.lockByIdentifier("ValidLocation")).thenReturn(location);
    Mockito.when(warehouseStore.countByLocation("ValidLocation")).thenReturn(9L); // Less than max warehouses

    // When
    createWarehouseUseCase.create(warehouse);

    // Then
    Mockito.verify(warehouseStore).create(warehouse);
    Assertions.assertNotNull(warehouse.createdAt);
  }
}
