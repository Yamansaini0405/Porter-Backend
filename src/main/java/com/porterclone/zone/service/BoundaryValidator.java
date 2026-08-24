package com.porterclone.zone.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.porterclone.common.exception.ApiException;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;

@Component
public class BoundaryValidator {
    private final ObjectMapper objectMapper;
    public BoundaryValidator(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }

    public void validatePolygon(String json) {
        final List<List<BigDecimal>> points;
        try {
            points = objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(
                    List.class, objectMapper.getTypeFactory().constructCollectionType(List.class, BigDecimal.class)));
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            throw ApiException.badRequest("INVALID_BOUNDARY", "boundaryCoordinates must be a JSON array of [latitude, longitude] pairs");
        }
        if (points == null || points.size() < 3)
            throw ApiException.badRequest("INVALID_BOUNDARY", "A polygon requires at least 3 coordinate points");
        for (List<BigDecimal> point : points) {
            if (point == null || point.size() != 2 || point.get(0) == null || point.get(1) == null)
                throw ApiException.badRequest("INVALID_BOUNDARY", "Every polygon coordinate must contain exactly [latitude, longitude]");
            double lat = point.get(0).doubleValue(), lng = point.get(1).doubleValue();
            if (lat < -90 || lat > 90 || lng < -180 || lng > 180)
                throw ApiException.badRequest("INVALID_BOUNDARY", "Polygon coordinates must use latitude -90..90 and longitude -180..180");
        }
    }
}
