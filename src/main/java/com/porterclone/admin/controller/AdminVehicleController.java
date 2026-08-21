package com.porterclone.admin.controller;

import com.porterclone.common.ApiResponse;
import com.porterclone.vehicle.entity.Vehicle;
import com.porterclone.vehicle.service.VehicleService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/vehicles")
public class AdminVehicleController {

    private final VehicleService vehicleService;

    public AdminVehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping("/{vehicleId}/verify")
    public ApiResponse<Vehicle> verify(@PathVariable Long vehicleId, @RequestParam boolean approved) {
        return ApiResponse.ok(vehicleService.verifyVehicle(vehicleId, approved));
    }
}
