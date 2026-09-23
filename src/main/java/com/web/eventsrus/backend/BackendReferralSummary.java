package com.web.eventsrus.backend;

import java.math.BigDecimal;
import java.time.Instant;

public record BackendReferralSummary(
        long id,
        String referredBusinessName,
        String status,
        BigDecimal commissionAmount,
        Instant createdAt,
        Instant convertedAt,
        Instant paidAt,
        String paymentRemarks,
        String paymentProofUrl) {}
