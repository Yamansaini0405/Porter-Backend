package com.porterclone.zone.dto;
public record PickupDropServiceabilityResponse(boolean serviceable, ZoneSummaryResponse pickupZone, ZoneSummaryResponse dropZone, String reason) {}
