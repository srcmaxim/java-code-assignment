package com.fulfilment.application.monolith.stores;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Path("store")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class StoreResource {

  private static final Logger LOGGER = Logger.getLogger(StoreResource.class.getName());

  @Inject
  private LegacyStoreManagerGateway legacyStoreManagerGateway;
  @Inject
  private MeterRegistry registry;
  
  @GET
  @Operation(summary = "Get all stores", description = "Returns a list of all stores sorted by name.")
  @APIResponses(value = {
      @APIResponse(responseCode = "200", description = "Successfully retrieved list",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = Store.class)))
  })
  public List<Store> get() {
    return Store.listAll(Sort.by("name"));
  }

  @GET
  @Path("{id}")
  @Operation(summary = "Get store by ID", description = "Returns a single store for the given ID.")
  @APIResponses(value = {
      @APIResponse(responseCode = "200", description = "Store found",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = Store.class))),
      @APIResponse(responseCode = "404", description = "Store not found")
  })
  public Store getSingle(
      @Parameter(description = "ID of the store to retrieve", required = true) Long id) {
    Store entity = Store.findById(id);
    if (entity == null) {
      throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
    }
    return entity;
  }

  @POST
  @Timed(value = "create_store_time", description = "Time taken to create store")
  @Counted(value = "create_store_count", description = "Number of stores created")
  @Operation(summary = "Create a new store", description = "Creates a new store in the system.")
  @APIResponses(value = {
      @APIResponse(responseCode = "201", description = "Store created successfully",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = Store.class))),
      @APIResponse(responseCode = "422", description = "Invalid input")
  })
  public Response create(
      @Parameter(description = "Store object that needs to be added", required = true) Store store) {
    if (store.id != null) {
      throw new WebApplicationException("Id was invalidly set on request.", 422);
    }

    var entityCopy = new AtomicReference<Store>();
    QuarkusTransaction.requiringNew().run(() -> {
      store.persist();
      // Persistence entities should not be exposed to external layers.
      // This leads to "Object modified outside of transaction" kind of errors.
      entityCopy.set(new Store(store));
    });

    try {
      // TODO srcmaxim: Should I do @Timeout, @Retry, @CircuitBreaker here? Does this call idempotent?
      legacyStoreManagerGateway.createStoreOnLegacySystem(entityCopy.get());
    } catch (RuntimeException e) {
      registry.counter("create_legacy_store_errors").count();
      LOGGER.warnv(e, "Failed to create Store in legacy system; Store {0}", entityCopy.get());
      throw e;
    }
    return Response.ok(entityCopy.get()).status(201).build();
  }

  @PUT
  @Path("{id}")
  @Timed(value = "update_store_time", description = "Time taken to update store")
  @Counted(value = "update_store_count", description = "Number of stores updated")
  @Operation(summary = "Update an existing store", description = "Updates an existing store in the system by ID.")
  @APIResponses(value = {
      @APIResponse(responseCode = "200", description = "Store updated successfully",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = Store.class))),
      @APIResponse(responseCode = "404", description = "Store not found"),
      @APIResponse(responseCode = "422", description = "Invalid input")
  })
  public Store update(
      @Parameter(description = "ID of the store to update", required = true) Long id,
      @Parameter(description = "Updated store object", required = true) Store updatedStore) {
    if (updatedStore.name == null) {
      throw new WebApplicationException("Store Name was not set on request.", 422);
    }

    var entityCopy = new AtomicReference<Store>();
    QuarkusTransaction.requiringNew().run(() -> {
      Store entity = Store.findById(id);

      if (entity == null) {
        throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
      }

      entity.name = updatedStore.name;
      entity.quantityProductsInStock = updatedStore.quantityProductsInStock;
      // Persistence entities should not be exposed to external layers.
      // This leads to "Object modified outside of transaction" kind of errors.
      entityCopy.set(new Store(entity));
    });

    try {
      // TODO srcmaxim: Should I do @Timeout, @Retry, @CircuitBreaker here? Does this call idempotent?
      legacyStoreManagerGateway.updateStoreOnLegacySystem(entityCopy.get());
    } catch (RuntimeException e) {
      registry.counter("update_legacy_store_errors").count();
      LOGGER.warnv(e, "Failed to update Store in legacy system; Store {0}", entityCopy.get());
      throw e;
    }
    return entityCopy.get();
  }

  @PATCH
  @Path("{id}")
  @Timed(value = "patch_store_time", description = "Time taken to patch store")
  @Counted(value = "patch_store_count", description = "Number of store patches")
  @Operation(summary = "Patch an existing store", description = "Partially updates an existing store in the system by ID.")
  @APIResponses(value = {
      @APIResponse(responseCode = "200", description = "Store patched successfully",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = Store.class))),
      @APIResponse(responseCode = "404", description = "Store not found"),
      @APIResponse(responseCode = "422", description = "Invalid input")
  })
  public Store patch(
      @Parameter(description = "ID of the store to patch", required = true) Long id,
      @Parameter(description = "Store object with the fields to patch", required = true) Store updatedStore) {
    if (updatedStore.name == null) {
      throw new WebApplicationException("Store Name was not set on request.", 422);
    }

    var entityCopy = new AtomicReference<Store>();
    QuarkusTransaction.requiringNew().run(() -> {
      Store entity = Store.findById(id);

      if (entity == null) {
        throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
      }

      if (entity.name != null) {
        entity.name = updatedStore.name;
      }

      if (entity.quantityProductsInStock != 0) {
        entity.quantityProductsInStock = updatedStore.quantityProductsInStock;
      }
      entity.persist();
      // Persistence entities should not be exposed to external layers.
      // This leads to "Object modified outside of transaction" kind of errors.
      entityCopy.set(new Store(entity));
    });

    try {
      // TODO srcmaxim: Should I do @Timeout, @Retry, @CircuitBreaker here? Does this call idempotent?
      legacyStoreManagerGateway.updateStoreOnLegacySystem(entityCopy.get());
    } catch (RuntimeException e) {
      registry.counter("patch_legacy_store_errors").count();
      LOGGER.warnv(e, "Failed to patch Store in legacy system; Store {0}", entityCopy.get());
      throw e;
    }
    return new Store(entityCopy.get());
  }

  @DELETE
  @Path("{id}")
  @Transactional
  @Operation(summary = "Delete a store", description = "Deletes a store from the system by ID.")
  @APIResponses(value = {
      @APIResponse(responseCode = "204", description = "Store deleted successfully"),
      @APIResponse(responseCode = "404", description = "Store not found")
  })
  public Response delete(
      @Parameter(description = "ID of the store to delete", required = true) Long id) {
    Store entity = Store.findById(id);
    if (entity == null) {
      throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
    }
    entity.delete();
    return Response.status(204).build();
  }

  @Provider
  public static class ErrorMapper implements ExceptionMapper<Exception> {

    @Inject
    ObjectMapper objectMapper;

    @Override
    public Response toResponse(Exception exception) {
      LOGGER.error("Failed to handle request", exception);

      int code = 500;
      if (exception instanceof WebApplicationException) {
        code = ((WebApplicationException) exception).getResponse().getStatus();
      }

      ObjectNode exceptionJson = objectMapper.createObjectNode();
      exceptionJson.put("exceptionType", exception.getClass().getName());
      exceptionJson.put("code", code);

      if (exception.getMessage() != null) {
        exceptionJson.put("error", exception.getMessage());
      }

      return Response.status(code).entity(exceptionJson).build();
    }
  }
}
