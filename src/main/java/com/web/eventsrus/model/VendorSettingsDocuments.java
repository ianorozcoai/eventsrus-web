package com.web.eventsrus.model;

/**
 * The vendor's currently-uploaded files (or null for any not yet
 * uploaded) - logo from Tab 1, plus the verification documents captured at
 * onboarding (ID card, selfie - private, shown here so the vendor can
 * confirm what's on file and replace it), and the two Tab 3 policy PDFs.
 * Mirrors every URL field on eventsrus-backend's VendorSettingsResponse.
 * Kept separate from VendorSettingsForm since none of these are form
 * fields - each is a file upload with its own <input type="file">, not
 * something th:field can bind to. businessPermitUrl is gone - legal
 * documents are their own list now (see VendorLegalDocumentItem), fetched
 * separately, same as paymentMethods already is.
 */
public record VendorSettingsDocuments(
        String logoImageUrl,
        String idCardUrl,
        String selfieUrl,
        String cancellationPolicyUrl,
        String refundTermsUrl) {}
