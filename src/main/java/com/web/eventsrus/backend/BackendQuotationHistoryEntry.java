package com.web.eventsrus.backend;

import com.web.eventsrus.model.QuotationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Mirrors eventsrus-backend's {@code QuotationStatusEventResponse} field-for-field. */
public record BackendQuotationHistoryEntry(
        Long id,
        QuotationStatus fromStatus,
        QuotationStatus toStatus,
        Long changedByUserId,
        String changedByName,
        String reason,
        String pdfUrl,
        Integer version,
        BigDecimal quotedAmount,
        LocalDate targetDate,
        List<String> packageNames,
        String paymentScreenshotUrl,
        String invoiceUrl,
        Instant createdAt) {}
