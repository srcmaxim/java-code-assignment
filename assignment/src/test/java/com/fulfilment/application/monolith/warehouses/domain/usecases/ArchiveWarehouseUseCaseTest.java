package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;

@QuarkusTest
public class ArchiveWarehouseUseCaseTest {

  @InjectMock
  WarehouseStore warehouseStore;

  @Inject
  ArchiveWarehouseUseCase archiveWarehouseUseCase;

  @Test
  public void testArchiveWarehouseNotFound() {
    // Given
    String businessUnitCode = "XYZ";
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = businessUnitCode;

    // Mock behavior
    Mockito.when(warehouseStore.findByBusinessUnitCode(businessUnitCode)).thenReturn(null);

    // When & Then
    WebApplicationException exception = Assertions.assertThrows(WebApplicationException.class, () -> {
      archiveWarehouseUseCase.archive(warehouse);
    });

    Assertions.assertEquals(400, exception.getResponse().getStatus());
    Assertions.assertTrue(exception.getMessage().contains("Warehouse with [businessUnitCode=" + businessUnitCode + "] doesn't exist"));
  }

  @Test
  public void testArchiveWarehouseAlreadyArchived() {
    // Given
    String businessUnitCode = "MWH.001";
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = businessUnitCode;
    Warehouse entity = new Warehouse();
    entity.businessUnitCode = businessUnitCode;
    entity.archivedAt = LocalDateTime.now(); // Already archived

    // Mock behavior
    Mockito.when(warehouseStore.findByBusinessUnitCode(businessUnitCode)).thenReturn(entity);

    // When
    archiveWarehouseUseCase.archive(warehouse);

    // Then
    Mockito.verify(warehouseStore, Mockito.never()).update(entity); // Ensure update is never called
  }

  @Test
  public void testArchiveWarehouseSuccessful() {
    // Given
    String businessUnitCode = "DEF";
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = businessUnitCode;
    Warehouse entity = new Warehouse();
    entity.businessUnitCode = businessUnitCode;
    entity.archivedAt = null; // Not archived

    // Mock behavior
    Mockito.when(warehouseStore.findByBusinessUnitCode(businessUnitCode)).thenReturn(entity);

    // When
    archiveWarehouseUseCase.archive(warehouse);

    // Then
    Mockito.verify(warehouseStore).update(entity); // Ensure update is called
    Assertions.assertNotNull(entity.archivedAt); // Ensure archivedAt is set
  }
}
