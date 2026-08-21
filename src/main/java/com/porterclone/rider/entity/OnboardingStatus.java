package com.porterclone.rider.entity;

/**
 * Rider onboarding state machine.
 * REGISTERED -> DOCUMENT_SUBMITTED -> UNDER_REVIEW -> APPROVED -> ACTIVE
 *                                          |
 *                                       REJECTED (can resubmit -> DOCUMENT_SUBMITTED)
 * ACTIVE <-> SUSPENDED (admin action)
 *
 * IMPORTANT: a rider can only go online_status=ONLINE once onboarding_status=ACTIVE.
 */
public enum OnboardingStatus {
    REGISTERED,
    DOCUMENT_SUBMITTED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    ACTIVE,
    SUSPENDED
}
