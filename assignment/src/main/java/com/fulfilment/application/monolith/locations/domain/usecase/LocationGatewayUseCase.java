package com.fulfilment.application.monolith.locations.domain.usecase;

import com.fulfilment.application.monolith.locations.adapters.database.LocationRepository;
import com.fulfilment.application.monolith.locations.domain.models.Location;
import com.fulfilment.application.monolith.locations.domain.ports.LocationResolverOperation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import java.util.function.Predicate;
import java.util.regex.Pattern;

@ApplicationScoped
public class LocationGatewayUseCase implements LocationResolverOperation {

  private static final String IDENTIFICATION_NOT_VALID =
      "Location should be in form PLACE-number, example: 'ZWOLLE-001'";
  private static final Predicate<String> IDENTIFICATION_PATTERN =
      Pattern.compile("^[A-Z]{3,12}-[0-9]{3}$").asPredicate();

  @Inject
  private LocationRepository locationRepository;

  @Override
  public Location resolveByIdentifier(String identifier) {
    if (!validIdentifier(identifier)) {
      throw new WebApplicationException(IDENTIFICATION_NOT_VALID, 400);
    }
    return locationRepository.resolveByIdentifier(identifier);
  }

  @Override
  public Location lockByIdentifier(String identifier) {
    if (!validIdentifier(identifier)) {
      throw new WebApplicationException(IDENTIFICATION_NOT_VALID, 400);
    }
    return locationRepository.lockByIdentifier(identifier);
  }

  private static boolean validIdentifier(String identifier) {
    return identifier != null
        && !identifier.isEmpty()
        && IDENTIFICATION_PATTERN.test(identifier);
  }
}
