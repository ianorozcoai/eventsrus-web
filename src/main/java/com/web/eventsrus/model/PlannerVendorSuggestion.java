package com.web.eventsrus.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mirrors eventsrus-backend's real {@code SuggestedVendorResponse} - one
 * vendor (or generic category, when vendorProfileId is null - no matching
 * vendor for that type/location yet) suggested for an event by
 * AiSuggestionService/VendorSearchService (see EventService#toResponse).
 * categorySelected/categoryBudget repeat the same value on every row sharing
 * a vendorType - they're a per-category choice, not a per-vendor one.
 * businessTypes is the vendor's full type list, shown instead of a
 * Verified/Top Supplier badge (see SuggestedVendorResponse's javadoc on the
 * backend for why) - null for a generic category placeholder with no
 * matching vendor yet.
 */
public record PlannerVendorSuggestion(
        BusinessType vendorType, boolean categorySelected, BigDecimal categoryBudget, boolean essential,
        Long vendorProfileId, String businessName, String slug, String logoImageUrl, String city, boolean verified,
        boolean topVendor, Double averageRating, int reviewCount, List<BusinessType> businessTypes) {}
