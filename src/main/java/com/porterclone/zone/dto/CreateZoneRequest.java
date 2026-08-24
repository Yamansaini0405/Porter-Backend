package com.porterclone.zone.dto;
import com.porterclone.zone.entity.*;
import jakarta.validation.constraints.*;
public record CreateZoneRequest(
 @NotBlank @Size(max=120) String name,
 @NotBlank @Size(max=50) String code,
 @Size(max=1000) String description,
 @NotNull BoundaryType boundaryType,
 @NotBlank String boundaryCoordinates,
 ZoneStatus status, Boolean isServiceable,
 @NotNull @Min(0) Integer priority) {}
