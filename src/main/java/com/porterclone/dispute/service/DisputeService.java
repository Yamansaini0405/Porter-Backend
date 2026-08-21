package com.porterclone.dispute.service;

import com.porterclone.common.exception.ApiException;
import com.porterclone.dispute.dto.RaiseDisputeRequest;
import com.porterclone.dispute.entity.Dispute;
import com.porterclone.dispute.entity.DisputeStatus;
import com.porterclone.dispute.repository.DisputeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class DisputeService {

    private final DisputeRepository disputeRepository;

    public DisputeService(DisputeRepository disputeRepository) {
        this.disputeRepository = disputeRepository;
    }

    @Transactional
    public Dispute raise(RaiseDisputeRequest request) {
        Dispute dispute = new Dispute();
        dispute.setTripId(request.tripId());
        dispute.setRaisedBy(request.raisedBy());
        dispute.setCategory(request.category());
        dispute.setDescription(request.description());
        dispute.setStatus(DisputeStatus.OPEN);
        return disputeRepository.save(dispute);
    }

    @Transactional
    public Dispute resolve(Long disputeId, Long adminId, String notes, boolean accepted) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> ApiException.notFound("DISPUTE_NOT_FOUND", "Dispute not found"));
        dispute.setStatus(accepted ? DisputeStatus.RESOLVED : DisputeStatus.REJECTED);
        dispute.setResolvedByAdminId(adminId);
        dispute.setResolutionNotes(notes);
        dispute.setResolvedAt(LocalDateTime.now());
        return disputeRepository.save(dispute);
    }
}
