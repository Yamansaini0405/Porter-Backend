package com.porterclone.commission.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "commission_configs")
@Getter
@Setter
@NoArgsConstructor
public class CommissionConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** NULL = global default, applies to any vehicle type without a more specific config. */
    @Column(name = "vehicle_type_id")
    private Long vehicleTypeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "commission_type", nullable = false, length = 20)
    private CommissionType commissionType;

    @Column(nullable = false)
    private BigDecimal value;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
