package com.porterclone.rider.repository;

import com.porterclone.rider.entity.RiderDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiderDocumentRepository extends JpaRepository<RiderDocument, Long> {
    List<RiderDocument> findByRiderId(Long riderId);
    List<RiderDocument> findByRiderIdOrderByCreatedAtDesc(Long riderId);
}
