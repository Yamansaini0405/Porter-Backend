package com.porterclone.delivery.service;

import com.porterclone.delivery.entity.DeliveryRequest;
import com.porterclone.delivery.entity.TripStatus;
import com.porterclone.delivery.repository.DeliveryRequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Since dispatch() now broadcasts to everyone instead of assigning synchronously, a
 * request can sit in SEARCHING_RIDER forever if nobody accepts. This sweep closes it
 * out after a configurable window (app.matching.broadcast-timeout-seconds) so the
 * customer isn't left staring at "searching..." indefinitely.
 */
@Component
public class DispatchTimeoutScheduler {

    private final DeliveryRequestRepository deliveryRequestRepository;
    private final DeliveryService deliveryService;
    private final long timeoutSeconds;

    public DispatchTimeoutScheduler(DeliveryRequestRepository deliveryRequestRepository,
                                     DeliveryService deliveryService,
                                     @Value("${app.matching.broadcast-timeout-seconds:60}") long timeoutSeconds) {
        this.deliveryRequestRepository = deliveryRequestRepository;
        this.deliveryService = deliveryService;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Scheduled(fixedDelay = 10000)
    public void expireStaleSearches() {
        // Change this:
        LocalDateTime cutoff = LocalDateTime.now(ZoneOffset.UTC).minusSeconds(timeoutSeconds);
        List<DeliveryRequest> stale = deliveryRequestRepository.findByStatusAndRequestedAtBefore(
                TripStatus.SEARCHING_RIDER, cutoff);

        for (DeliveryRequest trip : stale) {
            deliveryService.expireSearch(trip.getId());
        }
    }
}
