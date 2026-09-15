package com.web.eventsrus.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Mirrors eventsrus-backend's {@code QuotationResponse} field-for-field.
 * quotedAmount is now a real vendor-entered figure (not a stub) since
 * Phase 1 of the quotation/booking lifecycle rework - see project memory
 * quotation-booking-target-state-machine.
 */
public record VendorQuotation(
        long id,
        long eventId,
        String eventName,
        EventType eventType,
        long vendorUserId,
        String vendorBusinessName,
        String vendorSlug,
        long plannerUserId,
        LocalDate targetDate,
        String requestMessage,
        QuotationStatus status,
        int version,
        BigDecimal quotedAmount,
        String pdfUrl,
        Instant respondedAt,
        Instant acceptedAt,
        String paymentScreenshotUrl,
        String paymentRejectionReason,
        Instant createdAt,
        List<Long> packageIds,
        List<String> packageNames,
        Instant declinedAt) {}
