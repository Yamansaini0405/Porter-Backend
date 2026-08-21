package com.porterclone.delivery.repository;

import com.porterclone.delivery.entity.TripStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripStatusHistoryRepository extends JpaRepository<TripStatusHistory, Long> {
    List<TripStatusHistory> findByTripIdOrderByRecordedAtAsc(Long tripId);
}
