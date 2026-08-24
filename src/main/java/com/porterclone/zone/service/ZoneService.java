package com.porterclone.zone.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.porterclone.common.exception.ApiException;
import com.porterclone.delivery.repository.DeliveryRequestRepository;
import com.porterclone.zone.dto.*;
import com.porterclone.zone.entity.*;
import com.porterclone.zone.repository.ZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ZoneService {

    private final ZoneRepository zoneRepository;
    private final BoundaryValidator boundaryValidator;
    private final ObjectMapper objectMapper;
    private final DeliveryRequestRepository deliveryRequestRepository;

    public ZoneService(ZoneRepository zoneRepository,
                       BoundaryValidator boundaryValidator,
                       ObjectMapper objectMapper,
                       DeliveryRequestRepository deliveryRequestRepository) {
        this.zoneRepository = zoneRepository;
        this.deliveryRequestRepository = deliveryRequestRepository;
        this.boundaryValidator = boundaryValidator;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<Zone> getAll() {
        return zoneRepository.findAllByDeletedAtIsNullOrderByPriorityDescNameAsc();
    }

    @Transactional(readOnly = true)
    public Zone get(Long id) {
        return zoneRepository.findById(id)
                .filter(z -> z.getDeletedAt() == null)
                .orElseThrow(() -> ApiException.notFound("ZONE_NOT_FOUND", "Zone not found: " + id));
    }

    @Transactional
    public Zone create(CreateZoneRequest request) {
        String code = normalizeCode(request.code());
        if (zoneRepository.existsByCode(code)) {
            throw ApiException.conflict("ZONE_CODE_EXISTS", "A zone with code '" + code + "' already exists");
        }
        validateBoundary(request.boundaryType(), request.boundaryCoordinates());

        Zone zone = new Zone();
        zone.setName(request.name().trim());
        zone.setCode(code);
        zone.setDescription(request.description());
        zone.setStatus(request.status() == null ? ZoneStatus.ACTIVE : request.status());
        zone.setServiceable(request.isServiceable() == null || request.isServiceable());
        zone.setPriority(request.priority());
        zone.setBoundaryType(request.boundaryType());
        zone.setBoundaryCoordinates(request.boundaryCoordinates());
        return zoneRepository.save(zone);
    }

    @Transactional
    public Zone update(Long id, UpdateZoneRequest request) {
        Zone zone = get(id);
        String code = normalizeCode(request.code());
        if (zoneRepository.existsByCodeAndIdNot(code, id)) {
            throw ApiException.conflict("ZONE_CODE_EXISTS", "A zone with code '" + code + "' already exists");
        }
        validateBoundary(request.boundaryType(), request.boundaryCoordinates());

        zone.setName(request.name().trim());
        zone.setCode(code);
        zone.setDescription(request.description());
        zone.setStatus(request.status());
        zone.setServiceable(request.isServiceable());
        zone.setPriority(request.priority());
        zone.setBoundaryType(request.boundaryType());
        zone.setBoundaryCoordinates(request.boundaryCoordinates());
        return zoneRepository.save(zone);
    }

    @Transactional
    public Zone activate(Long id) {
        Zone zone = get(id);
        zone.setStatus(ZoneStatus.ACTIVE);
        zone.setServiceable(true);
        return zoneRepository.save(zone);
    }

    @Transactional
    public Zone deactivate(Long id) {
        Zone zone = get(id);
        zone.setStatus(ZoneStatus.INACTIVE);
        return zoneRepository.save(zone);
    }

    @Transactional
    public void delete(Long id) {
        Zone zone = get(id);
        // Delivery history must never lose its zone reference. Always soft-delete.
        zone.setStatus(ZoneStatus.INACTIVE);
        zone.setServiceable(false);
        zone.setDeletedAt(LocalDateTime.now());
        zoneRepository.save(zone);
    }

    @Transactional(readOnly = true)
    public Optional<Zone> resolveServiceableZone(BigDecimal latitude, BigDecimal longitude) {
        double lat = latitude.doubleValue();
        double lng = longitude.doubleValue();

        for (Zone zone : zoneRepository.findServiceableZones(ZoneStatus.ACTIVE)) {
            if (contains(zone, lat, lng)) {
                return Optional.of(zone);
            }
        }
        return Optional.empty();
    }

    private boolean contains(Zone zone, double latitude, double longitude) {
        try {
            List<List<BigDecimal>> points = objectMapper.readValue(
                    zone.getBoundaryCoordinates(),
                    new TypeReference<List<List<BigDecimal>>>() {});
            boolean inside = false;
            for (int i = 0, j = points.size() - 1; i < points.size(); j = i++) {
                double yi = points.get(i).get(0).doubleValue();
                double xi = points.get(i).get(1).doubleValue();
                double yj = points.get(j).get(0).doubleValue();
                double xj = points.get(j).get(1).doubleValue();

                if (onSegment(latitude, longitude, yi, xi, yj, xj)) {
                    return true;
                }

                boolean intersects = ((yi > latitude) != (yj > latitude))
                        && (longitude < (xj - xi) * (latitude - yi) / (yj - yi) + xi);
                if (intersects) inside = !inside;
            }
            return inside;
        } catch (Exception ex) {
            throw ApiException.badRequest("INVALID_ZONE_BOUNDARY",
                    "Zone " + zone.getCode() + " contains invalid boundary coordinates");
        }
    }

    private boolean onSegment(double lat, double lng, double lat1, double lng1, double lat2, double lng2) {
        double cross = (lng - lng1) * (lat2 - lat1) - (lat - lat1) * (lng2 - lng1);
        if (Math.abs(cross) > 1e-10) return false;
        return lat >= Math.min(lat1, lat2) - 1e-10 && lat <= Math.max(lat1, lat2) + 1e-10
                && lng >= Math.min(lng1, lng2) - 1e-10 && lng <= Math.max(lng1, lng2) + 1e-10;
    }

    private void validateBoundary(BoundaryType type, String coordinates) {
        if (type == null) throw ApiException.badRequest("INVALID_BOUNDARY", "boundaryType is required");
        if (type == BoundaryType.POLYGON) boundaryValidator.validatePolygon(coordinates);
        else throw ApiException.badRequest("UNSUPPORTED_BOUNDARY_TYPE", "Unsupported boundary type: " + type);
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
