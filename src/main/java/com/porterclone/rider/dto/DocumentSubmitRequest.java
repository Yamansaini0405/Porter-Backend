package com.porterclone.rider.dto;

import com.porterclone.rider.entity.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DocumentSubmitRequest(
        @NotNull DocumentType docType,
        @NotBlank String fileUrl
) {}
