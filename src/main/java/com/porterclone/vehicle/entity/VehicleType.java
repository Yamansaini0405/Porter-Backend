package com.porterclone.vehicle.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_types")
@Getter
@Setter
@NoArgsConstructor
public class VehicleType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "capacity_kg")
    private BigDecimal capacityKg;

    @Column(name = "base_fare", nullable = false)
    private BigDecimal baseFare;

    @Column(name = "per_km_rate", nullable = false)
    private BigDecimal perKmRate;

    @Column(name = "per_min_rate", nullable = false)
    private BigDecimal perMinRate;

    @Column(name = "free_wait_minutes", nullable = false)
    private int freeWaitMinutes = 5;

    @Column(name = "wait_charge_per_min", nullable = false)
    private BigDecimal waitChargePerMin;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}