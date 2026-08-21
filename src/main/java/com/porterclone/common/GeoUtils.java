package com.porterclone.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Straight-line (Haversine) distance — used as a placeholder for estimates.
 * In production, swap this for Google Distance Matrix API or a self-hosted OSRM instance,
 * which give actual road distance/duration instead of "as the crow flies".
 */
public final class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtils() {}

    public static BigDecimal haversineDistanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distanceKm = EARTH_RADIUS_KM * c;
        return BigDecimal.valueOf(distanceKm).setScale(2, RoundingMode.HALF_UP);
    }

    /** Rough duration estimate assuming average city speed of 25 km/h — replace with Maps ETA in production. */
    public static BigDecimal estimateDurationMin(BigDecimal distanceKm) {
        double avgSpeedKmh = 25.0;
        double minutes = distanceKm.doubleValue() / avgSpeedKmh * 60.0;
        return BigDecimal.valueOf(minutes).setScale(2, RoundingMode.HALF_UP);
    }
}
