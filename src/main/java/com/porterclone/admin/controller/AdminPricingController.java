package com.porterclone.admin.controller;

import com.porterclone.admin.dto.CommissionConfigRequest;
import com.porterclone.admin.dto.CreateVehicleTypeRequest;
import com.porterclone.commission.entity.CommissionConfig;
import com.porterclone.commission.repository.CommissionConfigRepository;
import com.porterclone.common.ApiResponse;
import com.porterclone.common.exception.ApiException;
import com.porterclone.vehicle.entity.VehicleType;
import com.porterclone.vehicle.repository.VehicleTypeRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/** Admin panel: manage vehicle type categories, their pricing, and platform commission. */
@RestController
@RequestMapping("/api/v1/admin/pricing")
public class AdminPricingController {

    private final VehicleTypeRepository vehicleTypeRepository;
    private final CommissionConfigRepository commissionConfigRepository;

    public AdminPricingController(VehicleTypeRepository vehicleTypeRepository,
                                   CommissionConfigRepository commissionConfigRepository) {
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.commissionConfigRepository = commissionConfigRepository;
    }

    /** Creates a brand new vehicle type category (e.g. "PICKUP_TRUCK"). Immediately visible to customers. */
    @PostMapping("/vehicle-types")
    public ApiResponse<VehicleType> createVehicleType(@Valid @RequestBody CreateVehicleTypeRequest request) {
        if (vehicleTypeRepository.findByActiveTrue().stream().anyMatch(vt -> vt.getName().equalsIgnoreCase(request.name()))) {
            throw ApiException.conflict("VEHICLE_TYPE_EXISTS", "A vehicle type named '" + request.name() + "' already exists");
        }
        VehicleType vehicleType = new VehicleType();
        vehicleType.setName(request.name());
        vehicleType.setCapacityKg(request.capacityKg());
        vehicleType.setBaseFare(request.baseFare());
        vehicleType.setPerKmRate(request.perKmRate());
        vehicleType.setPerMinRate(request.perMinRate());
        vehicleType.setFreeWaitMinutes(request.freeWaitMinutes());
        vehicleType.setWaitChargePerMin(request.waitChargePerMin());
        vehicleType.setActive(true);
        return ApiResponse.ok(vehicleTypeRepository.save(vehicleType));
    }

    @PutMapping("/vehicle-types/{id}")
    public ApiResponse<VehicleType> updatePricing(@PathVariable Long id, @RequestBody VehicleType updated) {
        VehicleType existing = vehicleTypeRepository.findById(id).orElseThrow();
        existing.setBaseFare(updated.getBaseFare());
        existing.setPerKmRate(updated.getPerKmRate());
        existing.setPerMinRate(updated.getPerMinRate());
        existing.setFreeWaitMinutes(updated.getFreeWaitMinutes());
        existing.setWaitChargePerMin(updated.getWaitChargePerMin());
        return ApiResponse.ok(vehicleTypeRepository.save(existing));
    }

    /**
     * Creates a new commission config effective from now. Does NOT edit existing rows —
     * historical trips must keep reporting the rate that applied when they happened.
     */
    @PostMapping("/commission")
    public ApiResponse<CommissionConfig> setCommission(@Valid @RequestBody CommissionConfigRequest request) {
        CommissionConfig config = new CommissionConfig();
        config.setVehicleTypeId(request.vehicleTypeId());
        config.setCommissionType(request.commissionType());
        config.setValue(request.value());
        config.setEffectiveFrom(LocalDateTime.now());
        config.setActive(true);
        return ApiResponse.ok(commissionConfigRepository.save(config));
    }
}
