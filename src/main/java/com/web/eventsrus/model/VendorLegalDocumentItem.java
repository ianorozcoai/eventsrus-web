package com.web.eventsrus.model;

import java.time.Instant;

/** Mirrors eventsrus-backend's {@code VendorLegalDocumentResponse} field-for-field. */
public record VendorLegalDocumentItem(
        long id,
        LegalDocumentType documentType,
        String label,
        String url,
        Instant createdAt) {}
