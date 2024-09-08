package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.warehouse.api.WarehouseResource;
import com.warehouse.api.beans.Warehouse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import java.util.List;

@ApplicationScoped
public class WarehouseResourceImpl implements WarehouseResource {

  @Inject
  private WarehouseStore warehouseStore;
  @Inject
  private ArchiveWarehouseOperation archiveWarehouseOperation;
  @Inject
  private CreateWarehouseOperation createWarehouseOperation;
  @Inject
  private ReplaceWarehouseOperation replaceWarehouseOperation;

  @Override
  @Operation(summary = "List all warehouse units", description = "Returns a list of all warehouse units.")
  @APIResponses(value = {
      @APIResponse(responseCode = "200", description = "List of warehouse units retrieved successfully",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = Warehouse.class)))
  })
  public List<Warehouse> listAllWarehousesUnits() {
    return warehouseStore.getAll().stream().map(this::toWarehouseResponse).toList();
  }

  @Transactional
  @Override
  @Operation(summary = "Create a new warehouse unit", description = "Creates a new warehouse unit in the system.")
  @APIResponses(value = {
      @APIResponse(responseCode = "201", description = "Warehouse unit created successfully",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = Warehouse.class))),
      @APIResponse(responseCode = "409", description = "Conflict creating warehouses at the same location"),
      @APIResponse(responseCode = "422", description = "Invalid input")
  })
  public Warehouse createANewWarehouseUnit(
      @Parameter(description = "Warehouse object that needs to be created", required = true) @NotNull Warehouse data) {
    if (data.getId() != null) {
      throw new WebApplicationException("Id was invalidly set on request.", 422);
    }
    createWarehouseOperation.create(toWarehouse(data));
    var buCode = data.getBusinessUnitCode();
    var warehouse = warehouseStore.findByBusinessUnitCode(buCode);
    return toWarehouseResponse(warehouse);
  }

  @Override
  @Operation(summary = "Get a warehouse unit by ID", description = "Returns a warehouse unit for the given business unit code.")
  @APIResponses(value = {
      @APIResponse(responseCode = "200", description = "Warehouse unit found",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = Warehouse.class))),
      @APIResponse(responseCode = "404", description = "Warehouse unit not found")
  })
  public Warehouse getAWarehouseUnitByID(
      @Parameter(description = "Business unit code of the warehouse to retrieve", required = true) String buCode) {
    var warehouse = warehouseStore.findByBusinessUnitCode(buCode);
    if (warehouse == null) {
      throw new WebApplicationException("Warehouse with id of " + buCode + " does not exist.", 404);
    }
    return toWarehouseResponse(warehouse);
  }

  @Transactional
  @Override
  @Operation(summary = "Archive a warehouse unit by ID", description = "Archives a warehouse unit in the system by business unit code.")
  @APIResponses(value = {
      @APIResponse(responseCode = "204", description = "Warehouse unit archived successfully"),
      @APIResponse(responseCode = "404", description = "Warehouse unit not found")
  })
  public void archiveAWarehouseUnitByID(
      @Parameter(description = "Business unit code of the warehouse to archive", required = true) String buCode) {
    var warehouse = warehouseStore.findByBusinessUnitCode(buCode);
    if (warehouse == null) {
      throw new WebApplicationException("Warehouse with id of " + buCode + " does not exist.", 404);
    }
    archiveWarehouseOperation.archive(warehouse);
  }

  @Transactional
  @Override
  @Operation(summary = "Replace the current active warehouse", description = "Replaces the current active warehouse unit in the system by business unit code.")
  @APIResponses(value = {
      @APIResponse(responseCode = "200", description = "Warehouse unit replaced successfully",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = Warehouse.class))),
      @APIResponse(responseCode = "404", description = "Warehouse unit not found"),
      @APIResponse(responseCode = "422", description = "Invalid input")
  })
  public Warehouse replaceTheCurrentActiveWarehouse(
      @Parameter(description = "Business unit code of the warehouse to replace", required = true) String buCode,
      @Parameter(description = "Warehouse object with updated details", required = true) @NotNull Warehouse data) {
    var warehouse = warehouseStore.findByBusinessUnitCode(buCode);
    if (warehouse == null) {
      throw new WebApplicationException("Warehouse with id of " + buCode + " does not exist.", 404);
    }
    var newWarehouse = toWarehouse(data);
    replaceWarehouseOperation.replace(newWarehouse);
    return toWarehouseResponse(newWarehouse);
  }

  private Warehouse toWarehouseResponse(
      com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse) {
    var response = new Warehouse();
    response.setBusinessUnitCode(warehouse.businessUnitCode);
    response.setLocation(warehouse.location);
    response.setCapacity(warehouse.capacity);
    response.setStock(warehouse.stock);

    return response;
  }

  public com.fulfilment.application.monolith.warehouses.domain.models.Warehouse toWarehouse(Warehouse data) {
    var warehouse = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    warehouse.businessUnitCode = data.getBusinessUnitCode();
    warehouse.location = data.getLocation();
    warehouse.capacity = data.getCapacity();
    warehouse.stock = data.getStock();
    return warehouse;
  }
}
