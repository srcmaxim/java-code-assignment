package com.fulfilment.application.monolith.locations.adapters.restapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fulfilment.application.monolith.locations.domain.models.Location;
import com.fulfilment.application.monolith.locations.domain.ports.LocationResolverOperation;
import com.fulfilment.application.monolith.locations.domain.ports.LocationResource;
import com.fulfilment.application.monolith.stores.StoreResource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
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

@Path("location")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class LocationResourceImpl implements LocationResource {

  private static final Logger LOGGER = Logger.getLogger(StoreResource.class.getName());

  @Inject
  private LocationResolverOperation locationResolverOperation;

  @GET
  @Path("{identifier}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Get location by identifier", description = "Returns a location for the given identifier")
  @APIResponses(value = {
      @APIResponse(responseCode = "200", description = "Successful, location found",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = Location.class))),
      @APIResponse(responseCode = "400", description = "Wrong identifier"),
      @APIResponse(responseCode = "404", description = "Location not found"),
      @APIResponse(responseCode = "500", description = "Internal server error")
  })
  public Location getByIdentifier(
      @Parameter(description = "Identifier of the location", required = true) String identifier) {
    var location = locationResolverOperation.resolveByIdentifier(identifier);
    if (location == null) {
      throw new WebApplicationException("Location not found", 404);
    }
    return location;
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
