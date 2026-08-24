package com.porterclone.zone.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
public record PickupDropServiceabilityRequest(@NotNull @Valid LocationRequest pickup, @NotNull @Valid LocationRequest drop) {}
