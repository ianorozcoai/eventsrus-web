package com.web.eventsrus.backend;

import java.time.Instant;

/** Mirrors eventsrus-backend's dto.SubscriptionStatusResponse field-for-field. */
public record BackendSubscriptionStatus(
        String plan,
        Instant expiresAt,
        boolean expiringSoon,
        boolean expired,
        boolean inGracePeriod,
        Instant graceEndsAt,
        int monthlyPrice,
        int quarterlyPrice,
        int semiAnnualPrice,
        int annualPrice,
        String billingSource,
        String monthlyPlanId,
        String quarterlyPlanId,
        String semiAnnualPlanId,
        String annualPlanId,
        boolean showWelcomePopup,
        boolean gcashAwaitingVerification,
        boolean gcashRejected,
        String gcashRejectionReason) {}
