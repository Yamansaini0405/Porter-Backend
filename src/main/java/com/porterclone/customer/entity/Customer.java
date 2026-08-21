package com.porterclone.customer.entity;

import com.porterclone.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
public class Customer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "rating_avg")
    private BigDecimal ratingAvg = new BigDecimal("5.00");

    @Column(name = "total_trips")
    private int totalTrips = 0;
}
