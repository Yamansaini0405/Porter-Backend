package com.porterclone.admin.controller;

import com.porterclone.common.ApiResponse;
import com.porterclone.zone.dto.*;
import com.porterclone.zone.entity.Zone;
import com.porterclone.zone.service.ZoneService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/zones")
public class AdminZoneController {
    private final ZoneService zoneService;

    public AdminZoneController(ZoneService zoneService) { this.zoneService = zoneService; }

    @PostMapping
    public ApiResponse<Zone> create(@Valid @RequestBody CreateZoneRequest request) {
        return ApiResponse.ok(zoneService.create(request));
    }

    @GetMapping
    public ApiResponse<List<Zone>> getAll() {
        return ApiResponse.ok(zoneService.getAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<Zone> get(@PathVariable Long id) {
        return ApiResponse.ok(zoneService.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Zone> update(@PathVariable Long id, @Valid @RequestBody UpdateZoneRequest request) {
        return ApiResponse.ok(zoneService.update(id, request));
    }

    @PatchMapping("/{id}/activate")
    public ApiResponse<Zone> activate(@PathVariable Long id) {
        return ApiResponse.ok(zoneService.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    public ApiResponse<Zone> deactivate(@PathVariable Long id) {
        return ApiResponse.ok(zoneService.deactivate(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        zoneService.delete(id);
        return ApiResponse.ok(null, "Zone deactivated and soft-deleted");
    }
}
