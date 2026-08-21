package com.porterclone.vehicle.controller;

import com.porterclone.common.ApiResponse;
import com.porterclone.vehicle.entity.Vehicle;
import com.porterclone.vehicle.entity.VehicleType;
import com.porterclone.vehicle.service.VehicleService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    /** Public — customer app needs this to show vehicle options before requesting a delivery. */
    @GetMapping("/vehicle-types")
    public ApiResponse<List<VehicleType>> listVehicleTypes() {
        return ApiResponse.ok(vehicleService.listActiveVehicleTypes());
    }

    public record RegisterVehicleRequest(
            @NotNull Long vehicleTypeId,
            @NotBlank String registrationNumber,
            String model,
            Integer year
    ) {}

    @PostMapping("/riders/{riderId}/vehicles")
    public ApiResponse<Vehicle> registerVehicle(@PathVariable Long riderId,
                                                 @RequestBody RegisterVehicleRequest request) {
        return ApiResponse.ok(vehicleService.registerVehicle(
                riderId, request.vehicleTypeId(), request.registrationNumber(), request.model(), request.year()));
    }
}
