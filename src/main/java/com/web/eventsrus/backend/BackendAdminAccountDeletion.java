package com.web.eventsrus.backend;

import java.time.Instant;

/** Mirrors eventsrus-backend's AdminAccountDeletionResponse (PENDING = inside the 30-day grace period, COMPLETED = scrubbed). */
public record BackendAdminAccountDeletion(
        Long userId,
        String status,
        String role,
        String name,
        String email,
        Instant requestedAt,
        Instant scheduledDeletionAt,
        Instant deletedAt) {}
