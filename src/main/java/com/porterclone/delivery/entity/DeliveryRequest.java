package com.porterclone.delivery.entity;

import com.porterclone.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_requests")
@Getter
@Setter
@NoArgsConstructor
public class DeliveryRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "rider_id")
    private Long riderId;

    @Column(name = "vehicle_type_id", nullable = false)
    private Long vehicleTypeId;

    @Column(name = "pickup_address", nullable = false, length = 500)
    private String pickupAddress;

    @Column(name = "pickup_lat", nullable = false)
    private BigDecimal pickupLat;

    @Column(name = "pickup_lng", nullable = false)
    private BigDecimal pickupLng;

    @Column(name = "drop_address", nullable = false, length = 500)
    private String dropAddress;

    @Column(name = "drop_lat", nullable = false)
    private BigDecimal dropLat;

    @Column(name = "drop_lng", nullable = false)
    private BigDecimal dropLng;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TripStatus status = TripStatus.REQUESTED;

    @Column(name = "estimated_fare")
    private BigDecimal estimatedFare;

    @Column(name = "final_fare")
    private BigDecimal finalFare;

    @Column(name = "estimated_distance_km")
    private BigDecimal estimatedDistanceKm;

    @Column(name = "actual_distance_km")
    private BigDecimal actualDistanceKm;

    @Column(name = "otp_code", length = 6)
    private String otpCode;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "rider_arrived_at")
    private LocalDateTime riderArrivedAt;

    @Column(name = "trip_started_at")
    private LocalDateTime tripStartedAt;

    @Column(name = "trip_completed_at")
    private LocalDateTime tripCompletedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by", length = 20)
    private String cancelledBy;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;
}
