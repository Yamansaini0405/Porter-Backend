package com.porterclone.commission.repository;

import com.porterclone.commission.entity.CommissionConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CommissionConfigRepository extends JpaRepository<CommissionConfig, Long> {

    @org.springframework.data.jpa.repository.Query("""
        SELECT c FROM CommissionConfig c
        WHERE c.vehicleTypeId = :vehicleTypeId
          AND c.active = true
          AND c.effectiveFrom <= :at
          AND (c.effectiveTo IS NULL OR c.effectiveTo > :at)
        ORDER BY c.effectiveFrom DESC
        """)
    List<CommissionConfig> findApplicableForVehicleType(Long vehicleTypeId, LocalDateTime at);

    @org.springframework.data.jpa.repository.Query("""
        SELECT c FROM CommissionConfig c
        WHERE c.vehicleTypeId IS NULL
          AND c.active = true
          AND c.effectiveFrom <= :at
          AND (c.effectiveTo IS NULL OR c.effectiveTo > :at)
        ORDER BY c.effectiveFrom DESC
        """)
    List<CommissionConfig> findApplicableGlobal(LocalDateTime at);
}
