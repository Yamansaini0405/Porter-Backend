package com.porterclone.rider.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "rider_onboarding_history")
@Getter
@Setter
@NoArgsConstructor
public class RiderOnboardingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rider_id", nullable = false)
    private Long riderId;

    @Column(name = "from_status", length = 30)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 30)
    private String toStatus;

    @Column(name = "changed_by", length = 100)
    private String changedBy;

    @Column(length = 500)
    private String remarks;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
