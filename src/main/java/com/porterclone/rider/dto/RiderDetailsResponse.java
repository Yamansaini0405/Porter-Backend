package com.porterclone.rider.dto;

import com.porterclone.rider.entity.Rider;
import com.porterclone.rider.entity.RiderDocument;

import java.util.List;

public record RiderDetailsResponse(
        Rider rider,
        List<RiderDocument> documents
) {}
