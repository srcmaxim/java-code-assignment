package com.fulfilment.application.monolith.locations.domain.ports;

import com.fulfilment.application.monolith.locations.domain.models.Location;

public interface LocationResolverOperation {
  Location resolveByIdentifier(String identifier);

  Location lockByIdentifier(String identifier);
}
