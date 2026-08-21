package com.porterclone.delivery.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trip_status_history")
@Getter
@Setter
@NoArgsConstructor
public class TripStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TripStatus status;

    private BigDecimal lat;
    private BigDecimal lng;

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt = LocalDateTime.now();
}
