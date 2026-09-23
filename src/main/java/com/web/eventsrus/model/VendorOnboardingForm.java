package com.web.eventsrus.model;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mirrors eventsrus-backend's dto.VendorOnboardingRequest field-for-field
 * (file uploads aside, which the controller will bind separately as
 * MultipartFile parameters once this posts to the real backend), plus one
 * stub-only addition: operatingAreas. The real request DTO doesn't have
 * this yet - same speculative field already added to VendorSettingsForm's
 * Tab 2, required here since the vendor should be forced to state where
 * they operate before finishing onboarding. acceptedTerms mirrors the real
 * request DTO's own new field (@AssertTrue there) - required in the UI,
 * linking to /terms.
 * A mutable POJO rather than a record - Thymeleaf's th:field="*{...}"
 * binding needs a no-arg constructor and setters.
 */
@Getter
@Setter
@NoArgsConstructor
public class VendorOnboardingForm {

    private String businessName;
    private List<BusinessType> businessTypes;
    private String ownerName;
    private String description;
    private String contactEmail;
    private String phoneNumber;
    private String facebookPageUrl;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private List<String> operatingAreas;

    // Which event types the vendor caters to - hidden on the simplified
    // onboarding form (no longer required either client- or server-side).
    // Editable later in Account Settings' Service Scope tab (VendorSettingsForm).
    private List<EventType> cateredEventTypes;

    private boolean acceptedTerms;

    // Populated client-side by onboarding.html's inline script (grecaptcha.execute)
    // right before submit - real reCAPTCHA v3, verified for real by the backend
    // (see UserService#becomeVendor there), unlike everything else in this app.
    private String recaptchaToken;

    // Pre-filled server-side from the session (see WebSession.REFERRAL_CODE,
    // stashed by AuthWebController#vendorLogin from /vendor/?ref=CODE) -
    // real, verified by the backend (VendorReferralService#attribute).
    private String referralCode;

    // Checked against the admin-configured Vendor Promo Code system setting
    // (see PromoCodeService) - a valid code grants the free trial with no
    // paywall; blank sends the vendor through the pay-or-skip flow instead.
    private String promoCode;
}
