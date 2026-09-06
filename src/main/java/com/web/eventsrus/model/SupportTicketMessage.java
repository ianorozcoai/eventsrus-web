package com.web.eventsrus.model;

import java.time.Instant;

/**
 * Mirrors eventsrus-backend's {@code SupportTicketMessageResponse}, except
 * senderRole is a plain String here ("PLANNER"/"VENDOR"/"ADMIN") rather than
 * the real enums.Role - this stub-only app has no Role concept of its own.
 */
public record SupportTicketMessage(
        long id,
        long senderUserId,
        String senderName,
        String senderRole,
        String body,
        Instant createdAt) {}
