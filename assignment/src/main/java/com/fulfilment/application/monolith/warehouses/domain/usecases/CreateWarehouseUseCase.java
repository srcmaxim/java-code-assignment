package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.locations.domain.models.Location;
import com.fulfilment.application.monolith.locations.domain.ports.LocationResolverOperation;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PessimisticLockException;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;

import java.time.LocalDateTime;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  @Inject
  private WarehouseStore warehouseStore;
  @Inject
  private LocationResolverOperation locationResolverOperation;
  @Inject
  private MeterRegistry registry;

  @Override
  @Transactional
  public void create(Warehouse warehouse) {
    if (warehouse.businessUnitCode == null) {
      throw new WebApplicationException("Can not create Warehouse; No Warehouse businessUnitCode provided", 400);
    }
    if (warehouse.location == null) {
      throw new WebApplicationException("Can not create Warehouse; No Warehouse location provided", 400);
    }

    // Business Unit Code Verification
    // Ensure that the specified business unit code for the warehouse doesn't already exists.
    var oldWarehouse = warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode);
    if (oldWarehouse != null) {
      throw new WebApplicationException("Can not create Warehouse; Warehouse with [businessUnitCode=%s] already exists".formatted(warehouse.businessUnitCode), 400);
    }
    // Location Validation
    // Confirm that the warehouse location is valid, meaning it must be an existing valid location.
    var location = locationResolverOperation.resolveByIdentifier(warehouse.location);
    if (location == null) {
      throw new WebApplicationException("Can not create Warehouse; Location for warehouse [location=%s] doesn't exist".formatted(warehouse.location), 400);
    }
    // Warehouse Creation Feasibility
    // Check if a new warehouse can be created at the specified location or if the maximum number of warehouses has already been reached.
    // TODO srcmaxim: How to check if warehouse can be created at the specified location?
    // TODO srcmaxim: Should I count by location only or by location and not archived Warehouses? My implementation is count by location and not archived Warehouses.
    // TODO srcmaxim: We can't guarantee that new warehouses are not added as part of other transactions.
    // This is phantom read problem, it happens when you read data twice and new data appears from other TXs.
    //
    // 1. We can fix it by using SERIALIZABLE TX. Our TX will think as it's executes sequentially w/o affect of other TXs.
    //
    // 2. Lock on Location table row on READ with `FOR UPDATE`:
    //     public Long findUnprocessedEvents(String location) {
    //        return select("location", location)
    //                .withLock(LockModeType.WRITE)
    //                .first();
    //    }
    //    Other transactions that want to add Warehouse must wait on one row.
    //    We can force `NOWAIT` policy to prevent deadlock.
    //    If we don't want to wait on lock we can return with HTTP 409 Concurrent modification exception.
    //
    // 3. We can also do the same with optimistic locking, and for that we need to create new table LocationWarehouseCount.
    //    First we read locationCount from LocationWarehouseCount. Then we do business logic w/o TX running.
    //    Then when we want to add Warehouse we run 3 SQL queries in one TX in DB:
    //    SELECT 1 FROM locationwarehousecount WHERE location = :loc AND count = :oldCount FOR UPDATE;
    //    INSERT INTO warehouse (...) VALUES (...);
    //    UPDATE locationwarehousecount set count =?:newCount location WHERE location =?:loc AND count =?:oldCount FOR UPDATE;
    //
    // 4. Also we can do CQRS to process all data as a stream of events.
    //    This is what Picnic does for updating warehouses. But, do you want to rewrite all app? :)
    //
    // I'll go with solution #2 because it's the most straightforward of all.
    //
    long warehousesInTheSameLocation = warehouseStore.countByLocation(warehouse.location);
    canCreateWarehouseAtLocation(warehousesInTheSameLocation, location);
    try {
      // Try to convert Location READ lock to WRITE lock.
      // Lock on Location with `FOR UPDATE NOWAIT` to prevent deadlock
      location = locationResolverOperation.lockByIdentifier(warehouse.location);
      warehousesInTheSameLocation = warehouseStore.countByLocation(warehouse.location);
      // Repeat business logic in slow path
      canCreateWarehouseAtLocation(warehousesInTheSameLocation, location);
    } catch (PessimisticLockException e) {
      // TODO srcmaxim: I didn't test this part!
      // We need proper test setup for DB invariants test with load testing.
      registry.counter("create_warehouse_pessimistic_lock_fail").count();
      throw new WebApplicationException("Can not create Warehouse; Resource conflict; Try again", 409);
    }
    // Capacity and Stock Validation
    // Validate the warehouse capacity, ensuring it does not exceed the maximum capacity associated with the location and that it can handle the stock informed.
    // TODO srcmaxim: Found a bug in SQL!
    // As you see max capacity at Location of Warehouse is less than in Warehouse.
    // INSERT INTO warehouse(id, businessUnitCode, location, capacity, ...)
    // VALUES (1, 'MWH.001', 'ZWOLLE-001', 100, ...);
    // INSERT INTO location(id, identification, maxNumberOfWarehouses, maxCapacity)
    // VALUES (1, 'ZWOLLE-001', 1, 40);
    if (warehouse.capacity > location.maxCapacity()) {
      throw new WebApplicationException("Can not create Warehouse; Capacity of warehouses exceeds max capacity at Location [capacity=%s, maxCapacity=%s]"
          .formatted(warehouse.capacity, location.maxCapacity()), 400);
    }
    // TODO srcmaxim: How to check that it can handle the stock informed?
    if (warehouse.stock <= 0) {
      throw new WebApplicationException("Can not create Warehouse; Stock of Warehouse is less that 0 [stock=%s]"
          .formatted(warehouse.stock), 400);
    }
    warehouse.createdAt = LocalDateTime.now();
    warehouseStore.create(warehouse);
  }

  private static void canCreateWarehouseAtLocation(long warehousesInTheSameLocation, Location location) {
    if (warehousesInTheSameLocation + 1 > location.maxNumberOfWarehouses()) {
      throw new WebApplicationException("Can not create Warehouse; Number of warehouses at Location reached maximum [location=%s, maxNumberOfWarehouses=%s]"
          .formatted(location.identification(), location.maxNumberOfWarehouses()), 400);
    }
  }
}
