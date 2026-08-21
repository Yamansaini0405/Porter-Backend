package com.porterclone.pricing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fare_details")
@Getter
@Setter
@NoArgsConstructor
public class FareDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id", nullable = false, unique = true)
    private Long tripId;

    @Column(name = "base_fare", nullable = false)
    private BigDecimal baseFare = BigDecimal.ZERO;

    @Column(name = "distance_fare", nullable = false)
    private BigDecimal distanceFare = BigDecimal.ZERO;

    @Column(name = "time_fare", nullable = false)
    private BigDecimal timeFare = BigDecimal.ZERO;

    @Column(name = "waiting_charge", nullable = false)
    private BigDecimal waitingCharge = BigDecimal.ZERO;

    @Column(name = "surge_multiplier", nullable = false)
    private BigDecimal surgeMultiplier = BigDecimal.ONE;

    @Column(name = "surge_amount", nullable = false)
    private BigDecimal surgeAmount = BigDecimal.ZERO;

    @Column(name = "toll_charge", nullable = false)
    private BigDecimal tollCharge = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "coupon_code")
    private String couponCode;

    @Column(name = "tax_amount", nullable = false)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "gross_fare", nullable = false)
    private BigDecimal grossFare = BigDecimal.ZERO;

    @Column(name = "commission_amount", nullable = false)
    private BigDecimal commissionAmount = BigDecimal.ZERO;

    @Column(name = "commission_percentage", nullable = false)
    private BigDecimal commissionPercentage = BigDecimal.ZERO;

    @Column(name = "rider_net_earning", nullable = false)
    private BigDecimal riderNetEarning = BigDecimal.ZERO;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
