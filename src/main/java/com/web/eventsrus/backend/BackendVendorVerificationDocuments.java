package com.web.eventsrus.backend;

import com.web.eventsrus.model.VendorLegalDocumentItem;
import java.time.Instant;
import java.util.List;

/** Mirrors eventsrus-backend's {@code VendorVerificationDocumentsResponse} - one vendor's full document review view. */
public record BackendVendorVerificationDocuments(
        Long vendorUserId,
        String businessName,
        String ownerName,
        String idCardUrl,
        String selfieUrl,
        List<VendorLegalDocumentItem> legalDocuments,
        boolean verified,
        Instant verifiedAt,
        String verifiedByAdmin) {}
