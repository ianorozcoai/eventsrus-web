package com.web.eventsrus.backend;

import com.web.eventsrus.model.AdminPlannerSummary;
import com.web.eventsrus.model.CoordinatorHistory;
import com.web.eventsrus.model.CoordinatorQuestion;
import com.web.eventsrus.model.EventType;
import com.web.eventsrus.model.PlannerEvent;
import com.web.eventsrus.model.PlannerEventSummary;
import com.web.eventsrus.model.SupportTicket;
import com.web.eventsrus.model.SupportTicketMessage;
import com.web.eventsrus.model.VendorBooking;
import com.web.eventsrus.model.VendorCalendarEntry;
import com.web.eventsrus.model.VendorConversation;
import com.web.eventsrus.model.VendorConversationMessage;
import com.web.eventsrus.model.VendorDashboard;
import com.web.eventsrus.model.VendorLead;
import com.web.eventsrus.model.VendorLegalDocumentForm;
import com.web.eventsrus.model.VendorLegalDocumentItem;
import com.web.eventsrus.model.VendorPackageForm;
import com.web.eventsrus.model.VendorPackageImageItem;
import com.web.eventsrus.model.PlannerProfileForm;
import com.web.eventsrus.model.VendorPackageItem;
import com.web.eventsrus.model.VendorPaymentMethodForm;
import com.web.eventsrus.model.VendorPaymentMethodItem;
import com.web.eventsrus.model.VendorPublicProfile;
import com.web.eventsrus.model.VendorQuotation;
import com.web.eventsrus.model.Notification;
import com.web.eventsrus.model.VendorReview;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

/**
 * The one place eventsrus-web talks to the real eventsrus-backend. Every
 * method takes the caller's JWT explicitly rather than reading it from a
 * session itself, so this class has no web-layer concerns (HttpSession,
 * cookies) at all. Response types are mostly the SAME model records the app
 * used for stub data (VendorLead, VendorBooking, ...) - they were already
 * built to mirror the real backend DTOs field-for-field, so Jackson
 * deserializes the real JSON into them directly with no separate `Backend*`
 * wrapper needed. A few stub-only fields those records carry (e.g.
 * VendorQuotation.quotedAmount, VendorCalendarEntry.endDatetime) simply come
 * back null from the real backend - harmless, see the individual template
 * comments for where that's handled.
 */
@Service
@RequiredArgsConstructor
public class BackendClient {

    private final RestClient backendRestClient;
    private final ObjectMapper objectMapper;

