package com.porterclone.commission.repository;

import com.porterclone.commission.entity.RiderEarningsLedger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiderEarningsLedgerRepository extends JpaRepository<RiderEarningsLedger, Long> {
    List<RiderEarningsLedger> findByRiderIdOrderByCreatedAtDesc(Long riderId);
}
