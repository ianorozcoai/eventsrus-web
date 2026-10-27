package com.web.eventsrus.backend;

import java.time.Instant;

/** Mirrors eventsrus-backend's AdminReportResponse (enums as plain strings). */
public record BackendAdminReport(
        Long id,
        String contentType,
        Long contentId,
        String reason,
        String details,
        String status,
        Long reporterUserId,
        String reporterName,
        Long reportedUserId,
        String reportedName,
        String contentPreview,
        Instant createdAt,
        Instant resolvedAt,
        String resolvedNote) {}
