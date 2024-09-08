package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;

import java.time.LocalDateTime;

@ApplicationScoped
public class ArchiveWarehouseUseCase implements ArchiveWarehouseOperation {

  @Inject
  private WarehouseStore warehouseStore;

  @Transactional
  @Override
  public void archive(Warehouse warehouse) {
    var entity = warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode);

    if (entity == null) {
      throw new WebApplicationException("Can not archive Warehouse; Warehouse with [businessUnitCode=%s] doesn't exist".formatted(warehouse.businessUnitCode), 400);
    }
    if (entity.archivedAt != null) {
      // Warehouse already archived
      return;
    }
    entity.archivedAt = LocalDateTime.now();
    warehouseStore.update(entity);
  }
}
