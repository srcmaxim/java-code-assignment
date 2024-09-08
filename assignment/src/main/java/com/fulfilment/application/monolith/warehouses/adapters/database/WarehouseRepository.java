package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  @Override
  public List<Warehouse> getAll() {
    return list("archivedAt is null").stream()
        .map(DbWarehouse::toWarehouse)
        .toList();
  }

  @Override
  public void create(Warehouse warehouse) {
    persist(toDbWarehouse(warehouse));
  }

  @Override
  public void update(Warehouse warehouse) {
    update("location = ?1, capacity = ?2, stock = ?3, archivedAt = ?4 where businessUnitCode = ?5 and archivedAt is null",
        warehouse.location,
        warehouse.capacity,
        warehouse.stock,
        // warehouse.createdAt, createdAt set only at create
        warehouse.archivedAt, // We can archive entity by setting archivedAt, this is one time operation
        warehouse.businessUnitCode);
  }

  @Override
  public void remove(Warehouse warehouse) {
    delete("businessUnitCode", warehouse.businessUnitCode);
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    return find("businessUnitCode = ?1 and archivedAt is null", buCode).firstResultOptional()
        .map(DbWarehouse::toWarehouse)
        .orElse(null);
  }

  @Override
  public long countByLocation(String location) {
    return count("location = ?1 and archivedAt is null", location);
  }

  public DbWarehouse toDbWarehouse(Warehouse data) {
    var warehouse = new DbWarehouse();
    warehouse.businessUnitCode = data.businessUnitCode;
    warehouse.location = data.location;
    warehouse.capacity = data.capacity;
    warehouse.stock = data.stock;
    return warehouse;
  }
}
