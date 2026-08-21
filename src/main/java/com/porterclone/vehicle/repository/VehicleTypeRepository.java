package com.porterclone.vehicle.repository;

import com.porterclone.vehicle.entity.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleTypeRepository extends JpaRepository<VehicleType, Long> {
    List<VehicleType> findByActiveTrue();
}
