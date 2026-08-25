package com.porterclone.admin.controller;

import com.porterclone.admin.dto.CommissionConfigRequest;
import com.porterclone.common.ApiResponse;
import com.porterclone.common.exception.ApiException;
import com.porterclone.common.service.FileStorageService;
import com.porterclone.commission.entity.CommissionConfig;
import com.porterclone.commission.repository.CommissionConfigRepository;
import com.porterclone.vehicle.entity.VehicleType;
import com.porterclone.vehicle.repository.VehicleTypeRepository;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin/pricing")
public class AdminPricingController {

    private final VehicleTypeRepository vehicleTypeRepository;
    private final CommissionConfigRepository commissionConfigRepository;
    private final FileStorageService fileStorageService;

    public AdminPricingController(
            VehicleTypeRepository vehicleTypeRepository,
            CommissionConfigRepository commissionConfigRepository,
            FileStorageService fileStorageService
    ) {
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.commissionConfigRepository = commissionConfigRepository;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Creates a vehicle type with an optional image.
     *
     * Content-Type: multipart/form-data
     */
    @PostMapping(
            value = "/vehicle-types",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<VehicleType> createVehicleType(
            @RequestParam String name,
            @RequestParam(required = false) BigDecimal capacityKg,
            @RequestParam BigDecimal baseFare,
            @RequestParam BigDecimal perKmRate,
            @RequestParam BigDecimal perMinRate,
            @RequestParam(defaultValue = "5") int freeWaitMinutes,
            @RequestParam BigDecimal waitChargePerMin,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {

        if (vehicleTypeRepository.findByActiveTrue()
                .stream()
                .anyMatch(vt -> vt.getName().equalsIgnoreCase(name))) {

            throw ApiException.conflict(
                    "VEHICLE_TYPE_EXISTS",
                    "A vehicle type named '" + name + "' already exists"
            );
        }

        VehicleType vehicleType = new VehicleType();

        vehicleType.setName(name);
        vehicleType.setCapacityKg(capacityKg);
        vehicleType.setBaseFare(baseFare);
        vehicleType.setPerKmRate(perKmRate);
        vehicleType.setPerMinRate(perMinRate);
        vehicleType.setFreeWaitMinutes(freeWaitMinutes);
        vehicleType.setWaitChargePerMin(waitChargePerMin);
        vehicleType.setActive(true);
        vehicleType.setCreatedAt(LocalDateTime.now());
        vehicleType.setUpdatedAt(LocalDateTime.now());

        if (image != null && !image.isEmpty()) {
            vehicleType.setImageUrl(
                    fileStorageService.storeVehicleTypeImage(image)
            );
        }

        return ApiResponse.ok(vehicleTypeRepository.save(vehicleType));
    }

    /**
     * Updates vehicle type pricing and optionally its image.
     *
     * Content-Type: multipart/form-data
     */
    @PutMapping(
            value = "/vehicle-types/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<VehicleType> updatePricing(
            @PathVariable Long id,
            @RequestParam(required = false) BigDecimal baseFare,
            @RequestParam(required = false) BigDecimal perKmRate,
            @RequestParam(required = false) BigDecimal perMinRate,
            @RequestParam(required = false) Integer freeWaitMinutes,
            @RequestParam(required = false) BigDecimal waitChargePerMin,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {

        VehicleType existing = vehicleTypeRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(
                        "VEHICLE_TYPE_NOT_FOUND",
                        "Vehicle type not found"
                ));

        if (baseFare != null) {
            existing.setBaseFare(baseFare);
        }

        if (perKmRate != null) {
            existing.setPerKmRate(perKmRate);
        }

        if (perMinRate != null) {
            existing.setPerMinRate(perMinRate);
        }

        if (freeWaitMinutes != null) {
            existing.setFreeWaitMinutes(freeWaitMinutes);
        }

        if (waitChargePerMin != null) {
            existing.setWaitChargePerMin(waitChargePerMin);
        }

        if (image != null && !image.isEmpty()) {
            existing.setImageUrl(
                    fileStorageService.storeVehicleTypeImage(image)
            );
        }

        existing.setUpdatedAt(LocalDateTime.now());

        return ApiResponse.ok(vehicleTypeRepository.save(existing));
    }

    /**
     * Creates a new commission config effective from now.
     */
    @PostMapping("/commission")
    public ApiResponse<CommissionConfig> setCommission(
            @Valid @RequestBody CommissionConfigRequest request
    ) {

        CommissionConfig config = new CommissionConfig();

        config.setVehicleTypeId(request.vehicleTypeId());
        config.setCommissionType(request.commissionType());
        config.setValue(request.value());
        config.setEffectiveFrom(LocalDateTime.now());
        config.setActive(true);

        return ApiResponse.ok(
                commissionConfigRepository.save(config)
        );
    }
}