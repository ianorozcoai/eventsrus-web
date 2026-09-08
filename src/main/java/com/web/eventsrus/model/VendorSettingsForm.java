package com.web.eventsrus.model;

import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mirrors eventsrus-backend's dto.VendorSettingsRequest field-for-field,
 * including its own tab grouping (the backend's own comments already split
 * it into these 3 tabs - this class just keeps the same shape). One
 * stub-only field that used to live here - description (set at onboarding,
 * shown on the public storefront, but never exposed for editing again by
 * the real VendorSettingsRequest/Response) - was dropped once this form
 * started binding to the real endpoint; editing it here would have
 * silently done nothing. maxCustomersPerDay is real now too (V28) -
 * distinct from maxGuestCapacity (headcount per event): how many separate
 * client bookings the vendor can take on in one day.
 * operatingAreas (the provinces the vendor serves, or "Entire
 * Philippines") and
 * cateredEventTypes (which event types the vendor caters to) are both real
 * on the backend DTO now, bound the same way - repeated form fields from a
 * native multi-select, not a delimited string.
 * primaryCategory/maxGuestCapacity/basePrice/storefrontOverview are kept
 * here for real-DTO parity even though Tab 2 no longer renders them.
 * Tab 3 (retainerPercentage/cancellationPolicy/refundTerms) has no fields
 * here at all now - the real VendorSettingsRequest dropped retainer
 * entirely and moved cancellation policy / refund terms to PDF uploads
 * (separate multipart parts, not form fields - see VendorPolicyDocuments
 * for what's currently on file, and the Tab 3 template for the uploaders).
 * Tab 4's paymentInstructions is real on the backend DTO too - the QR code
 * uploads themselves are a separate list (VendorPaymentMethodItem), not a
 * field here, same reasoning as the Tab 3 PDFs above.
 * No field here has a validation annotation on the real DTO, so none are
 * marked required in the form either.
 * A mutable POJO rather than a record - Thymeleaf's th:field="*{...}"
 * binding needs a no-arg constructor and setters.
 */
@Getter
@Setter
@NoArgsConstructor
public class VendorSettingsForm {

    // Tab 1 - Business Info & Credentials
    private String businessName;
    private String ownerName;
    private BusinessType businessType;
    private String contactEmail;
    private String phoneNumber;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;

    // Tab 2 - Service Scope & Metrics
    private BusinessType primaryCategory;
    private Integer maxGuestCapacity;
    private Integer maxCustomersPerDay;
    private BigDecimal basePrice;
    private Integer leadTimeDays;
    private String storefrontOverview;
    private List<String> operatingAreas;
    private List<EventType> cateredEventTypes;

    // Tab 4 - Payment Methods
    private String paymentInstructions;
}
