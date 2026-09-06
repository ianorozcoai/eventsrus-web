package com.web.eventsrus.model;

/**
 * A client testimonial shown on the public vendor storefront. Stub-only -
 * no reviews/testimonials concept exists in eventsrus-backend yet.
 * reviewerInitials is stored directly rather than derived from
 * reviewerName, since it's synthetic stub data either way.
 */
public record VendorReview(String reviewerName, String reviewerInitials, String reviewerRole, int rating, String quote) {}
