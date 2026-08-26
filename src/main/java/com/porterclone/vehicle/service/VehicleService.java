package com.porterclone.vehicle.service;

import com.porterclone.common.exception.ApiException;
import com.porterclone.rider.entity.Rider;
import com.porterclone.rider.repository.RiderRepository;
import com.porterclone.vehicle.entity.Vehicle;
import com.porterclone.vehicle.entity.VehicleType;
import com.porterclone.vehicle.entity.VerificationStatus;
import com.porterclone.vehicle.repository.VehicleRepository;
import com.porterclone.vehicle.repository.VehicleTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final RiderRepository riderRepository;

    public VehicleService(VehicleRepository vehicleRepository, VehicleTypeRepository vehicleTypeRepository,
                           RiderRepository riderRepository) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.riderRepository = riderRepository;
    }

    public List<VehicleType> listActiveVehicleTypes() {
        return vehicleTypeRepository.findByActiveTrue();
    }

    @Transactional
    public Vehicle registerVehicle(Long riderId, Long vehicleTypeId, String regNumber, String model, Integer year) {
        if (vehicleRepository.findByRegistrationNumber(regNumber).isPresent()) {
            throw ApiException.conflict("VEHICLE_ALREADY_REGISTERED", "This registration number is already on file");
        }
        vehicleTypeRepository.findById(vehicleTypeId)
                .orElseThrow(() -> ApiException.notFound("VEHICLE_TYPE_NOT_FOUND", "Invalid vehicle type"));

        Vehicle vehicle = new Vehicle();
        vehicle.setRiderId(riderId);
        vehicle.setVehicleTypeId(vehicleTypeId);
        vehicle.setRegistrationNumber(regNumber);
        vehicle.setModel(model);
        vehicle.setYear(year);
        vehicle.setVerificationStatus(VerificationStatus.PENDING);
        return vehicleRepository.save(vehicle);
    }

    @Transactional
    public Vehicle verifyVehicle(Long vehicleId, boolean approved) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> ApiException.notFound("VEHICLE_NOT_FOUND", "Vehicle not found"));
        vehicle.setVerificationStatus(approved ? VerificationStatus.VERIFIED : VerificationStatus.REJECTED);
        return vehicleRepository.save(vehicle);
    }

    /**
     * Rider picks which of their (verified) vehicles they're driving right now.
     * This is what actually sets Rider.currentVehicleId — required before going online.
     * A rider with two registered vehicles (e.g. a bike and a mini-truck) switches
     * between shifts by calling this again with a different vehicleId.
     */
    @Transactional
    public void selectActiveVehicle(Long riderId, Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> ApiException.notFound("VEHICLE_NOT_FOUND", "Vehicle not found"));

        if (!riderId.equals(vehicle.getRiderId())) {
            throw ApiException.forbidden("NOT_YOUR_VEHICLE", "This vehicle is not registered to you");
        }
        if (vehicle.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw ApiException.badRequest("VEHICLE_NOT_VERIFIED", "This vehicle hasn't been verified by admin yet");
        }
        if (!vehicle.isActive()) {
            throw ApiException.badRequest("VEHICLE_INACTIVE", "This vehicle has been deactivated");
        }

        Rider rider = riderRepository.findById(riderId)
                .orElseThrow(() -> ApiException.notFound("RIDER_NOT_FOUND", "Rider not found"));
        rider.setCurrentVehicleId(vehicleId);
        riderRepository.save(rider);
    }

    /** Used by goOnline() and location pings to resolve which vehicle TYPE a rider is currently operating. */
    public Vehicle getActiveVehicleOrThrow(Long riderId, Long currentVehicleId) {
        if (currentVehicleId == null) {
            throw ApiException.badRequest("NO_ACTIVE_VEHICLE", "Select an active vehicle before going online");
        }
        Vehicle vehicle = vehicleRepository.findById(currentVehicleId)
                .orElseThrow(() -> ApiException.notFound("VEHICLE_NOT_FOUND", "Active vehicle no longer exists"));
        if (vehicle.getVerificationStatus() != VerificationStatus.VERIFIED || !vehicle.isActive()) {
            throw ApiException.forbidden("VEHICLE_NOT_VERIFIED", "Your active vehicle is not currently verified/active");
        }
        return vehicle;
    }

    public List<VehicleType> getAllVehicleTypes() {
        return vehicleTypeRepository.findAll();
    }

    public VehicleType getVehicleTypeById(Long vehicleTypeId) {
        return vehicleTypeRepository.findById(vehicleTypeId)
                .orElseThrow(() -> ApiException.notFound("VEHICLE_TYPE_NOT_FOUND", "Vehicle type not found"));
    }

    public VehicleType updateVehicleType(Long vehicleTypeId, VehicleType updatedVehicleType) {
        VehicleType existingVehicleType = vehicleTypeRepository.findById(vehicleTypeId)
                .orElseThrow(() -> ApiException.notFound("VEHICLE_TYPE_NOT_FOUND", "Vehicle type not found"));

        existingVehicleType.setName(updatedVehicleType.getName());
        existingVehicleType.setImageUrl(updatedVehicleType.getImageUrl());
        existingVehicleType.setCapacityKg(updatedVehicleType.getCapacityKg());
        existingVehicleType.setBaseFare(updatedVehicleType.getBaseFare());
        existingVehicleType.setPerKmRate(updatedVehicleType.getPerKmRate());
        existingVehicleType.setPerMinRate(updatedVehicleType.getPerMinRate());
        existingVehicleType.setFreeWaitMinutes(updatedVehicleType.getFreeWaitMinutes());
        existingVehicleType.setWaitChargePerMin(updatedVehicleType.getWaitChargePerMin());
        existingVehicleType.setActive(updatedVehicleType.isActive());

        return vehicleTypeRepository.save(existingVehicleType);
    }

    public void deleteVehicleType(Long vehicleTypeId) {
        VehicleType vehicleType = vehicleTypeRepository.findById(vehicleTypeId)
                .orElseThrow(() -> ApiException.notFound("VEHICLE_TYPE_NOT_FOUND", "Vehicle type not found"));
        vehicleTypeRepository.delete(vehicleType);
    }

}
