package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.locations.domain.ports.LocationResolverOperation;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;

import java.time.LocalDateTime;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  @Inject
  private WarehouseStore warehouseStore;

  @Inject
  private LocationResolverOperation locationResolverOperation;

  @Override
  @Transactional
  public void replace(Warehouse newWarehouse) {
    if (newWarehouse.businessUnitCode == null) {
      throw new WebApplicationException("Can not replace Warehouse; No Warehouse businessUnitCode provided", 400);
    }
    if (newWarehouse.location == null) {
      throw new WebApplicationException("Can not replace Warehouse; No Warehouse location provided", 400);
    }

    // Business Unit Code Verification
    // Ensure that the specified business unit code for the warehouse doesn't already exists.
    var oldWarehouse = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (oldWarehouse == null) {
      throw new WebApplicationException("Can not replace Warehouse; Warehouse with [businessUnitCode=%s] does not exist".formatted(newWarehouse.businessUnitCode), 400);
    }
    // Location Validation
    // TODO srcmaxim: Found one corner case?
    // Should old and new Warehouse exist in the same location?
    //    if (!newWarehouse.location.equals(oldWarehouse.location)) {
    //      throw new WebApplicationException("Can not replace Warehouse; Warehouse with [businessUnitCode=%s] is not in the same Location".formatted(newWarehouse.businessUnitCode), 400);
    //    }
    // Confirm that the warehouse location is valid, meaning it must be an existing valid location.
    var location = locationResolverOperation.resolveByIdentifier(newWarehouse.location);
    if (location == null) {
      throw new WebApplicationException("Can not replace Warehouse; Location for warehouse [location=%s] doesn't exist".formatted(newWarehouse.location), 400);
    }
    // Warehouse Creation Feasibility
    // Check if a new warehouse can be created at the specified location or if the maximum number of warehouses has already been reached.
    // TODO srcmaxim: How to check if warehouse can be created at the specified location?
    long warehousesInTheSameLocation = warehouseStore.countByLocation(newWarehouse.location);
    // TODO srcmaxim: We can't guarantee that new warehouses are not added as part of other transactions.
    // This is phantom read problem, it happens when you read data twice and new data appears.
    // We can fix it by using SERIALIZABLE transaction (TX will think as it's executes sequentially w/o affect of other TXs),
    // or by creating new table LocationWarehouseCount and selecting and updating it with DB lock:
    //
    // 1. Block row on READ with `FOR UPDATE`:
    //     public LocationWarehouseCount findUnprocessedEvents(String location) {
    //        return select("location", location)
    //                .withLock(LockModeType.WRITE)
    //                .first();
    //    }
    //
    // 2. Then create new Warehouse and update LocationWarehouseCount
    //
    // 3. Other transactions that want to add must wait on one row.
    //
    // 4. We can also do the same with optimistic locking, and for that we need to increment LocationWarehouseCount version.
    //    Update version, if version was changed before in the db we need to rollback transaction.
    //
    // 5. Also we can do CQRS to process all data as a stream of events.
    //    This is what Picknic does for updating warehouses. But, do you want to rewrite all app? :)
    //
    if (warehousesInTheSameLocation > location.maxNumberOfWarehouses()) {
      throw new WebApplicationException("Can not replace Warehouse; Number of warehouses at Location reached maximum [location=%s, maxNumberOfWarehouses=%s]"
          .formatted(newWarehouse.location, location.maxNumberOfWarehouses()), 400);
    }
    // Capacity and Stock Validation
    // Validate the warehouse capacity, ensuring it does not exceed the maximum capacity associated with the location and that it can handle the stock informed.
    if (newWarehouse.capacity > location.maxCapacity()) {
      throw new WebApplicationException("Can not replace Warehouse; Capacity of warehouses exceeds max capacity at Location [capacity=%s, maxCapacity=%s]"
          .formatted(newWarehouse.capacity, location.maxCapacity()), 400);
    }
    // TODO srcmaxim: How to check that it can handle the stock informed?

    // Additional Validations for Replacing a Warehouse
    // Capacity Accommodation
    // Ensure the new warehouse's capacity can accommodate the stock from the warehouse being replaced.
    if (newWarehouse.capacity < oldWarehouse.capacity) {
      throw new WebApplicationException("Can not replace Warehouse; Capacity of new warehouse is lower than capacity of old warehouse [newCapacity=%s, oldCapacity=%s]"
          .formatted(newWarehouse.capacity, oldWarehouse.capacity), 400);
    }
    // Stock Matching
    // Confirm that the stock of the new warehouse matches the stock of the previous warehouse.
    if (newWarehouse.stock != oldWarehouse.stock) {
      throw new WebApplicationException("Can not replace Warehouse; Stock of new Warehouse doesn't match old Warehouse [newStock=%s, oldStock=%s]"
          .formatted(newWarehouse.stock, oldWarehouse.stock), 400);
    }

    // TODO srcmaxim: What to do on warehouse replacement? Remove Warehouse or archive it?
    warehouseStore.remove(oldWarehouse);
    newWarehouse.createdAt = LocalDateTime.now();
    warehouseStore.create(newWarehouse);
  }
}
