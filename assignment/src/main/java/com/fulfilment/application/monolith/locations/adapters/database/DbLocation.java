package com.fulfilment.application.monolith.locations.adapters.database;

import com.fulfilment.application.monolith.locations.domain.models.Location;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "location")
@Cacheable
public class DbLocation extends PanacheEntity {

  @Column(length = 16, unique = true)
  public String identification;

  // maximum number of warehouses that can be created in this location
  public int maxNumberOfWarehouses;

  // maximum capacity of the location summing all the warehouse capacities
  public int maxCapacity;

  public DbLocation() {
  }

  public Location toLocation() {
    return new Location(
        this.identification,
        this.maxNumberOfWarehouses,
        this.maxCapacity);
  }
}