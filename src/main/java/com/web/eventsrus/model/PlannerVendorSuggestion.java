package com.web.eventsrus.model;

/**
 * Mirrors eventsrus-backend's real {@code SuggestedVendorResponse} - one
 * vendor (or generic category, when vendorProfileId is null - no matching
 * vendor for that type/location yet) suggested for an event by
 * AiSuggestionService/VendorSearchService (see EventService#toResponse).
 */
public record PlannerVendorSuggestion(
        BusinessType vendorType, Long vendorProfileId, String businessName, String slug, String logoImageUrl,
        String city, boolean verified, boolean topVendor, Double averageRating, int reviewCount) {}
