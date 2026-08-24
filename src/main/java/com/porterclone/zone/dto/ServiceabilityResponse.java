package com.porterclone.zone.dto;
public record ServiceabilityResponse(boolean serviceable, Long zoneId, String zoneCode, String zoneName, String message) {}
