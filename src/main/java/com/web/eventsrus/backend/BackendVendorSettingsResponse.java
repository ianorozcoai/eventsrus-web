package com.web.eventsrus.backend;

import com.web.eventsrus.model.BusinessType;
import com.web.eventsrus.model.EventType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Mirrors eventsrus-backend's VendorSettingsResponse field-for-field - one
 * combined payload covering what the web app splits into VendorSettingsForm
 * (editable fields) and VendorSettingsDocuments (file URLs) for template
 * binding. See VendorController#settings for how the split happens.
 */
public record BackendVendorSettingsResponse(
        String slug,
        String businessName,
        String description,
        String ownerName,
        BusinessType businessType,
        String contactEmail,
        String phoneNumber,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country,
        String logoImageUrl,
        String idCardUrl,
        String selfieUrl,
        boolean verified,
        Instant verifiedAt,
        BusinessType primaryCategory,
        Integer maxGuestCapacity,
        Integer maxCustomersPerDay,
        BigDecimal basePrice,
        Integer leadTimeDays,
        String storefrontOverview,
        List<String> operatingAreas,
        List<EventType> cateredEventTypes,
        String cancellationPolicyUrl,
        String refundTermsUrl,
        String paymentInstructions) {}
