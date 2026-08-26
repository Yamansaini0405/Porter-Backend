package com.porterclone.admin.controller;

import com.porterclone.common.ApiResponse;
import com.porterclone.vehicle.entity.Vehicle;
import com.porterclone.vehicle.entity.VehicleType;
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

    @GetMapping("vehicleType")
    public ApiResponse<?> getVehicleTypes() {
        return ApiResponse.ok(vehicleService.getAllVehicleTypes());
    }

    @GetMapping("vehicleType/{vehicleTypeId}")
    public ApiResponse<VehicleType> getVehicleTypeById(@PathVariable Long vehicleTypeId) {
        return ApiResponse.ok(vehicleService.getVehicleTypeById(vehicleTypeId));
    }

    @PutMapping("vehicleType/{vehicleTypeId}")
    public ApiResponse<VehicleType> updateVehicleType(@PathVariable Long vehicleTypeId, @RequestBody Object vehicleTypeUpdateRequest) {
        return ApiResponse.ok(vehicleService.updateVehicleType(vehicleTypeId, (VehicleType) vehicleTypeUpdateRequest));
    }

    @DeleteMapping("vehicleType/{vehicleTypeId}")
    public ApiResponse<?> deleteVehicleType(@PathVariable Long vehicleTypeId) {
        vehicleService.deleteVehicleType(vehicleTypeId);
        return ApiResponse.ok(null, "Vehicle type deleted successfully");
    }

}
