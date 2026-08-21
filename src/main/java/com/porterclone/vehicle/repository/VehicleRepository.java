package com.porterclone.vehicle.repository;

import com.porterclone.vehicle.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByRiderId(Long riderId);
    Optional<Vehicle> findByRegistrationNumber(String registrationNumber);
}
