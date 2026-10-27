package com.web.eventsrus.backend;

import java.time.Instant;

/** Mirrors eventsrus-backend's AdminBlockResponse (roles as plain strings). */
public record BackendAdminBlock(
        Long id,
        Long blockerUserId,
        String blockerName,
        String blockerRole,
        Long blockedUserId,
        String blockedName,
        String blockedRole,
        Instant createdAt) {}
