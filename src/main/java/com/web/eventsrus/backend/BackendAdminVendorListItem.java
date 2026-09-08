package com.web.eventsrus.backend;

import com.web.eventsrus.model.BusinessType;
import java.time.Instant;
import java.util.List;

/**
 * Mirrors eventsrus-backend's {@code AdminVendorListItemResponse} - one row
 * of the verification review queue AND the Vendors directory page (both
 * admin views are every vendor, just displayed differently).
 */
public record BackendAdminVendorListItem(
        Long vendorUserId,
        String businessName,
        String ownerName,
        String contactEmail,
        String phoneNumber,
        BusinessType businessType,
        String slug,
        String city,
        String state,
        List<String> operatingAreas,
        boolean hasIdCard,
        boolean hasSelfie,
        int legalDocumentCount,
        boolean verified,
        Instant verifiedAt,
        String verifiedByAdmin,
        Instant createdAt) {}
