package com.porterclone.dispute.repository;

import com.porterclone.dispute.entity.Dispute;
import com.porterclone.dispute.entity.DisputeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    List<Dispute> findByStatus(DisputeStatus status);
}
