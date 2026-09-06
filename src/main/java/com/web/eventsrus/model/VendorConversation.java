package com.web.eventsrus.model;

import java.time.Instant;

/** Mirrors eventsrus-backend's {@code ConversationSummaryResponse}. */
public record VendorConversation(
        long id,
        long eventId,
        String eventName,
        long otherPartyUserId,
        String otherPartyName,
        String lastMessagePreview,
        Instant lastMessageAt,
        long unreadCount) {}
