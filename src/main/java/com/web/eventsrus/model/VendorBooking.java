package com.web.eventsrus.model;

import java.math.BigDecimal;
import java.time.Instant;

/** Mirrors eventsrus-backend's {@code BookingResponse}. */
public record VendorBooking(
        long id,
        long eventId,
        String eventName,
        long vendorUserId,
        String vendorBusinessName,
        String vendorSlug,
        long plannerUserId,
        Long quotationId,
        BigDecimal price,
        Instant eventDatetime,
        String agreementDetails,
        BookingStatus status,
        Instant proposedAt,
        Instant respondedAt,
        String paymentScreenshotUrl,
        Instant paymentScreenshotUploadedAt,
        Instant paymentAcknowledgedAt,
        String paymentRejectionReason,
        String invoiceUrl,
        Instant invoiceUploadedAt,
        Instant cancelledAt,
        String cancellationReason,
        Long cancelledByUserId) {}
