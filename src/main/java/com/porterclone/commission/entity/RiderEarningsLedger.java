package com.porterclone.commission.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Append-only ledger — never update/delete a row. Corrections are new rows, like real accounting. */
@Entity
@Table(name = "rider_earnings_ledger")
@Getter
@Setter
@NoArgsConstructor
public class RiderEarningsLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rider_id", nullable = false)
    private Long riderId;

    @Column(name = "trip_id")
    private Long tripId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LedgerEntryType type;

    @Column(name = "balance_after", nullable = false)
    private BigDecimal balanceAfter;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
