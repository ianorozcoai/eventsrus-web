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
        String cancellationPolicyUrl,
        String refundTermsUrl,
        long plannerUserId,
        String plannerName,
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
        Long cancelledByUserId,
        boolean canReview,
        Long reviewId,
        Integer reviewRating,
        String reviewComment) {}
