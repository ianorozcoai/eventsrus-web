package com.web.eventsrus.model;

import java.time.Instant;
import java.time.LocalDate;

/** Mirrors eventsrus-backend's {@code ConversationSummaryResponse}. */
public record VendorConversation(
        long id,
        long eventId,
        String eventName,
        LocalDate eventDate,
        long otherPartyUserId,
        String otherPartyName,
        /** Null when the other party is a planner (not a vendor) - e.g. viewed from a vendor's own Messages page. */
        BusinessType otherPartyBusinessType,
        String otherPartySlug,
        String lastMessagePreview,
        Instant lastMessageAt,
        long unreadCount) {}
