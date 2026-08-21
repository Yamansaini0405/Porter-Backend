package com.porterclone.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/** Pushes a rider's live location to any customer currently tracking that trip. */
@Service
public class LocationBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    public LocationBroadcastService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public record LocationUpdate(Long riderId, double lat, double lng, long timestamp) {}

    public void broadcastLocation(Long tripId, Long riderId, double lat, double lng) {
        messagingTemplate.convertAndSend(
                "/topic/trip/" + tripId + "/location",
                new LocationUpdate(riderId, lat, lng, System.currentTimeMillis())
        );
    }
}
