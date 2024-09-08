package com.fulfilment.application.monolith.locations.domain.ports;

import com.fulfilment.application.monolith.locations.domain.models.Location;

public interface LocationStore {
  Location resolveByIdentifier(String identifier);

  Location lockByIdentifier(String identifier);
}
