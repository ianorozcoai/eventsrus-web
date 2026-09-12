package com.web.eventsrus.backend;

import com.web.eventsrus.model.QuotationStatus;
import java.time.Instant;

/** Mirrors eventsrus-backend's {@code QuotationStatusEventResponse} field-for-field. */
public record BackendQuotationHistoryEntry(
        Long id,
        QuotationStatus fromStatus,
        QuotationStatus toStatus,
        Long changedByUserId,
        String changedByName,
        String reason,
        String pdfUrl,
        Instant createdAt) {}
