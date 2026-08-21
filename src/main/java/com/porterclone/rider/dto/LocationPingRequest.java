package com.porterclone.rider.dto;

import jakarta.validation.constraints.NotNull;

/**
 * vehicleTypeId is deliberately NOT part of this request — it's resolved server-side
 * from the rider's selected active vehicle (see RiderLocationService.updateLocation()).
 * Trusting a client-supplied vehicle type here would let a rider claim to be driving
 * anything on any given ping, regardless of what they actually registered/got verified for.
 */
public record LocationPingRequest(
        @NotNull Double lat,
        @NotNull Double lng,
        Long activeTripId          // optional — if set, location is also broadcast to the customer tracking this trip
) {}
