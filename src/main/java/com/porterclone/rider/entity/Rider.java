package com.porterclone.rider.entity;

import com.porterclone.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "riders")
@Getter
@Setter
@NoArgsConstructor
public class Rider extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 150)
    private String name;

    private LocalDate dob;

    private String gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_status", nullable = false, length = 30)
    private OnboardingStatus onboardingStatus = OnboardingStatus.REGISTERED;

    @Enumerated(EnumType.STRING)
    @Column(name = "online_status", nullable = false, length = 20)
    private OnlineStatus onlineStatus = OnlineStatus.OFFLINE;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false, length = 20)
    private AvailabilityStatus availabilityStatus = AvailabilityStatus.AVAILABLE;

    @Column(name = "current_vehicle_id")
    private Long currentVehicleId;

    @Column(name = "rating_avg")
    private BigDecimal ratingAvg = new BigDecimal("5.00");

    @Column(name = "total_trips")
    private int totalTrips = 0;

    @Column(name = "wallet_balance")
    private BigDecimal walletBalance = BigDecimal.ZERO;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by_admin_id")
    private Long approvedByAdminId;
}
