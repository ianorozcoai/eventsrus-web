package com.web.eventsrus.model;

import java.time.Instant;
import java.time.LocalDate;

/** Mirrors eventsrus-backend's {@code ConversationMessageResponse}. */
public record VendorConversationMessage(
        long id, long senderUserId, String senderName, LocalDate targetDate, String body, Instant createdAt) {}
