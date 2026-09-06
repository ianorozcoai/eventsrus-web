package com.web.eventsrus.controller;

import com.web.eventsrus.model.BusinessType;
import com.web.eventsrus.model.CreateTicketForm;
import com.web.eventsrus.model.EventType;
import com.web.eventsrus.model.InquiryForm;
import com.web.eventsrus.model.LegalDocumentType;
import com.web.eventsrus.model.PackageType;
import com.web.eventsrus.model.PhilippineProvinces;
import com.web.eventsrus.model.PlannerVendorSuggestion;
import com.web.eventsrus.model.QuotationRequestForm;
import com.web.eventsrus.model.SupportTicket;
import com.web.eventsrus.model.SupportTicketMessage;
import com.web.eventsrus.model.TicketCategory;
import com.web.eventsrus.model.VendorBooking;
import com.web.eventsrus.model.VendorCalendarEntry;
import com.web.eventsrus.model.VendorConversation;
import com.web.eventsrus.model.VendorConversationMessage;
import com.web.eventsrus.model.VendorDashboard;
import com.web.eventsrus.model.VendorLead;
import com.web.eventsrus.model.VendorOnboardingForm;
import com.web.eventsrus.model.VendorPackageForm;
import com.web.eventsrus.model.VendorPackageItem;
import com.web.eventsrus.model.VendorLegalDocumentForm;
import com.web.eventsrus.model.VendorLegalDocumentItem;
import com.web.eventsrus.model.VendorPaymentMethodForm;
import com.web.eventsrus.model.VendorPaymentMethodItem;
import com.web.eventsrus.model.VendorPublicProfile;
import com.web.eventsrus.model.VendorQuotation;
import com.web.eventsrus.model.VendorSettingsDocuments;
import com.web.eventsrus.model.VendorSettingsForm;
import com.web.eventsrus.backend.BackendApiException;
import com.web.eventsrus.backend.BackendAuthResponse;
import com.web.eventsrus.backend.BackendClient;
import com.web.eventsrus.backend.WebSession;
import com.web.eventsrus.stub.StubDataService;
import jakarta.servlet.http.HttpSession;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tools.jackson.databind.ObjectMapper;

@Controller
@RequestMapping("/vendor")
public class VendorController {

    private final StubDataService stubDataService;
    private final ObjectMapper objectMapper;
    private final BackendClient backendClient;

    public VendorController(StubDataService stubDataService, ObjectMapper objectMapper, BackendClient backendClient) {
        this.stubDataService = stubDataService;
        this.objectMapper = objectMapper;
        this.backendClient = backendClient;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        VendorDashboard dashboard = stubDataService.load("vendor-dashboard.json", VendorDashboard.class);
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("activePage", "dashboard");
        return "vendor/dashboard";
    }

    @GetMapping("/onboarding")
    public String onboardingForm(Model model) {
        if (!model.containsAttribute("vendorOnboardingForm")) {
            VendorOnboardingForm form = new VendorOnboardingForm();
            form.setCountry("Philippines");
            model.addAttribute("vendorOnboardingForm", form);
        }
        model.addAttribute("businessTypes", BusinessType.values());
        model.addAttribute("provinces", PhilippineProvinces.ALL);
        model.addAttribute("operatingAreaOptions", PhilippineProvinces.OPERATING_AREA_OPTIONS);
        model.addAttribute("documentTypes", LegalDocumentType.values());
        return "vendor/onboarding";
    }

