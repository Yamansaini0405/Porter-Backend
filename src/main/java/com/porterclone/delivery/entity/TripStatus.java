package com.porterclone.delivery.entity;

/**
 * Delivery/trip lifecycle:
 * REQUESTED -> SEARCHING_RIDER -> RIDER_ASSIGNED -> RIDER_ARRIVED
 *   -> WAITING_AT_PICKUP (optional, if free wait time exceeded) -> IN_TRANSIT
 *   -> ARRIVED_AT_DROP -> COMPLETED
 *
 * Exits: NO_RIDER_FOUND, CANCELLED_BY_CUSTOMER, CANCELLED_BY_RIDER, EXPIRED
 */
public enum TripStatus {
    REQUESTED,
    SEARCHING_RIDER,
    RIDER_ASSIGNED,
    RIDER_ARRIVED,
    WAITING_AT_PICKUP,
    IN_TRANSIT,
    ARRIVED_AT_DROP,
    COMPLETED,
    CANCELLED_BY_CUSTOMER,
    CANCELLED_BY_RIDER,
    NO_RIDER_FOUND,
    EXPIRED
}
