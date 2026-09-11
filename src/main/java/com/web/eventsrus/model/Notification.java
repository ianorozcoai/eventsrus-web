package com.web.eventsrus.model;

import java.time.Instant;

/**
 * Mirrors eventsrus-backend's dto.NotificationResponse. type/relatedEntityType
 * are plain strings (not enums) here - the bell dropdown only ever needs to
 * show them or switch on relatedEntityType for a link/icon, never round-trip
 * one back to the backend, so there's nothing a real enum buys.
 */
public record Notification(
        long id,
        String type,
        String title,
        String body,
        String relatedEntityType,
        Long relatedEntityId,
        boolean read,
        Instant createdAt) {}
