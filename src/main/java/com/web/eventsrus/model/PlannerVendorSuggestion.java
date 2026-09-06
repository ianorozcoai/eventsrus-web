package com.web.eventsrus.model;

import java.util.List;

/**
 * A vendor shown on the landing page's "Recommended Suppliers" panel.
 * Mirrors the shape eventsrus-backend's real pipeline already produces
 * (EventService -> AiSuggestionService -> VendorSearchService ->
 * SuggestedVendorResponse), but this is sourced from a local stub
 * directory (stubs/vendor-directory.json) rather than a live call - see
 * PlannerCoordinatorService. slug is null for most entries - only vendors
 * with a real stubs/storefronts/{slug}.json get a browsable storefront
 * (VendorController#storefrontBySlug); cards without one show "Storefront
 * coming soon" rather than a dead or misleading link.
 */
public record PlannerVendorSuggestion(
        String businessName,
        BusinessType businessType,
        String city,
        List<String> operatingAreas,
        String description,
        String slug) {}
