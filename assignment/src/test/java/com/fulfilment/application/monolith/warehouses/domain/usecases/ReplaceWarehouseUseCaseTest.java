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
public class ReplaceWarehouseUseCaseTest {

  @InjectMock
  WarehouseStore warehouseStore;

  @InjectMock
  LocationResolverOperation locationResolverOperation;

  @Inject
  ReplaceWarehouseUseCase replaceWarehouseUseCase;

  @Test
  public void testReplaceWarehouseNoBusinessUnitCode() {
    // Given
    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = null; // Missing business unit code

    // When & Then
    WebApplicationException exception = Assertions.assertThrows(WebApplicationException.class, () -> {
      replaceWarehouseUseCase.replace(newWarehouse);
    });

    Assertions.assertEquals(400, exception.getResponse().getStatus());
    Assertions.assertTrue(exception.getMessage().contains("No Warehouse businessUnitCode provided"));
  }

  @Test
  public void testReplaceWarehouseNoLocation() {
    // Given
    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "XYZ";
    newWarehouse.location = null; // Missing location

    // When & Then
    WebApplicationException exception = Assertions.assertThrows(WebApplicationException.class, () -> {
      replaceWarehouseUseCase.replace(newWarehouse);
    });

    Assertions.assertEquals(400, exception.getResponse().getStatus());
    Assertions.assertTrue(exception.getMessage().contains("No Warehouse location provided"));
  }

  @Test
  public void testReplaceWarehouseBusinessUnitCodeDoesNotExist() {
    // Given
    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "ABC";
    newWarehouse.location = "Location1";

    // Mock no existing warehouse
    Mockito.when(warehouseStore.findByBusinessUnitCode("ABC")).thenReturn(null);

    // When & Then
    WebApplicationException exception = Assertions.assertThrows(WebApplicationException.class, () -> {
      replaceWarehouseUseCase.replace(newWarehouse);
    });

    Assertions.assertEquals(400, exception.getResponse().getStatus());
    Assertions.assertTrue(exception.getMessage().contains("Warehouse with [businessUnitCode=ABC] does not exist"));
  }

  @Test
  public void testReplaceWarehouseInvalidLocation() {
    // Given
    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "MWH.001";
    newWarehouse.location = "InvalidLocation";

    // Mock old warehouse and invalid location
    Mockito.when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(new Warehouse());
    Mockito.when(locationResolverOperation.resolveByIdentifier("InvalidLocation")).thenReturn(null);

    // When & Then
    WebApplicationException exception = Assertions.assertThrows(WebApplicationException.class, () -> {
      replaceWarehouseUseCase.replace(newWarehouse);
    });

    Assertions.assertEquals(400, exception.getResponse().getStatus());
    Assertions.assertTrue(exception.getMessage().contains("Location for warehouse [location=InvalidLocation] doesn't exist"));
  }

  @Test
  public void testReplaceWarehouseExceedsMaxNumberOfWarehouses() {
    // Given
    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "MWH.001";
    newWarehouse.location = "ValidLocation";
    Location location = new Location("ValidLocation", 5, 10_000_000);

    // Mock old warehouse, location, and warehouse count
    Mockito.when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(new Warehouse());
    Mockito.when(locationResolverOperation.resolveByIdentifier("ValidLocation")).thenReturn(location);
    Mockito.when(warehouseStore.countByLocation("ValidLocation")).thenReturn(6L); // Exceeds max

    // When & Then
    WebApplicationException exception = Assertions.assertThrows(WebApplicationException.class, () -> {
      replaceWarehouseUseCase.replace(newWarehouse);
    });

    Assertions.assertEquals(400, exception.getResponse().getStatus());
    Assertions.assertTrue(exception.getMessage().contains("Number of warehouses at Location reached maximum"));
  }

