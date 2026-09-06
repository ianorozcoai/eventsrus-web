package com.web.eventsrus.backend;

import java.math.BigDecimal;
import java.time.Instant;

/** Mirrors eventsrus-backend's dto.VendorBillingHistoryEntryResponse field-for-field. */
public record BackendBillingHistoryEntry(
        long id,
        String plan,
        String billingSource,
        BigDecimal amount,
        String currency,
        String paypalTransactionId,
        Instant periodStart,
        Instant periodEnd,
        Instant occurredAt) {}
