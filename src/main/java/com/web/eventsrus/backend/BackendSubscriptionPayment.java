package com.web.eventsrus.backend;

import java.time.Instant;
import java.util.List;

/** Mirrors eventsrus-backend's dto.AdminSubscriptionPaymentResponse - one row of the admin module's GCash payment-verification queue. */
public record BackendSubscriptionPayment(
        Long vendorUserId,
        String businessName,
        String ownerName,
        String billingCycle,
        String status,
        String screenshotUrl,
        Instant submittedAt,
        String rejectionReason,
        String vendorRemarks,
        List<BackendGcashHistoryEntry> history) {}
