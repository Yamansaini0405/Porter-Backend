package com.porterclone.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pushes a newly-created delivery request to every eligible nearby rider at once
 * (broadcast, not sequential), and tells the riders who DIDN'T win once someone else
 * has accepted — so their app can drop the request from screen instantly instead of
 * finding out only on the next poll.
 *
 * Each rider has their own personal topic (/topic/rider/{riderId}/requests) rather than
 * one shared topic, so a rider's app only ever sees requests actually meant for them.
 */
@Service
public class RequestBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    public RequestBroadcastService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public record NewRequestNotification(
            Long tripId,
            String pickupAddress, BigDecimal pickupLat, BigDecimal pickupLng,
            String dropAddress,
            BigDecimal estimatedFare,
            Long vehicleTypeId,
            LocalDateTime requestedAt
    ) {}

    public record RequestClosedNotification(Long tripId, String reason) {}

    public record TripAssignedNotification(Long tripId, Long riderId) {}

    public void broadcastNewRequest(Long riderId, NewRequestNotification notification) {
        messagingTemplate.convertAndSend("/topic/rider/" + riderId + "/requests", notification);
    }

    /** Tells a candidate rider this request is no longer up for grabs (someone else took it, or it timed out). */
    public void broadcastRequestClosed(Long riderId, Long tripId, String reason) {
        messagingTemplate.convertAndSend("/topic/rider/" + riderId + "/requests", new RequestClosedNotification(tripId, reason));
    }

    /** Lets the customer app know the instant a rider accepts, without waiting on a poll. */
    public void broadcastTripAssigned(Long tripId, Long riderId) {
        messagingTemplate.convertAndSend("/topic/trip/" + tripId + "/status", new TripAssignedNotification(tripId, riderId));
    }
}
