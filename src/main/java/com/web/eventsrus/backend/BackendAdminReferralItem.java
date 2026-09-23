package com.web.eventsrus.backend;

import com.web.eventsrus.model.ReferralStatus;
import java.math.BigDecimal;
import java.time.Instant;

/** Mirrors eventsrus-backend's {@code AdminReferralResponse} field-for-field. */
public record BackendAdminReferralItem(
        Long id,
        String referrerBusinessName,
        String referrerEmail,
        String referredBusinessName,
        String referredEmail,
        ReferralStatus status,
        BigDecimal commissionAmount,
        Instant createdAt,
        Instant convertedAt,
        Instant paidAt,
        String paymentRemarks,
        String paymentProofUrl) {}
