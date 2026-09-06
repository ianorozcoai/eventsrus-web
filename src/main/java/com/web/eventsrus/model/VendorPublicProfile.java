package com.web.eventsrus.model;

import java.util.List;

/**
 * The public vendor storefront - what a planner sees when they open
 * "View My Page". Mirrors eventsrus-backend's VendorPublicProfileResponse
 * field-for-field, plus a few stub-only additions the backend doesn't have
 * yet: galleryCaptions (no gallery concept exists yet - only a single
 * logoImageUrl), the tierLabel/primaryRegion/rating/bookingsCount/
 * responseTime hero-banner fields (no vendor tiering, ratings, or
 * response-time tracking exists yet), and reviews (no reviews/testimonials
 * concept exists yet). All of these stand in until the real backend fields
 * exist. paymentInstructions/paymentMethods are real on the backend
 * response now - note paymentMethods only ever holds APPROVED entries there
 * (VendorDirectoryService filters via
 * VendorPaymentMethodService#listApprovedForStorefront), so this stub JSON
 * is curated the same way rather than filtering client-side.
 * legalDocuments/identityVerified are also real now. legalDocuments is a
 * list, not a single flag/URL - a business can have several registration
 * documents on file (DTI, SEC, Mayor's Permit, Barangay Clearance, BIR,
 * ...), each with a real presigned link to the actual scanned document
 * (business paperwork is safe to show planners directly, unlike most other
 * stub fields here which are placeholder-only). There's deliberately no
 * equivalent list/URL for identityVerified - that one stays a yes/no
 * summary only, since it stands for the ID card + selfie (personal identity
 * documents, not business paperwork) which should never be shown to
 * anonymous storefront visitors.
 */
public record VendorPublicProfile(
        long vendorUserId,
        String businessName,
        String ownerName,
        String description,
        String logoImageUrl,
        BusinessType businessType,
        String city,
        String state,
        String country,
        String contactEmail,
        String phoneNumber,
        String tierLabel,
        String primaryRegion,
        double rating,
        int bookingsCount,
        String responseTime,
        List<VendorLegalDocumentItem> legalDocuments,
        boolean identityVerified,
        List<String> galleryCaptions,
        List<VendorPackageItem> packages,
        List<VendorReview> reviews,
        String paymentInstructions,
        List<VendorPaymentMethodItem> paymentMethods) {}