    public BackendAuthResponse loginWithGoogle(String googleIdToken) {
        return backendRestClient.post()
                .uri("/api/v1/auth/google")
                .body(Map.of("idToken", googleIdToken))
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::raise)
                .body(BackendAuthResponse.class);
    }

    public BackendSubscriptionStatus getSubscriptionStatus(String jwt) {
        return get("/api/v1/vendors/me/subscription", jwt, BackendSubscriptionStatus.class);
    }

    public List<BackendBillingHistoryEntry> getBillingHistory(String jwt) {
        return get("/api/v1/vendors/me/subscription/history", jwt, new ParameterizedTypeReference<List<BackendBillingHistoryEntry>>() {});
    }

    public BackendCreateSubscriptionResponse createSubscription(String jwt, String plan, String billingCycle) {
        return postJson("/api/v1/vendors/me/subscription", jwt, Map.of("plan", plan, "billingCycle", billingCycle),
                BackendCreateSubscriptionResponse.class);
    }

    public BackendSubscriptionStatus confirmSubscription(String jwt, Long vendorSubscriptionId) {
        return get("/api/v1/vendors/me/subscription/confirm?vendorSubscriptionId=" + vendorSubscriptionId, jwt,
                BackendSubscriptionStatus.class);
    }

    /**
     * PATCH /api/v1/users/me/vendor - the real vendor onboarding submission.
     * Field-for-field port of eventsrus-ui's vendor_api.dart#becomeVendor
     * (the one other real caller of this endpoint): same scalar fields, same
     * parallel-indexed legalDocumentFiles/Types/Labels arrays. Idempotent on
     * the backend - safe to call again for an already-VENDOR user (it just
     * upserts their existing profile), so no "already a vendor" guard here.
     * Returns a freshly-issued token/role, exactly like login - the caller
     * must WebSession.store(...) the result since role may have just become
     * VENDOR.
     */
    public BackendAuthResponse becomeVendor(
            String jwt,
            com.web.eventsrus.model.VendorOnboardingForm form,
            MultipartFile logo,
            MultipartFile idCard,
            MultipartFile selfie,
            List<MultipartFile> legalDocumentFiles,
            List<String> legalDocumentTypes,
            List<String> legalDocumentLabels) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        addIfPresent(body, "businessName", form.getBusinessName());
        addIfPresent(body, "businessType", form.getBusinessType() != null ? form.getBusinessType().name() : null);
        addIfPresent(body, "ownerName", form.getOwnerName());
        addIfPresent(body, "description", form.getDescription());
        addIfPresent(body, "contactEmail", form.getContactEmail());
        addIfPresent(body, "phoneNumber", form.getPhoneNumber());
        addIfPresent(body, "addressLine1", form.getAddressLine1());
        addIfPresent(body, "addressLine2", form.getAddressLine2());
        addIfPresent(body, "city", form.getCity());
        addIfPresent(body, "state", form.getState());
        addIfPresent(body, "postalCode", form.getPostalCode());
        addIfPresent(body, "country", form.getCountry());
        if (form.getOperatingAreas() != null) {
            form.getOperatingAreas().forEach(area -> body.add("operatingAreas", area));
        }
        if (form.getCateredEventTypes() != null) {
            form.getCateredEventTypes().forEach(type -> body.add("cateredEventTypes", type.name()));
        }
        body.add("acceptedTerms", String.valueOf(form.isAcceptedTerms()));
        addIfPresent(body, "recaptchaToken", form.getRecaptchaToken());
        addIfPresent(body, "referralCode", form.getReferralCode());
        addFileIfPresent(body, "logo", logo);
        addFileIfPresent(body, "idCard", idCard);
        addFileIfPresent(body, "selfie", selfie);
        if (legalDocumentFiles != null) {
            for (int i = 0; i < legalDocumentFiles.size(); i++) {
                MultipartFile file = legalDocumentFiles.get(i);
                if (file == null || file.isEmpty()) {
                    continue;
                }
                body.add("legalDocumentFiles", file.getResource());
                body.add("legalDocumentTypes",
                        legalDocumentTypes != null && i < legalDocumentTypes.size() ? legalDocumentTypes.get(i) : "OTHER");
                body.add("legalDocumentLabels",
                        legalDocumentLabels != null && i < legalDocumentLabels.size() ? legalDocumentLabels.get(i) : "");
            }
        }

        return backendRestClient.patch()
                .uri("/api/v1/users/me/vendor")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::raise)
                .body(BackendAuthResponse.class);
    }

    public BackendReferralOverview getReferralOverview(String jwt) {
        return get("/api/v1/vendors/me/referrals", jwt, BackendReferralOverview.class);
    }

    // --- Dashboard ---

    public VendorDashboard getDashboard(String jwt) {
        return get("/api/v1/vendors/me/dashboard", jwt, VendorDashboard.class);
    }

    // --- Leads ---

    public List<VendorLead> getLeads(String jwt) {
        return get("/api/v1/vendors/me/leads", jwt, new ParameterizedTypeReference<List<VendorLead>>() {});
    }

    // --- Bookings ---

    public List<VendorBooking> getBookings(String jwt) {
        return get("/api/v1/vendors/me/bookings", jwt, new ParameterizedTypeReference<List<VendorBooking>>() {});
    }

    public VendorBooking acknowledgeBookingPayment(String jwt, Long bookingId, MultipartFile invoice) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        addFileIfPresent(body, "invoice", invoice);
        return postMultipart("/api/v1/bookings/" + bookingId + "/acknowledge-payment", jwt, body, VendorBooking.class);
    }

    public VendorBooking rejectBookingPayment(String jwt, Long bookingId, String reason) {
        return putJson("/api/v1/bookings/" + bookingId + "/reject-payment", jwt, Map.of("reason", reason), VendorBooking.class);
    }

    public VendorBooking cancelBooking(String jwt, Long bookingId, String reason) {
        return putJson("/api/v1/bookings/" + bookingId + "/cancel", jwt, Map.of("reason", reason), VendorBooking.class);
    }

    public List<VendorBooking> getPlannerBookings(String jwt) {
        return get("/api/v1/planners/me/bookings", jwt, new ParameterizedTypeReference<List<VendorBooking>>() {});
    }

    public VendorBooking bookFromQuotation(
            String jwt, Long quotationId, java.math.BigDecimal price, Instant eventDatetime, String agreementDetails) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("price", price);
        body.put("eventDatetime", eventDatetime);
        body.put("agreementDetails", agreementDetails);
        return postJson("/api/v1/quotations/" + quotationId + "/book", jwt, body, VendorBooking.class);
    }

    public VendorBooking submitPaymentScreenshot(String jwt, Long bookingId, MultipartFile screenshot) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        addFileIfPresent(body, "screenshot", screenshot);
        return postMultipart("/api/v1/bookings/" + bookingId + "/payment-screenshot", jwt, body, VendorBooking.class);
    }

    // --- Conversations / messages ---

    public List<VendorConversation> getConversations(String jwt) {
        return get("/api/v1/conversations", jwt, new ParameterizedTypeReference<List<VendorConversation>>() {});
    }

    public List<VendorConversationMessage> getConversationMessages(String jwt, Long conversationId) {
        return get("/api/v1/conversations/" + conversationId + "/messages", jwt,
                new ParameterizedTypeReference<List<VendorConversationMessage>>() {});
    }

    public VendorConversationMessage replyToConversation(String jwt, Long conversationId, String message) {
        return postJson("/api/v1/conversations/" + conversationId + "/messages", jwt, Map.of("message", message),
                VendorConversationMessage.class);
    }

    /** Vendor-initiated first contact to a lead who's only browsed the storefront so far. */
    public VendorConversationMessage sendVendorMessage(String jwt, Long eventId, String message) {
        return postJson("/api/v1/events/" + eventId + "/vendor-messages", jwt, Map.of("message", message),
                VendorConversationMessage.class);
    }

    // --- Quotations ---

    public List<VendorQuotation> getQuotations(String jwt) {
        return get("/api/v1/vendors/me/quotations", jwt, new ParameterizedTypeReference<List<VendorQuotation>>() {});
    }

    public List<VendorQuotation> getPlannerQuotations(String jwt) {
        return get("/api/v1/planners/me/quotations", jwt, new ParameterizedTypeReference<List<VendorQuotation>>() {});
    }

    public VendorQuotation declineQuotation(String jwt, Long quotationId) {
        return backendRestClient.put()
                .uri("/api/v1/quotations/" + quotationId + "/decline")
                .header("Authorization", "Bearer " + jwt)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::raise)
                .body(VendorQuotation.class);
    }

    public VendorQuotation reviseQuotation(String jwt, Long quotationId, LocalDate targetDate, String message, List<Long> packageIds) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("targetDate", targetDate);
        body.put("message", message);
        body.put("packageIds", packageIds);
        return postJson("/api/v1/quotations/" + quotationId + "/revise", jwt, body, VendorQuotation.class);
    }

    // --- Events (planner) ---

    public PlannerEvent createEvent(String jwt, EventType eventType, LocalDate eventDate, String location, String description) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eventType", eventType != null ? eventType.name() : null);
        body.put("eventDate", eventDate);
        body.put("location", location);
        body.put("description", description);
        return postJson("/api/v1/events", jwt, body, PlannerEvent.class);
    }

    public List<PlannerEventSummary> listEvents(String jwt) {
        return get("/api/v1/events", jwt, new ParameterizedTypeReference<List<PlannerEventSummary>>() {});
    }

    public PlannerEvent getEvent(String jwt, Long eventId) {
        return get("/api/v1/events/" + eventId, jwt, PlannerEvent.class);
    }

    /** Names the event and marks it saved - splits out from createEvent since the real CreateEventRequest has no name field. */
    /** Sets/changes date and/or location after creation - the only way to supply these once past the initial intake form. */
    public PlannerEvent updateEventDetails(String jwt, Long eventId, LocalDate eventDate, String location) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eventDate", eventDate);
        body.put("location", location);
        return putJson("/api/v1/events/" + eventId + "/details", jwt, body, PlannerEvent.class);
    }

    public PlannerEvent saveEvent(String jwt, Long eventId, String name) {
        return putJson("/api/v1/events/" + eventId + "/save", jwt, Map.of("name", name), PlannerEvent.class);
    }

    // --- Calendar ---

    public List<VendorCalendarEntry> getCalendar(String jwt) {
        return get("/api/v1/vendors/me/calendar", jwt, new ParameterizedTypeReference<List<VendorCalendarEntry>>() {});
    }

    // --- Packages ---

    public List<VendorPackageItem> getPackages(String jwt) {
        return get("/api/v1/vendors/me/packages", jwt, new ParameterizedTypeReference<List<VendorPackageItem>>() {});
    }

    public VendorPackageItem addPackage(String jwt, VendorPackageForm form) {
        return postJson("/api/v1/vendors/me/packages", jwt, packageRequestBody(form), VendorPackageItem.class);
    }

    public VendorPackageItem updatePackage(String jwt, Long packageId, VendorPackageForm form) {
        return putJson("/api/v1/vendors/me/packages/" + packageId, jwt, packageRequestBody(form), VendorPackageItem.class);
    }

    private Map<String, Object> packageRequestBody(VendorPackageForm form) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", form.getName());
        body.put("description", form.getDescription());
        body.put("packageType", form.getPackageType() != null ? form.getPackageType().name() : null);
        body.put("pricingType", form.getPricingType() != null ? form.getPricingType().name() : null);
        body.put("price", form.getPrice());
        body.put("minPrice", form.getMinPrice());
        body.put("maxPrice", form.getMaxPrice());
        return body;
    }

    public VendorPackageImageItem addPackageImage(String jwt, Long packageId, MultipartFile image, String caption) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        addFileIfPresent(body, "image", image);
        addIfPresent(body, "caption", caption);
        return postMultipart("/api/v1/vendors/me/packages/" + packageId + "/images", jwt, body, VendorPackageImageItem.class);
    }

    public void deletePackageImage(String jwt, Long packageId, Long imageId) {
        delete("/api/v1/vendors/me/packages/" + packageId + "/images/" + imageId, jwt);
    }

    // --- Settings ---

    public BackendVendorSettingsResponse getSettings(String jwt) {
        return get("/api/v1/vendors/me/settings", jwt, BackendVendorSettingsResponse.class);
    }

    public BackendVendorSettingsResponse updateSettings(
            String jwt,
            com.web.eventsrus.model.VendorSettingsForm form,
            MultipartFile logo,
            MultipartFile idCard,
            MultipartFile selfie,
            MultipartFile cancellationPolicyFile,
            MultipartFile refundTermsFile) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        addIfPresent(body, "businessName", form.getBusinessName());
        addIfPresent(body, "ownerName", form.getOwnerName());
        addIfPresent(body, "businessType", form.getBusinessType() != null ? form.getBusinessType().name() : null);
        addIfPresent(body, "contactEmail", form.getContactEmail());
        addIfPresent(body, "phoneNumber", form.getPhoneNumber());
        addIfPresent(body, "addressLine1", form.getAddressLine1());
        addIfPresent(body, "addressLine2", form.getAddressLine2());
        addIfPresent(body, "city", form.getCity());
        addIfPresent(body, "state", form.getState());
        addIfPresent(body, "postalCode", form.getPostalCode());
        addIfPresent(body, "country", form.getCountry());
        addIfPresent(body, "primaryCategory", form.getPrimaryCategory() != null ? form.getPrimaryCategory().name() : null);
        if (form.getMaxGuestCapacity() != null) {
            body.add("maxGuestCapacity", String.valueOf(form.getMaxGuestCapacity()));
        }
        if (form.getMaxCustomersPerDay() != null) {
            body.add("maxCustomersPerDay", String.valueOf(form.getMaxCustomersPerDay()));
        }
        if (form.getBasePrice() != null) {
            body.add("basePrice", form.getBasePrice().toString());
        }
        if (form.getLeadTimeDays() != null) {
            body.add("leadTimeDays", String.valueOf(form.getLeadTimeDays()));
        }
        addIfPresent(body, "storefrontOverview", form.getStorefrontOverview());
        addIfPresent(body, "paymentInstructions", form.getPaymentInstructions());
        if (form.getOperatingAreas() != null) {
            form.getOperatingAreas().forEach(area -> body.add("operatingAreas", area));
        }
        if (form.getCateredEventTypes() != null) {
            form.getCateredEventTypes().forEach(type -> body.add("cateredEventTypes", type.name()));
        }
        addFileIfPresent(body, "logo", logo);
        addFileIfPresent(body, "idCard", idCard);
        addFileIfPresent(body, "selfie", selfie);
        addFileIfPresent(body, "cancellationPolicyFile", cancellationPolicyFile);
        addFileIfPresent(body, "refundTermsFile", refundTermsFile);

        return backendRestClient.put()
                .uri("/api/v1/vendors/me/settings")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::raise)
                .body(BackendVendorSettingsResponse.class);
    }

    // --- Legal documents ---

    public List<VendorLegalDocumentItem> getLegalDocuments(String jwt) {
        return get("/api/v1/vendors/me/legal-documents", jwt, new ParameterizedTypeReference<List<VendorLegalDocumentItem>>() {});
    }

    public VendorLegalDocumentItem addLegalDocument(String jwt, VendorLegalDocumentForm form, MultipartFile file) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        addIfPresent(body, "documentType", form.getDocumentType() != null ? form.getDocumentType().name() : null);
        addIfPresent(body, "label", form.getLabel());
        addFileIfPresent(body, "file", file);
        return postMultipart("/api/v1/vendors/me/legal-documents", jwt, body, VendorLegalDocumentItem.class);
    }

    public void deleteLegalDocument(String jwt, Long documentId) {
        delete("/api/v1/vendors/me/legal-documents/" + documentId, jwt);
    }

    // --- Payment methods ---

    public List<VendorPaymentMethodItem> getPaymentMethods(String jwt) {
        return get("/api/v1/vendors/me/payment-methods", jwt, new ParameterizedTypeReference<List<VendorPaymentMethodItem>>() {});
    }

    public VendorPaymentMethodItem addPaymentMethod(String jwt, VendorPaymentMethodForm form, MultipartFile qrImage) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        addIfPresent(body, "label", form.getLabel());
        addFileIfPresent(body, "qrImage", qrImage);
        return postMultipart("/api/v1/vendors/me/payment-methods", jwt, body, VendorPaymentMethodItem.class);
    }

    public void deletePaymentMethod(String jwt, Long paymentMethodId) {
        delete("/api/v1/vendors/me/payment-methods/" + paymentMethodId, jwt);
    }

    // --- Public storefront ---

    /**
     * jwt may be null - the storefront is publicly browsable (see
     * SecurityConfig's permitAll for GET /api/v1/vendors/{slug}), so an
     * anonymous visitor is a normal case, not an error. Passing a token when
     * one IS available (a logged-in planner) lets the backend record/refresh
     * a Lead for eventId, same as it always has.
     */
    public VendorPublicProfile getVendorProfile(String jwt, String slug, Long eventId) {
        String uri = "/api/v1/vendors/" + slug + (eventId != null ? "?eventId=" + eventId : "");
        RestClient.RequestHeadersSpec<?> request = backendRestClient.get().uri(uri);
        if (jwt != null) {
            request = request.header("Authorization", "Bearer " + jwt);
        }
        return request.retrieve().onStatus(HttpStatusCode::isError, this::raise).body(VendorPublicProfile.class);
    }

    /** Planner-initiated - requires the visiting PLANNER's own jwt, not the vendor's. */
    public VendorQuotation submitQuotationRequest(
            String jwt, Long eventId, Long vendorUserId, String plannerName, LocalDate targetDate, String message, List<Long> packageIds) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("plannerName", plannerName);
        body.put("targetDate", targetDate);
        body.put("message", message);
        body.put("packageIds", packageIds);
        // Real endpoint is QuotationController#requestQuotation - returns a
        // QuotationResponse (VendorQuotation), NOT a conversation message.
        return postJson("/api/v1/events/" + eventId + "/vendors/" + vendorUserId + "/quotations", jwt, body,
                VendorQuotation.class);
    }

    /** Planner-initiated - requires the visiting PLANNER's own jwt, not the vendor's. */
    public VendorConversationMessage submitInquiry(
            String jwt, Long eventId, Long vendorUserId, String plannerName, LocalDate targetDate, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("plannerName", plannerName);
        body.put("targetDate", targetDate);
        body.put("message", message);
        return postJson("/api/v1/events/" + eventId + "/vendors/" + vendorUserId + "/inquiries", jwt, body,
                VendorConversationMessage.class);
    }

    // --- Support tickets ---

    public List<SupportTicket> getSupportTickets(String jwt) {
        return get("/api/v1/support-tickets/me", jwt, new ParameterizedTypeReference<List<SupportTicket>>() {});
    }

    /**
     * Admin's "Vendor complaints" / "Planner complaints" split - every
     * ticket raised by a user of that role, not just the caller's own.
     * raisedByRole null means every ticket. jwt is the admin's own token
     * (see AdminSession) - the vendor-facing getSupportTicketMessages /
     * replyToSupportTicket / updateSupportTicketStatus methods below all
     * work unchanged for an admin too, since eventsrus-backend already lets
     * any admin act on any ticket, not just their own.
     */
    public List<SupportTicket> getSupportTicketsForAdmin(String jwt, String raisedByRole) {
        String uri = "/api/v1/admin/support-tickets"
                + (raisedByRole != null ? "?raisedByRole=" + raisedByRole : "");
        return get(uri, jwt, new ParameterizedTypeReference<List<SupportTicket>>() {});
    }

    public SupportTicket updateSupportTicketStatus(String jwt, Long ticketId, String status) {
        return putJson("/api/v1/support-tickets/" + ticketId + "/status", jwt, Map.of("status", status),
                SupportTicket.class);
    }

    public List<SupportTicketMessage> getSupportTicketMessages(String jwt, Long ticketId) {
        return get("/api/v1/support-tickets/" + ticketId + "/messages", jwt,
                new ParameterizedTypeReference<List<SupportTicketMessage>>() {});
    }

    public SupportTicket createSupportTicket(
            String jwt, String subject, String category, String message, Long relatedEventId, MultipartFile attachment) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        addIfPresent(body, "subject", subject);
        addIfPresent(body, "category", category);
        addIfPresent(body, "message", message);
        if (relatedEventId != null) {
            body.add("relatedEventId", relatedEventId);
        }
        addFileIfPresent(body, "attachment", attachment);
        return postMultipart("/api/v1/support-tickets", jwt, body, SupportTicket.class);
    }

    /** Returns the whole updated ticket (matches SupportTicketResponse), not just the new message. */
    public SupportTicket replyToSupportTicket(String jwt, Long ticketId, String message, MultipartFile attachment) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        addIfPresent(body, "message", message);
        addFileIfPresent(body, "attachment", attachment);
        return postMultipart("/api/v1/support-tickets/" + ticketId + "/messages", jwt, body, SupportTicket.class);
    }

    // --- Notifications (bell icon - see NotificationModelAttributes) ---

    /** Every notification for the caller, newest first - the backend doesn't paginate this yet. */
    public List<Notification> getNotifications(String jwt) {
        return get("/api/v1/notifications", jwt, new ParameterizedTypeReference<List<Notification>>() {});
    }

    public Notification markNotificationRead(String jwt, Long notificationId) {
        return backendRestClient.put()
                .uri("/api/v1/notifications/" + notificationId + "/read")
                .header("Authorization", "Bearer " + jwt)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::raise)
                .body(Notification.class);
    }

    // --- Admin (real accounts on the backend - see AdminAccountController) ---

    /**
     * Real username/password check against eventsrus-backend's admin_accounts
     * table, returning a genuine per-admin ADMIN-role JWT. Called once per
     * eventsrus-web admin login (see AdminAuthController), not per-request -
     * the caller holds onto the result for that admin's session. Returns
     * null (rather than throwing) on bad credentials or an unreachable
     * backend, so AdminAuthController can show one generic "invalid
     * username or password" message either way.
     */
    public String adminLogin(String username, String password) {
        try {
            Map<String, Object> body = backendRestClient.post()
                    .uri("/api/v1/admin/auth/login")
                    .body(Map.of("username", username, "password", password))
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            return body == null ? null : (String) body.get("token");
        } catch (Exception e) {
            return null;
        }
    }

    public List<BackendAdminAccount> listAdminAccounts(String adminJwt) {
        return get("/api/v1/admin/accounts", adminJwt, new ParameterizedTypeReference<List<BackendAdminAccount>>() {});
    }

    public BackendAdminAccount createAdminAccount(String adminJwt, String username, String password) {
        return postJson("/api/v1/admin/accounts", adminJwt, Map.of("username", username, "password", password),
                BackendAdminAccount.class);
    }

    public List<BackendAdminVendorListItem> listVendorsForAdmin(String adminJwt) {
        return get("/api/v1/admin/vendors", adminJwt, new ParameterizedTypeReference<List<BackendAdminVendorListItem>>() {});
    }

    public BackendVendorVerificationDocuments getVendorVerificationDocuments(String adminJwt, Long vendorUserId) {
        return get("/api/v1/admin/vendors/" + vendorUserId + "/verification-documents", adminJwt,
                BackendVendorVerificationDocuments.class);
    }

    // --- Planner's own profile (any logged-in user - a vendor has one too, just reached from a different page) ---

    public PlannerProfileForm getProfile(String jwt) {
        return get("/api/v1/users/me/profile", jwt, PlannerProfileForm.class);
    }

    /**
     * Returns a fresh token, not just the saved profile - email is editable
     * here and is also the JWT subject, so a caller who changes their own
     * email needs a reissued token or their next request would fail to
     * resolve to any user (see UserController#updateProfile). The caller
     * must store this back into the session (WebSession.store) - the token
     * this method was called with becomes stale the moment this succeeds.
     */
    public BackendAuthResponse updateProfile(String jwt, PlannerProfileForm form) {
        return putJson("/api/v1/users/me/profile", jwt, form, BackendAuthResponse.class);
    }

    // --- Events Coordinator (planner-facing AI ideas/advice assistant) ---

    public CoordinatorHistory getCoordinatorHistory(String jwt, Long eventId) {
        return get("/api/v1/events/" + eventId + "/coordinator/questions", jwt, CoordinatorHistory.class);
    }

    public CoordinatorQuestion askCoordinator(String jwt, Long eventId, String question) {
        return postJson("/api/v1/events/" + eventId + "/coordinator/ask", jwt, Map.of("question", question),
                CoordinatorQuestion.class);
    }

    public BackendAdminDashboardStats getAdminDashboardStats(String adminJwt) {
        return get("/api/v1/admin/dashboard", adminJwt, BackendAdminDashboardStats.class);
    }

    /** AdminPlannerListItemResponse's fields match model.AdminPlannerSummary field-for-field. */
    public List<AdminPlannerSummary> listPlannersForAdmin(String adminJwt) {
        return get("/api/v1/admin/planners", adminJwt, new ParameterizedTypeReference<List<AdminPlannerSummary>>() {});
    }

    public AdminPlannerSummary getPlannerForAdmin(String adminJwt, long plannerUserId) {
        return get("/api/v1/admin/planners/" + plannerUserId, adminJwt, AdminPlannerSummary.class);
    }

    public void verifyVendor(String adminJwt, Long vendorUserId, String adminUsername) {
        String uri = "/api/v1/admin/vendors/" + vendorUserId + "/verify"
                + (adminUsername != null ? "?adminUsername=" + java.net.URLEncoder.encode(adminUsername, java.nio.charset.StandardCharsets.UTF_8) : "");
        backendRestClient.post()
                .uri(uri)
                .header("Authorization", "Bearer " + adminJwt)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::raise)
                .toBodilessEntity();
    }

    public void unverifyVendor(String adminJwt, Long vendorUserId) {
        backendRestClient.post()
                .uri("/api/v1/admin/vendors/" + vendorUserId + "/unverify")
                .header("Authorization", "Bearer " + adminJwt)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::raise)
                .toBodilessEntity();
    }

    public void setTopVendor(String adminJwt, Long vendorUserId, boolean topVendor) {
        backendRestClient.post()
                .uri("/api/v1/admin/vendors/" + vendorUserId + (topVendor ? "/mark-top" : "/unmark-top"))
                .header("Authorization", "Bearer " + adminJwt)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::raise)
                .toBodilessEntity();
    }

    // --- Reviews ---

    public VendorReview submitReview(String jwt, Long bookingId, int rating, String comment) {
        return postJson("/api/v1/bookings/" + bookingId + "/review", jwt,
                Map.of("rating", rating, "comment", comment), VendorReview.class);
    }

    public VendorReview updateReview(String jwt, Long reviewId, int rating, String comment) {
        return putJson("/api/v1/reviews/" + reviewId, jwt,
                Map.of("rating", rating, "comment", comment), VendorReview.class);
    }

    public void setReviewHidden(String adminJwt, Long reviewId, boolean hidden) {
        backendRestClient.post()
                .uri("/api/v1/admin/reviews/" + reviewId + (hidden ? "/hide" : "/unhide"))
                .header("Authorization", "Bearer " + adminJwt)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::raise)
                .toBodilessEntity();
    }

    // --- Shared request helpers ---

    private <T> T get(String uri, String jwt, Class<T> type) {
        return backendRestClient.get()
                .uri(uri)
                .header("Authorization", "Bearer " + jwt)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::raise)
                .body(type);
    }

    private <T> T get(String uri, String jwt, ParameterizedTypeReference<T> type) {
        return backendRestClient.get()
                .uri(uri)
                .header("Authorization", "Bearer " + jwt)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::raise)
                .body(type);
    }

    private <T> T postJson(String uri, String jwt, Object body, Class<T> type) {
        return backendRestClient.post()
                .uri(uri)
                .header("Authorization", "Bearer " + jwt)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::raise)
                .body(type);
    }

    private <T> T putJson(String uri, String jwt, Object body, Class<T> type) {
        return backendRestClient.put()
                .uri(uri)
                .header("Authorization", "Bearer " + jwt)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::raise)
                .body(type);
    }

    private <T> T postMultipart(String uri, String jwt, MultiValueMap<String, Object> body, Class<T> type) {
        return backendRestClient.post()
                .uri(uri)
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::raise)
                .body(type);
    }

    private void delete(String uri, String jwt) {
        backendRestClient.delete()
                .uri(uri)
                .header("Authorization", "Bearer " + jwt)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::raise)
                .toBodilessEntity();
    }

    private void addIfPresent(MultiValueMap<String, Object> body, String key, String value) {
        if (value != null && !value.isBlank()) {
            body.add(key, value);
        }
    }

    private void addFileIfPresent(MultiValueMap<String, Object> body, String key, MultipartFile file) {
        if (file != null && !file.isEmpty()) {
            body.add(key, file.getResource());
        }
    }

    @SuppressWarnings("unchecked")
    private void raise(org.springframework.http.HttpRequest request,
            org.springframework.http.client.ClientHttpResponse response) throws java.io.IOException {
        String message;
        try {
            Map<String, Object> body = (Map<String, Object>) objectMapper.readValue(response.getBody(), Map.class);
            message = String.valueOf(body.getOrDefault("message", "Request to eventsrus-backend failed"));
        } catch (Exception e) {
            message = "Request to eventsrus-backend failed (" + response.getStatusCode() + ")";
        }
        throw new BackendApiException(message, response.getStatusCode().value());
    }
}
