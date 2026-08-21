package com.porterclone.pricing.repository;

import com.porterclone.pricing.entity.FareDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FareDetailsRepository extends JpaRepository<FareDetails, Long> {
    Optional<FareDetails> findByTripId(Long tripId);
}
