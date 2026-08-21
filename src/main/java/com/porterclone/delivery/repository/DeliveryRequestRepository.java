package com.porterclone.delivery.repository;

import com.porterclone.delivery.entity.DeliveryRequest;
import com.porterclone.delivery.entity.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface DeliveryRequestRepository extends JpaRepository<DeliveryRequest, Long> {
    List<DeliveryRequest> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<DeliveryRequest> findByRiderIdOrderByCreatedAtDesc(Long riderId);

    /** Backs the rider's "pending requests" poll — same eligible set the broadcast went out to. */
    List<DeliveryRequest> findByStatusAndVehicleTypeId(TripStatus status, Long vehicleTypeId);

    /** Backs the timeout sweep — SEARCHING_RIDER trips nobody has accepted within the broadcast window. */
    List<DeliveryRequest> findByStatusAndRequestedAtBefore(TripStatus status, LocalDateTime cutoff);
}
