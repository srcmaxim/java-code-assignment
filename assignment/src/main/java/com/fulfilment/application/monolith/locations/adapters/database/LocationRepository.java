package com.fulfilment.application.monolith.locations.adapters.database;

import com.fulfilment.application.monolith.locations.domain.models.Location;
import com.fulfilment.application.monolith.locations.domain.ports.LocationStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import org.hibernate.LockOptions;
import org.hibernate.cfg.AvailableSettings;

@ApplicationScoped
public class LocationRepository implements LocationStore, PanacheRepository<DbLocation> {

  @Override
  public Location resolveByIdentifier(String identifier) {
    return find("identification", identifier)
        .firstResultOptional()
        .map(DbLocation::toLocation)
        .orElse(null);
  }

  @Override
  public Location lockByIdentifier(String identifier) {
    return find("identification", identifier)
        .withLock(LockModeType.WRITE)
        .withHint(AvailableSettings.JPA_LOCK_TIMEOUT, LockOptions.SKIP_LOCKED)
        .firstResultOptional()
        .map(DbLocation::toLocation)
        .orElse(null);
  }
}