    // Real submission now - PATCH /api/v1/users/me/vendor on eventsrus-backend
    // (BackendClient#becomeVendor). Reachable by any logged-in user (see
    // WebMvcConfig), including a brand-new PLANNER - that's exactly who this
    // is for. On success the backend returns a freshly-issued token with
    // role=VENDOR, which replaces the session's current one.
    @PostMapping("/onboarding")
    public String submitOnboarding(
            @ModelAttribute VendorOnboardingForm vendorOnboardingForm,
            @RequestParam(required = false) MultipartFile logo,
            @RequestParam(required = false) MultipartFile idCard,
            @RequestParam(required = false) MultipartFile selfie,
            @RequestParam(required = false) List<MultipartFile> legalDocumentFiles,
            @RequestParam(required = false) List<String> legalDocumentTypes,
            @RequestParam(required = false) List<String> legalDocumentLabels,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            BackendAuthResponse response = backendClient.becomeVendor(
                    WebSession.token(session), vendorOnboardingForm, logo, idCard, selfie,
                    legalDocumentFiles, legalDocumentTypes, legalDocumentLabels);
            WebSession.store(session, response);
            return "redirect:/vendor/dashboard";
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("onboardingError", e.getMessage());
            redirectAttributes.addFlashAttribute("vendorOnboardingForm", vendorOnboardingForm);
            return "redirect:/vendor/onboarding";
        }
    }

    @GetMapping("/leads")
    public String leads(Model model) {
        List<VendorLead> leads = stubDataService.loadList("leads.json", VendorLead.class).stream()
                .sorted(Comparator.comparing(VendorLead::lastVisitedAt).reversed())
                .toList();
        model.addAttribute("leads", leads);
        // A lead who's already messaged has a real thread to jump back
        // into; one who's only browsed the storefront doesn't - the
        // template uses this to decide between "Send Message" linking
        // straight to that conversation vs. opening a compose modal.
        Map<Long, Long> conversationIdByEventId = stubDataService.loadList("messages.json", VendorConversation.class)
                .stream()
                .collect(Collectors.toMap(VendorConversation::eventId, VendorConversation::id, (a, b) -> a));
        model.addAttribute("conversationIdByEventId", conversationIdByEventId);
        model.addAttribute("activePage", "leads");
        model.addAttribute("pageTitle", "Leads");
        return "vendor/leads";
    }

    @PostMapping("/leads/{eventId}/message")
    public String sendLeadMessage(
            @PathVariable Long eventId, @RequestParam String message, HttpSession session,
            RedirectAttributes redirectAttributes) {
        // Stub only for now - proves the round trip; wires up to
        // POST /api/v1/events/{eventId}/vendor-messages on eventsrus-backend
        // later (ConversationService#sendVendorMessage already does this
        // for real - finds or creates the conversation and posts the
        // opening message). Doesn't actually create a conversation here.
        // Gating check is real (mirrors ConversationService#sendVendorMessage's
        // subscription requirement) even though the send itself is still a stub.
        if (WebSession.isSubscriptionExpired(session)) {
            redirectAttributes.addFlashAttribute(
                    "leadsError", "Your subscription has ended. Renew your plan to message leads.");
            return "redirect:/vendor/leads";
        }
        redirectAttributes.addFlashAttribute("leadMessageSent", true);
        return "redirect:/vendor/leads";
    }

    @GetMapping("/bookings")
    public String bookings(Model model) {
        List<VendorBooking> bookings = stubDataService.loadList("bookings.json", VendorBooking.class).stream()
                .sorted(Comparator.comparing(VendorBooking::eventDatetime))
                .toList();
        model.addAttribute("bookings", bookings);
        model.addAttribute("activePage", "bookings");
        model.addAttribute("pageTitle", "Bookings");
        return "vendor/bookings";
    }

    @PostMapping("/bookings/{bookingId}/acknowledge-payment")
    public String acknowledgeBookingPayment(
            @PathVariable Long bookingId, @RequestParam(required = false) MultipartFile invoice,
            HttpSession session, RedirectAttributes redirectAttributes) {
        // Stub only for now - proves the round trip; wires up to
        // POST /api/v1/bookings/{bookingId}/acknowledge-payment on
        // eventsrus-backend later (BookingService#acknowledgePayment already
        // requires this same invoice/receipt upload for real - see its
        // Javadoc). Doesn't actually flip the booking's status or persist
        // the file anywhere (StubDataService re-reads the static JSON every
        // request, same limitation as every other stub POST here).
        // Gating check is real (mirrors BookingService#acknowledgePayment's
        // subscription requirement) even though the acknowledgement itself is stub.
        if (WebSession.isSubscriptionExpired(session)) {
            redirectAttributes.addFlashAttribute(
                    "bookingsError", "Your subscription has ended. Renew your plan to acknowledge payments.");
            return "redirect:/vendor/bookings";
        }
        if (invoice == null || invoice.isEmpty()) {
            redirectAttributes.addFlashAttribute("bookingsError",
                    "An invoice or receipt document is required to acknowledge this payment.");
            return "redirect:/vendor/bookings";
        }
        redirectAttributes.addFlashAttribute("paymentAcknowledged", true);
        return "redirect:/vendor/bookings";
    }

    @PostMapping("/bookings/{bookingId}/reject-payment")
    public String rejectBookingPayment(
            @PathVariable Long bookingId, @RequestParam String reason, RedirectAttributes redirectAttributes) {
        // Stub only for now - wires up to
        // PUT /api/v1/bookings/{bookingId}/reject-payment on eventsrus-backend later.
        if (reason == null || reason.isBlank()) {
            redirectAttributes.addFlashAttribute("bookingsError", "A rejection reason is required.");
            return "redirect:/vendor/bookings";
        }
        redirectAttributes.addFlashAttribute("paymentRejected", true);
        return "redirect:/vendor/bookings";
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    public String cancelBooking(
            @PathVariable Long bookingId, @RequestParam String reason, RedirectAttributes redirectAttributes) {
        // Stub only for now - proves the round trip; wires up to
        // PUT /api/v1/bookings/{bookingId}/cancel on eventsrus-backend later
        // (BookingService#cancel already does this for real - fixes
        // BookingStatus.CANCELLED having existed with no code path that
        // ever set it). Doesn't actually flip the booking's status here.
        if (reason == null || reason.isBlank()) {
            redirectAttributes.addFlashAttribute("bookingsError", "A cancellation reason is required.");
            return "redirect:/vendor/bookings";
        }
        redirectAttributes.addFlashAttribute("bookingCancelled", true);
        return "redirect:/vendor/bookings";
    }

    // The stub vendor's own user id (matches vendorUserId across the other
    // stub JSON files) - used to tell "sent" bubbles from "received" ones.
    private static final long VENDOR_USER_ID = 9001L;

    // Support tickets - symmetric with PlannerSupportController's /planner/support.
    // Two-pane messenger-style layout (ticket list + selected thread), same
    // shape as /vendor/messages.

    @GetMapping("/support")
    public String support(@RequestParam(required = false) Long ticketId, Model model) {
        List<SupportTicket> tickets = stubDataService.loadList("support-tickets.json", SupportTicket.class).stream()
                .filter(t -> t.raisedByUserId() == VENDOR_USER_ID)
                .sorted(Comparator.comparing(SupportTicket::createdAt).reversed())
                .toList();

        SupportTicket selectedTicket = tickets.stream()
                .filter(t -> t.id() == (ticketId != null ? ticketId : -1))
                .findFirst()
                .orElseGet(() -> tickets.isEmpty() ? null : tickets.get(0));

        Map<String, List<SupportTicketMessage>> threads =
                stubDataService.loadMapOfLists("support-ticket-messages.json", SupportTicketMessage.class);
        List<SupportTicketMessage> thread = selectedTicket == null
                ? List.of()
                : threads.getOrDefault(String.valueOf(selectedTicket.id()), List.of());

        if (!model.containsAttribute("createTicketForm")) {
            model.addAttribute("createTicketForm", new CreateTicketForm());
        }
        model.addAttribute("tickets", tickets);
        model.addAttribute("selectedTicket", selectedTicket);
        model.addAttribute("thread", thread);
        model.addAttribute("ticketCategories", TicketCategory.values());
        model.addAttribute("currentUserId", VENDOR_USER_ID);
        model.addAttribute("activePage", "support");
        model.addAttribute("pageTitle", "Support");
        return "vendor/support";
    }

    @PostMapping("/support")
    public String createTicket(@ModelAttribute CreateTicketForm createTicketForm, RedirectAttributes redirectAttributes) {
        // Stub only for now - proves the round trip; wires up to
        // POST /api/v1/support-tickets on eventsrus-backend later
        // (SupportTicketService#createTicket already does this for real).
        redirectAttributes.addFlashAttribute("ticketCreated", true);
        return "redirect:/vendor/support";
    }

    @PostMapping("/support/{ticketId}/reply")
    public String replyToTicket(
            @PathVariable Long ticketId, @RequestParam String message, RedirectAttributes redirectAttributes) {
        // Stub only for now - wires up to
        // POST /api/v1/support-tickets/{id}/messages on eventsrus-backend
        // later (SupportTicketService#reply already does this for real).
        // Doesn't actually append to the thread (StubDataService re-reads
        // the static JSON every request), same limitation as every other
        // stub reply in this app.
        if (message == null || message.isBlank()) {
            redirectAttributes.addFlashAttribute("supportError", "A message is required.");
        } else {
            redirectAttributes.addFlashAttribute("replySent", true);
        }
        redirectAttributes.addAttribute("ticketId", ticketId);
        return "redirect:/vendor/support";
    }

    @GetMapping("/messages")
    public String messages(@RequestParam(required = false) Long conversationId, Model model) {
        List<VendorConversation> conversations = stubDataService
                .loadList("messages.json", VendorConversation.class).stream()
                .sorted(Comparator.comparing(VendorConversation::lastMessageAt).reversed())
                .toList();

        VendorConversation selected = conversations.stream()
                .filter(c -> c.id() == (conversationId != null ? conversationId : -1))
                .findFirst()
                .orElseGet(() -> conversations.isEmpty() ? null : conversations.get(0));

        Map<String, List<VendorConversationMessage>> threads =
                stubDataService.loadMapOfLists("conversation-messages.json", VendorConversationMessage.class);
        List<VendorConversationMessage> thread = selected == null
                ? List.of()
                : threads.getOrDefault(String.valueOf(selected.id()), List.of()).stream()
                        .sorted(Comparator.comparing(VendorConversationMessage::createdAt))
                        .toList();

        model.addAttribute("conversations", conversations);
        model.addAttribute("selectedConversation", selected);
        model.addAttribute("thread", thread);
        model.addAttribute("vendorUserId", VENDOR_USER_ID);
        model.addAttribute("activePage", "messages");
        model.addAttribute("pageTitle", "Messages");
        return "vendor/messages";
    }

    @PostMapping("/messages/{conversationId}/reply")
    public String replyToConversation(
            @PathVariable Long conversationId, @RequestParam String body, HttpSession session,
            RedirectAttributes redirectAttributes) {
        // Stub only for now - proves the round trip; wires up to
        // POST /api/v1/conversations/{id}/messages on eventsrus-backend later.
        // Doesn't actually append to the thread (StubDataService re-reads the
        // static JSON every request), so the reply won't appear after redirect.
        // Gating check is real (mirrors ConversationService#sendMessage's
        // vendor-side subscription requirement) even though the reply itself is stub.
        if (WebSession.isSubscriptionExpired(session)) {
            redirectAttributes.addFlashAttribute(
                    "messagesError", "Your subscription has ended. Renew your plan to reply to messages.");
            return "redirect:/vendor/messages?conversationId=" + conversationId;
        }
        redirectAttributes.addFlashAttribute("replySent", true);
        return "redirect:/vendor/messages?conversationId=" + conversationId;
    }

    @GetMapping("/quotations")
    public String quotations(Model model) {
        List<VendorQuotation> quotations = stubDataService
                .loadList("quotations.json", VendorQuotation.class).stream()
                .sorted(Comparator.comparing(VendorQuotation::createdAt).reversed())
                .toList();
        model.addAttribute("quotations", quotations);
        model.addAttribute("activePage", "quotations");
        model.addAttribute("pageTitle", "Quotations");
        return "vendor/quotations";
    }

    @GetMapping("/calendar")
    public String calendar(Model model) {
        List<VendorCalendarEntry> entries = stubDataService
                .loadList("calendar.json", VendorCalendarEntry.class).stream()
                .sorted(Comparator.comparing(VendorCalendarEntry::eventDatetime))
                .toList();
        model.addAttribute("entries", entries);
        model.addAttribute("calendarEventsJson", toFullCalendarEventsJson(entries));
        model.addAttribute("activePage", "calendar");
        model.addAttribute("pageTitle", "Calendar");
        return "vendor/calendar";
    }

    /** Converts calendar entries into the JSON array FullCalendar's {@code events} option expects. */
    private String toFullCalendarEventsJson(List<VendorCalendarEntry> entries) {
        List<Map<String, Object>> events = entries.stream()
                .map(entry -> {
                    Map<String, Object> event = new LinkedHashMap<>();
                    event.put("title", entry.eventName());
                    event.put("start", entry.eventDatetime().toString());
                    if (entry.endDatetime() != null) {
                        event.put("end", entry.endDatetime().toString());
                    }
                    event.put("display", "block");
                    event.put("color", switch (entry.status()) {
                        case "BOOKED" -> "#198754";
                        case "INQUIRY" -> "#ffc107";
                        case "CANCELLED" -> "#dc3545";
                        default -> "#6c757d";
                    });
                    return event;
                })
                .toList();
        return objectMapper.writeValueAsString(events);
    }

    @GetMapping("/packages")
    public String packages(Model model) {
        List<VendorPackageItem> packages = stubDataService.loadList("packages.json", VendorPackageItem.class);
        model.addAttribute("packages", packages);
        model.addAttribute("activePage", "packages");
        model.addAttribute("pageTitle", "Packages");
        if (!model.containsAttribute("vendorPackageForm")) {
            model.addAttribute("vendorPackageForm", new VendorPackageForm());
        }
        model.addAttribute("packageTypes", PackageType.values());
        return "vendor/packages";
    }

    @PostMapping("/packages")
    public String addPackage(
            @ModelAttribute VendorPackageForm vendorPackageForm, RedirectAttributes redirectAttributes) {
        // Stub only for now - proves the round trip; wires up to
        // POST /api/v1/vendors/me/packages on eventsrus-backend later.
        redirectAttributes.addFlashAttribute("packageAdded", true);
        redirectAttributes.addFlashAttribute("addedPackageName", vendorPackageForm.getName());
        return "redirect:/vendor/packages";
    }

    // Public storefront - what a planner sees when they open "View My Page"
    // (the vendor's own page, unchanged) or click "View Storefront" from a
    // planner event's Overview tab (a specific slugged vendor - see
    // PlannerEvent/PlannerVendorSuggestion). Opens in its own tab, so it's a
    // standalone page (no vendor sidebar), same pattern as the onboarding page.

    @GetMapping("/storefront")
    public String storefront(Model model) {
        loadStorefront(model, "vendor-public-profile.json", null, null);
        return "vendor/storefront";
    }

    @GetMapping("/storefront/{slug}")
    public String storefrontBySlug(
            @PathVariable String slug, @RequestParam(required = false) Long eventId, Model model) {
        if (!model.containsAttribute("quotationRequestForm")) {
            model.addAttribute("quotationRequestForm", new QuotationRequestForm());
        }
        if (!model.containsAttribute("inquiryForm")) {
            model.addAttribute("inquiryForm", new InquiryForm());
        }
        model.addAttribute("profile", loadOrSynthesizeProfile(slug));
        model.addAttribute("redirectSlug", slug);
        model.addAttribute("eventId", eventId);
        return "vendor/storefront";
    }

    private void loadStorefront(Model model, String stubFileName, String slug, Long eventId) {
        if (!model.containsAttribute("quotationRequestForm")) {
            model.addAttribute("quotationRequestForm", new QuotationRequestForm());
        }
        if (!model.containsAttribute("inquiryForm")) {
            model.addAttribute("inquiryForm", new InquiryForm());
        }
        VendorPublicProfile profile = stubDataService.load(stubFileName, VendorPublicProfile.class);
        model.addAttribute("profile", profile);
        model.addAttribute("redirectSlug", slug);
        model.addAttribute("eventId", eventId);
    }

    // Only 3 vendors in stubs/vendor-directory.json have a hand-authored
    // stubs/storefronts/{slug}.json (full packages/reviews/gallery). Every
    // other vendor still needs a real, correctly-labeled storefront rather
    // than a dead link, so this synthesizes one on the fly from the
    // matching directory entry when no bespoke file exists - real fields
    // from the directory, honest generic defaults for what it doesn't carry.
    private VendorPublicProfile loadOrSynthesizeProfile(String slug) {
        try {
            return stubDataService.load("storefronts/" + slug + ".json", VendorPublicProfile.class);
        } catch (UncheckedIOException notFound) {
            PlannerVendorSuggestion match = stubDataService
                    .loadList("vendor-directory.json", PlannerVendorSuggestion.class).stream()
                    .filter(v -> slug.equals(v.slug()))
                    .findFirst()
                    .orElseThrow(() -> notFound);
            return new VendorPublicProfile(
                    9100L + Math.abs(slug.hashCode() % 900),
                    match.businessName(),
                    null,
                    match.description(),
                    null,
                    match.businessType(),
                    match.city(),
                    match.operatingAreas().isEmpty() ? null : match.operatingAreas().get(0),
                    "Philippines",
                    null,
                    null,
                    null,
                    String.join(", ", match.operatingAreas()),
                    4.5,
                    0,
                    "Usually within a day",
                    List.of(),
                    false,
                    List.of(),
                    List.of(),
                    List.of(),
                    null,
                    List.of());
        }
    }

    @PostMapping("/storefront/quotation-request")
    public String submitQuotationRequest(
            @ModelAttribute QuotationRequestForm quotationRequestForm,
            @RequestParam(required = false) String redirectSlug,
            @RequestParam(required = false) Long eventId,
            RedirectAttributes redirectAttributes) {
        // Stub only for now - proves the round trip; wires up to
        // POST /api/v1/vendors/{slug}/quotations on eventsrus-backend later.
        return redirectAfterStorefrontSubmit(
                redirectSlug, eventId, "quotationRequestSubmitted",
                "Your quotation request has been sent. (stub only for now - won't appear in the thread below)",
                redirectAttributes);
    }

    @PostMapping("/storefront/inquiry")
    public String submitInquiry(
            @ModelAttribute InquiryForm inquiryForm,
            @RequestParam(required = false) String redirectSlug,
            @RequestParam(required = false) Long eventId,
            RedirectAttributes redirectAttributes) {
        // Stub only for now - wires up to a real conversation-start endpoint
        // later (ConversationService#sendInquiry already does this for real
        // on eventsrus-backend, finding or creating the (event, vendor)
        // conversation and posting the opening message).
        return redirectAfterStorefrontSubmit(
                redirectSlug, eventId, "inquirySubmitted",
                "Your inquiry has been sent. (stub only for now - won't appear in the thread below)",
                redirectAttributes);
    }

    // When reached from a planner event's Overview tab (eventId present),
    // send the planner back into that event's Chats tab instead of just
    // back to the storefront - that's the "inquiry lands in the chat"
    // moment, even though nothing is actually persisted yet.
    private String redirectAfterStorefrontSubmit(
            String redirectSlug, Long eventId, String plainFlashKey, String eventFlashMessage, RedirectAttributes redirectAttributes) {
        if (eventId != null) {
            redirectAttributes.addFlashAttribute("chatNotice", eventFlashMessage);
            return "redirect:/planner/events?eventId=" + eventId + "&tab=chats";
        }
        redirectAttributes.addFlashAttribute(plainFlashKey, true);
        return redirectSlug != null ? "redirect:/vendor/storefront/" + redirectSlug : "redirect:/vendor/storefront";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        if (!model.containsAttribute("vendorSettingsForm")) {
            VendorSettingsForm form = stubDataService.load("vendor-settings.json", VendorSettingsForm.class);
            model.addAttribute("vendorSettingsForm", form);
        }
        if (!model.containsAttribute("settingsDocuments")) {
            model.addAttribute(
                    "settingsDocuments",
                    stubDataService.load("vendor-settings-documents.json", VendorSettingsDocuments.class));
        }
        if (!model.containsAttribute("vendorPaymentMethodForm")) {
            model.addAttribute("vendorPaymentMethodForm", new VendorPaymentMethodForm());
        }
        List<VendorPaymentMethodItem> paymentMethods =
                stubDataService.loadList("vendor-payment-methods.json", VendorPaymentMethodItem.class);
        model.addAttribute("paymentMethods", paymentMethods);
        if (!model.containsAttribute("vendorLegalDocumentForm")) {
            model.addAttribute("vendorLegalDocumentForm", new VendorLegalDocumentForm());
        }
        List<VendorLegalDocumentItem> legalDocuments =
                stubDataService.loadList("vendor-legal-documents.json", VendorLegalDocumentItem.class);
        model.addAttribute("legalDocuments", legalDocuments);
        model.addAttribute("documentTypes", LegalDocumentType.values());
        model.addAttribute("businessTypes", BusinessType.values());
        model.addAttribute("provinces", PhilippineProvinces.ALL);
        model.addAttribute("operatingAreaOptions", PhilippineProvinces.OPERATING_AREA_OPTIONS);
        model.addAttribute("eventTypeOptions", EventType.values());
        model.addAttribute("activePage", "settings");
        model.addAttribute("pageTitle", "Account Settings");
        return "vendor/settings";
    }

    // businessPermit is gone from this form - legal documents (any number of
    // them) are now added/removed one at a time via the endpoints below,
    // same pattern as payment methods, instead of being folded into this
    // big Save Changes submit.
    @PostMapping("/settings")
    public String updateSettings(
            @ModelAttribute VendorSettingsForm vendorSettingsForm,
            @RequestParam(required = false) MultipartFile logo,
            @RequestParam(required = false) MultipartFile idCard,
            @RequestParam(required = false) MultipartFile selfie,
            @RequestParam(required = false) MultipartFile cancellationPolicyFile,
            @RequestParam(required = false) MultipartFile refundTermsFile,
            RedirectAttributes redirectAttributes) {
        // Mirrors eventsrus-backend's own PDF-only validation on this
        // endpoint (VendorSettingsController/UserService#requirePdf) - stub
        // only for now otherwise, doesn't actually store any of these files.
        if (!isPdfOrEmpty(cancellationPolicyFile) || !isPdfOrEmpty(refundTermsFile)) {
            redirectAttributes.addFlashAttribute("settingsError", "Only PDF files are accepted for cancellation policy and refund terms.");
            return "redirect:/vendor/settings";
        }
        redirectAttributes.addFlashAttribute("settingsSaved", true);
        return "redirect:/vendor/settings";
    }

    @PostMapping("/settings/legal-documents")
    public String addLegalDocument(
            @ModelAttribute VendorLegalDocumentForm vendorLegalDocumentForm,
            @RequestParam(required = false) MultipartFile file,
            RedirectAttributes redirectAttributes) {
        // Stub only for now - proves the round trip; wires up to
        // POST /api/v1/vendors/me/legal-documents on eventsrus-backend later
        // (VendorLegalDocumentService#create already does this for real -
        // replacing the old single business-permit upload, since a vendor
        // can have several of these: DTI, SEC, Mayor's Permit, Barangay
        // Clearance, BIR, ...). Doesn't actually store the file or add it
        // to the list (StubDataService re-reads the static JSON every
        // request, same limitation as every other stub POST here).
        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("settingsError", "A document file is required.");
            return "redirect:/vendor/settings";
        }
        redirectAttributes.addFlashAttribute("legalDocumentAdded", true);
        return "redirect:/vendor/settings";
    }

    @PostMapping("/settings/legal-documents/{documentId}/delete")
    public String deleteLegalDocument(@PathVariable Long documentId, RedirectAttributes redirectAttributes) {
        // Stub only for now - wires up to
        // DELETE /api/v1/vendors/me/legal-documents/{id} on eventsrus-backend later.
        redirectAttributes.addFlashAttribute("legalDocumentRemoved", true);
        return "redirect:/vendor/settings";
    }

    private boolean isPdfOrEmpty(MultipartFile file) {
        return file == null || file.isEmpty() || "application/pdf".equals(file.getContentType());
    }

    @PostMapping("/settings/payment-methods")
    public String addPaymentMethod(
            @ModelAttribute VendorPaymentMethodForm vendorPaymentMethodForm,
            @RequestParam(required = false) MultipartFile qrImage,
            RedirectAttributes redirectAttributes) {
        // Mirrors eventsrus-backend's own image-type validation on this
        // endpoint (VendorPaymentMethodController/Service#requireImage) -
        // stub only for now otherwise, doesn't actually store the QR image
        // or add it to the list (StubDataService re-reads the static JSON
        // every request, same limitation as every other stub POST here).
        if (qrImage == null || qrImage.isEmpty()) {
            redirectAttributes.addFlashAttribute("settingsError", "A QR code image is required.");
            return "redirect:/vendor/settings";
        }
        if (!isPngOrJpeg(qrImage)) {
            redirectAttributes.addFlashAttribute("settingsError", "Only PNG or JPEG images are accepted for payment method QR codes.");
            return "redirect:/vendor/settings";
        }
        redirectAttributes.addFlashAttribute("paymentMethodAdded", true);
        return "redirect:/vendor/settings";
    }

    private boolean isPngOrJpeg(MultipartFile file) {
        String contentType = file.getContentType();
        return "image/png".equals(contentType) || "image/jpeg".equals(contentType);
    }
}
