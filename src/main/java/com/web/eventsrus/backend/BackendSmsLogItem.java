package com.web.eventsrus.backend;

import com.web.eventsrus.model.SmsTriggerType;
import java.time.Instant;

/** Mirrors eventsrus-backend's {@code SmsLogResponse} field-for-field. */
public record BackendSmsLogItem(
        Long id,
        String recipientNumber,
        String message,
        SmsTriggerType triggerType,
        boolean success,
        String errorMessage,
        String sentByAdminEmail,
        Instant createdAt) {}
