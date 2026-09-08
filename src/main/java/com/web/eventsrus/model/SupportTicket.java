package com.web.eventsrus.model;

import java.time.Instant;

/** Mirrors eventsrus-backend's {@code SupportTicketResponse} field-for-field. */
public record SupportTicket(
        long id,
        String subject,
        TicketCategory category,
        TicketStatus status,
        long raisedByUserId,
        String raisedByName,
        String raisedByEmail,
        String raisedByRole,
        Long relatedEventId,
        String relatedEventName,
        Long relatedBookingId,
        Long relatedQuotationId,
        Long assignedAdminUserId,
        String assignedAdminName,
        String lastMessagePreview,
        Instant lastMessageAt,
        Instant resolvedAt,
        Instant closedAt,
        Instant createdAt) {}
