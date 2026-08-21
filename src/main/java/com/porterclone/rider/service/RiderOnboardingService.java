package com.porterclone.rider.service;

import com.porterclone.common.exception.ApiException;
import com.porterclone.common.exception.InvalidStateTransitionException;
import com.porterclone.rider.entity.*;
import com.porterclone.rider.repository.RiderDocumentRepository;
import com.porterclone.rider.repository.RiderOnboardingHistoryRepository;
import com.porterclone.rider.repository.RiderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Owns EVERY rider onboarding state transition. No other class should mutate
 * Rider.onboardingStatus directly — that's how you guarantee a rider can never
 * skip straight from REGISTERED to ACTIVE, or go online while still under review.
 *
 * REGISTERED -> DOCUMENT_SUBMITTED -> UNDER_REVIEW -> APPROVED -> ACTIVE
 *                                            |
 *                                         REJECTED --(resubmit)--> DOCUMENT_SUBMITTED
 * ACTIVE <-> SUSPENDED
 */
@Service
public class RiderOnboardingService {

    private static final Map<OnboardingStatus, Set<OnboardingStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(OnboardingStatus.class);
    static {
        ALLOWED_TRANSITIONS.put(OnboardingStatus.REGISTERED, EnumSet.of(OnboardingStatus.DOCUMENT_SUBMITTED));
        ALLOWED_TRANSITIONS.put(OnboardingStatus.DOCUMENT_SUBMITTED, EnumSet.of(OnboardingStatus.UNDER_REVIEW));
        ALLOWED_TRANSITIONS.put(OnboardingStatus.UNDER_REVIEW, EnumSet.of(OnboardingStatus.APPROVED, OnboardingStatus.REJECTED));
        ALLOWED_TRANSITIONS.put(OnboardingStatus.APPROVED, EnumSet.of(OnboardingStatus.ACTIVE));
        ALLOWED_TRANSITIONS.put(OnboardingStatus.REJECTED, EnumSet.of(OnboardingStatus.DOCUMENT_SUBMITTED));
        ALLOWED_TRANSITIONS.put(OnboardingStatus.ACTIVE, EnumSet.of(OnboardingStatus.SUSPENDED));
        ALLOWED_TRANSITIONS.put(OnboardingStatus.SUSPENDED, EnumSet.of(OnboardingStatus.ACTIVE));
    }

    private final RiderRepository riderRepository;
    private final RiderDocumentRepository riderDocumentRepository;
    private final RiderOnboardingHistoryRepository historyRepository;

    public RiderOnboardingService(RiderRepository riderRepository,
                                   RiderDocumentRepository riderDocumentRepository,
                                   RiderOnboardingHistoryRepository historyRepository) {
        this.riderRepository = riderRepository;
        this.riderDocumentRepository = riderDocumentRepository;
        this.historyRepository = historyRepository;
    }

    /** Rider uploads a KYC document. Moves REGISTERED -> DOCUMENT_SUBMITTED on first upload. */
    @Transactional
    public RiderDocument submitDocument(Long riderId, DocumentType type, String fileUrl) {
        Rider rider = getRider(riderId);

        RiderDocument doc = new RiderDocument();
        doc.setRiderId(riderId);
        doc.setDocType(type);
        doc.setFileUrl(fileUrl);
        doc.setVerificationStatus(VerificationStatus.PENDING);
        riderDocumentRepository.save(doc);

        if (rider.getOnboardingStatus() == OnboardingStatus.REGISTERED) {
            transition(rider, OnboardingStatus.DOCUMENT_SUBMITTED, "SYSTEM", "First document uploaded");
        } else if (rider.getOnboardingStatus() == OnboardingStatus.REJECTED) {
            transition(rider, OnboardingStatus.DOCUMENT_SUBMITTED, "SYSTEM", "Document resubmitted after rejection");
        }
        return doc;
    }

    /** Rider confirms all required documents are uploaded — pushes the application into the admin review queue. */
    @Transactional
    public Rider submitForReview(Long riderId) {
        Rider rider = getRider(riderId);
        List<RiderDocument> docs = riderDocumentRepository.findByRiderId(riderId);
        if (docs.isEmpty()) {
            throw ApiException.badRequest("NO_DOCUMENTS", "Upload at least one document before submitting for review");
        }
        transition(rider, OnboardingStatus.UNDER_REVIEW, "RIDER", "Submitted for admin review");
        return rider;
    }

    /** Admin approves — rider still isn't ACTIVE yet, this just clears them to activate. */
    @Transactional
    public Rider approve(Long riderId, Long adminId) {
        Rider rider = getRider(riderId);
        transition(rider, OnboardingStatus.APPROVED, "ADMIN:" + adminId, "KYC approved");
        rider.setApprovedAt(LocalDateTime.now());
        rider.setApprovedByAdminId(adminId);
        return riderRepository.save(rider);
    }

    /** Admin rejects — rider must resubmit documents. */
    @Transactional
    public Rider reject(Long riderId, Long adminId, String reason) {
        Rider rider = getRider(riderId);
        transition(rider, OnboardingStatus.REJECTED, "ADMIN:" + adminId, reason);
        return rider;
    }

    /**
     * Final activation step — separate from APPROVED so a rider can be approved
     * but choose to activate later (e.g., after completing an induction video).
     */
    @Transactional
    public Rider activate(Long riderId) {
        Rider rider = getRider(riderId);
        transition(rider, OnboardingStatus.ACTIVE, "RIDER", "Rider activated their account");
        return rider;
    }

    @Transactional
    public Rider suspend(Long riderId, Long adminId, String reason) {
        Rider rider = getRider(riderId);
        transition(rider, OnboardingStatus.SUSPENDED, "ADMIN:" + adminId, reason);
        // A suspended rider must not remain visible to the matching engine.
        rider.setOnlineStatus(OnlineStatus.OFFLINE);
        return riderRepository.save(rider);
    }

    @Transactional
    public Rider reinstate(Long riderId, Long adminId) {
        Rider rider = getRider(riderId);
        transition(rider, OnboardingStatus.ACTIVE, "ADMIN:" + adminId, "Reinstated after suspension");
        return rider;
    }

    private void transition(Rider rider, OnboardingStatus to, String changedBy, String remarks) {
        OnboardingStatus from = rider.getOnboardingStatus();
        Set<OnboardingStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new InvalidStateTransitionException("Rider onboarding", from.name(), to.name());
        }

        rider.setOnboardingStatus(to);
        riderRepository.save(rider);

        RiderOnboardingHistory history = new RiderOnboardingHistory();
        history.setRiderId(rider.getId());
        history.setFromStatus(from.name());
        history.setToStatus(to.name());
        history.setChangedBy(changedBy);
        history.setRemarks(remarks);
        historyRepository.save(history);
    }

    public Rider getRider(Long riderId) {
        return riderRepository.findById(riderId)
                .orElseThrow(() -> ApiException.notFound("RIDER_NOT_FOUND", "Rider not found: " + riderId));
    }

    public Rider getRiderByUserId(Long userId) {
        return riderRepository.findByUserId(userId)
                .orElseThrow(() -> ApiException.notFound("RIDER_NOT_FOUND", "Rider profile not found for user: " + userId));
    }
}
