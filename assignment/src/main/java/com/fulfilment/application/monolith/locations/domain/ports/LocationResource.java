package com.fulfilment.application.monolith.locations.domain.ports;

import com.fulfilment.application.monolith.locations.domain.models.Location;

public interface LocationResource {
  Location getByIdentifier(String identifier);
}