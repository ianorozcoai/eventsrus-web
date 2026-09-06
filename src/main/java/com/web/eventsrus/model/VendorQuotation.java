package com.web.eventsrus.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Mirrors eventsrus-backend's {@code QuotationResponse} field-for-field
 * (including vendorBusinessName/vendorSlug, added there this round), plus
 * one stub-only addition: quotedAmount. The real quotation is PDF-only (no
 * structured price field on the backend), but the planner's "Book This"
 * flow needs something to prefill the price with - stands in until the
 * real DTO carries a headline price alongside the PDF, if it ever does.
 */
public record VendorQuotation(
        long id,
        long eventId,
        String eventName,
        long vendorUserId,
        String vendorBusinessName,
        String vendorSlug,
        long plannerUserId,
        LocalDate targetDate,
        String requestMessage,
        QuotationStatus status,
        String pdfUrl,
        BigDecimal quotedAmount,
        Instant respondedAt,
        Instant createdAt,
        List<Long> packageIds,
        List<String> packageNames,
        Instant declinedAt) {}
