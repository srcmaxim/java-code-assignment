package com.fulfilment.application.monolith.locations.domain.models;

public record Location(
    String identification,
    // maximum number of warehouses that can be created in this location
    int maxNumberOfWarehouses,
    // maximum capacity of the location summing all the warehouse capacities
    int maxCapacity) {
}