  @Test
  public void testReplaceWarehouseExceedsMaxCapacity() {
    // Given
    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "MWH.001";
    newWarehouse.location = "ValidLocation";
    newWarehouse.capacity = 150;
    Location location = new Location("ValidLocation", 5, 100);

    // Mock old warehouse and location
    Mockito.when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(new Warehouse());
    Mockito.when(locationResolverOperation.resolveByIdentifier("ValidLocation")).thenReturn(location);

    // When & Then
    WebApplicationException exception = Assertions.assertThrows(WebApplicationException.class, () -> {
      replaceWarehouseUseCase.replace(newWarehouse);
    });

    Assertions.assertEquals(400, exception.getResponse().getStatus());
    Assertions.assertTrue(exception.getMessage().contains("Capacity of warehouses exceeds max capacity at Location"));
  }

  @Test
  public void testReplaceWarehouseCapacityLowerThanOld() {
    // Given
    Warehouse oldWarehouse = new Warehouse();
    oldWarehouse.capacity = 100;
    oldWarehouse.stock = 50;

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "MWH.001";
    newWarehouse.location = "ValidLocation";
    newWarehouse.capacity = 80; // Lower capacity than the old warehouse
    newWarehouse.stock = 50;

    Location location = new Location("ValidLocation", 2, 100);

    // Mock old warehouse and location
    Mockito.when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(oldWarehouse);
    Mockito.when(locationResolverOperation.resolveByIdentifier("ValidLocation")).thenReturn(location);

    // When & Then
    WebApplicationException exception = Assertions.assertThrows(WebApplicationException.class, () -> {
      replaceWarehouseUseCase.replace(newWarehouse);
    });

    Assertions.assertEquals(400, exception.getResponse().getStatus());
    Assertions.assertTrue(exception.getMessage().contains("Capacity of new warehouse is lower than capacity of old warehouse"));
  }

  @Test
  public void testReplaceWarehouseStockDoesNotMatch() {
    // Given
    Warehouse oldWarehouse = new Warehouse();
    oldWarehouse.capacity = 100;
    oldWarehouse.stock = 50;

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "MWH.001";
    newWarehouse.location = "ValidLocation";
    newWarehouse.capacity = 100;
    newWarehouse.stock = 40; // Different stock than the old warehouse

    Location location = new Location("ValidLocation", 2, 100);

    // Mock old warehouse and location
    Mockito.when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(oldWarehouse);
    Mockito.when(locationResolverOperation.resolveByIdentifier("ValidLocation")).thenReturn(location);

    // When & Then
    WebApplicationException exception = Assertions.assertThrows(WebApplicationException.class, () -> {
      replaceWarehouseUseCase.replace(newWarehouse);
    });

    Assertions.assertEquals(400, exception.getResponse().getStatus());
    Assertions.assertTrue(exception.getMessage().contains("Stock of new Warehouse doesn't match old Warehouse"));
  }

  @Test
  public void testReplaceWarehouseSuccessful() {
    // Given
    Warehouse oldWarehouse = new Warehouse();
    oldWarehouse.capacity = 100;
    oldWarehouse.stock = 50;

    Warehouse newWarehouse = new Warehouse();
    newWarehouse.businessUnitCode = "MWH.001";
    newWarehouse.location = "ValidLocation";
    newWarehouse.capacity = 150;
    newWarehouse.stock = 50;
    Location location = new Location("ValidLocation", 10, 200);

    // Mock old warehouse, location, and warehouse count
    Mockito.when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(oldWarehouse);
    Mockito.when(locationResolverOperation.resolveByIdentifier("ValidLocation")).thenReturn(location);
    Mockito.when(warehouseStore.countByLocation("ValidLocation")).thenReturn(1L);

    // When
    replaceWarehouseUseCase.replace(newWarehouse);

    // Then
    Mockito.verify(warehouseStore).remove(oldWarehouse);
    Mockito.verify(warehouseStore).create(newWarehouse);
    Assertions.assertNotNull(newWarehouse.createdAt);
  }
}

