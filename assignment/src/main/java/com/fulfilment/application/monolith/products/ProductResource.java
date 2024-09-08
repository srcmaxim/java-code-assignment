package com.fulfilment.application.monolith.products;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.List;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

@Path("product")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class ProductResource {

  @Inject
  ProductRepository productRepository;

  private static final Logger LOGGER = Logger.getLogger(ProductResource.class.getName());

  @GET
  @Operation(summary = "Get all products", description = "Returns a list of all products sorted by name.")
  @APIResponses(value = {
      @APIResponse(responseCode = "200", description = "Successfully retrieved list",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = Product.class)))
  })
  public List<Product> get() {
    return productRepository.listAll(Sort.by("name"));
  }

  @GET
  @Path("{id}")
  @Operation(summary = "Get product by ID", description = "Returns a single product for the given ID.")
  @APIResponses(value = {
      @APIResponse(responseCode = "200", description = "Product found",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = Product.class))),
      @APIResponse(responseCode = "404", description = "Product not found")
  })
  public Product getSingle(
      @Parameter(description = "ID of the product to retrieve", required = true) Long id) {
    Product entity = productRepository.findById(id);
    if (entity == null) {
      throw new WebApplicationException("Product with id of " + id + " does not exist.", 404);
    }
    return entity;
  }

  @POST
  @Transactional
  @Operation(summary = "Create a new product", description = "Creates a new product in the system.")
  @APIResponses(value = {
      @APIResponse(responseCode = "201", description = "Product created successfully",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = Product.class))),
      @APIResponse(responseCode = "422", description = "Invalid input")
  })
  public Response create(
      @Parameter(description = "Product object that needs to be added", required = true) Product product) {
    if (product.id != null) {
      throw new WebApplicationException("Id was invalidly set on request.", 422);
    }

    productRepository.persist(product);
    return Response.ok(product).status(201).build();
  }

  @PUT
  @Path("{id}")
  @Transactional
  @Operation(summary = "Update an existing product", description = "Updates an existing product in the system by ID.")
  @APIResponses(value = {
      @APIResponse(responseCode = "200", description = "Product updated successfully",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = Product.class))),
      @APIResponse(responseCode = "404", description = "Product not found"),
      @APIResponse(responseCode = "422", description = "Invalid input")
  })
  public Product update(
      @Parameter(description = "ID of the product to update", required = true) Long id,
      @Parameter(description = "Updated product object", required = true) Product product) {
    if (product.name == null) {
      throw new WebApplicationException("Product Name was not set on request.", 422);
    }

    Product entity = productRepository.findById(id);

    if (entity == null) {
      throw new WebApplicationException("Product with id of " + id + " does not exist.", 404);
    }

    entity.name = product.name;
    entity.description = product.description;
    entity.price = product.price;
    entity.stock = product.stock;

    productRepository.persist(entity);

    return entity;
  }

  @DELETE
  @Path("{id}")
  @Transactional
  @Operation(summary = "Delete a product", description = "Deletes a product from the system by ID.")
  @APIResponses(value = {
      @APIResponse(responseCode = "204", description = "Product deleted successfully"),
      @APIResponse(responseCode = "404", description = "Product not found")
  })
  public Response delete(@Parameter(description = "ID of the product to delete", required = true) Long id) {
    Product entity = productRepository.findById(id);
    if (entity == null) {
      throw new WebApplicationException("Product with id of " + id + " does not exist.", 404);
    }
    productRepository.delete(entity);
    return Response.status(204).build();
  }

  @Provider
  public static class ErrorMapper implements ExceptionMapper<Exception> {

    @Inject ObjectMapper objectMapper;

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
