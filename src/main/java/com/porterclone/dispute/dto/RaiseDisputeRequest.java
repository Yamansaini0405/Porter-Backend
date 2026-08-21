package com.porterclone.dispute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RaiseDisputeRequest(
        @NotNull Long tripId,
        @NotBlank String raisedBy,     // CUSTOMER or RIDER
        String category,
        String description
) {}
