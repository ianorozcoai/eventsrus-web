package com.web.eventsrus.controller;

import com.web.eventsrus.model.BusinessType;
import com.web.eventsrus.model.CreateTicketForm;
import com.web.eventsrus.model.EventType;
import com.web.eventsrus.model.InquiryForm;
import com.web.eventsrus.model.LegalDocumentType;
import com.web.eventsrus.model.PackageType;
import com.web.eventsrus.model.PaymentType;
import com.web.eventsrus.model.PhilippineProvinces;
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
import com.web.eventsrus.model.VendorPaymentMethodForm;
import com.web.eventsrus.model.VendorPublicProfile;
import com.web.eventsrus.model.VendorQuotation;
import com.web.eventsrus.model.VendorSettingsDocuments;
import com.web.eventsrus.model.VendorSettingsForm;
import com.web.eventsrus.backend.BackendApiException;
import com.web.eventsrus.backend.BackendAuthResponse;
import com.web.eventsrus.backend.BackendClient;
import com.web.eventsrus.backend.BackendQuotationHistoryEntry;
import com.web.eventsrus.backend.BackendVendorSettingsResponse;
import com.web.eventsrus.backend.WebSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tools.jackson.databind.ObjectMapper;

/**
 * Every vendor business page - all wired to real eventsrus-backend data via
 * BackendClient now (no more StubDataService here). WebMvcConfig gates every
 * route below except /onboarding and /storefront/{slug} to a logged-in
 * VENDOR, so WebSession.token(session) is always present in those handlers.
 */
@Controller
@RequestMapping("/vendor")
public class VendorController {

    private final ObjectMapper objectMapper;
    private final BackendClient backendClient;

    @Value("${recaptcha.site-key}")
    private String recaptchaSiteKey;

    public VendorController(ObjectMapper objectMapper, BackendClient backendClient) {
        this.objectMapper = objectMapper;
        this.backendClient = backendClient;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        VendorDashboard dashboard = backendClient.getDashboard(WebSession.token(session));
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("activePage", "dashboard");

        // Fun first-package nudge - shown once per login (mirrors the
        // paywall modal's own "not on every page navigation" pattern), and
        // only for as long as the vendor genuinely has zero packages.
        boolean nudgeAlreadyShown = Boolean.TRUE.equals(session.getAttribute(WebSession.FIRST_PACKAGE_NUDGE_SHOWN));
        boolean showFirstPackageNudge = !dashboard.hasPackages() && !nudgeAlreadyShown;
        if (showFirstPackageNudge) {
            session.setAttribute(WebSession.FIRST_PACKAGE_NUDGE_SHOWN, true);
        }
        model.addAttribute("showFirstPackageNudge", showFirstPackageNudge);
        return "vendor/dashboard";
    }

    /**
     * Marks a bell-dropdown notification read and sends the vendor to
     * wherever that kind of thing lives - there's no per-item deep link
     * (a booking notification doesn't carry, say, which tab to open), just
     * the right list page. Falls back to the dashboard for anything
     * unrecognized rather than erroring.
     */
    @PostMapping("/notifications/{id}/read")
    public String markNotificationRead(@PathVariable Long id, @RequestParam(required = false) String relatedEntityType,
            HttpSession session) {
        try {
            backendClient.markNotificationRead(WebSession.token(session), id);
        } catch (BackendApiException e) {
            // Already read, not this vendor's, or the backend hiccuped -
            // still send them somewhere sensible rather than showing an error.
        }
        String type = relatedEntityType == null ? "" : relatedEntityType;
        return "redirect:" + switch (type) {
            case "BOOKING" -> "/vendor/bookings";
            case "CONVERSATION" -> "/vendor/messages";
            case "LEAD" -> "/vendor/leads";
            case "QUOTATION" -> "/vendor/quotations";
            case "SUPPORT_TICKET" -> "/vendor/support";
            default -> "/vendor/dashboard";
        };
    }

    @GetMapping("/onboarding")
    public String onboardingForm(HttpServletRequest request, HttpSession session, Model model) {
        if (!model.containsAttribute("vendorOnboardingForm")) {
            VendorOnboardingForm form = new VendorOnboardingForm();
            form.setCountry("Philippines");
            form.setReferralCode(WebSession.referralCode(session));
            // Default to the Google account's own email - still just a
            // starting point, not locked: the field stays editable in case
            // they'd rather use a different business contact address.
            form.setContactEmail(WebSession.email(session));
            model.addAttribute("vendorOnboardingForm", form);
        }
        model.addAttribute("businessTypes", BusinessType.displayOrder());
        model.addAttribute("provinces", PhilippineProvinces.ALL);
        model.addAttribute("operatingAreaOptions", PhilippineProvinces.OPERATING_AREA_OPTIONS);
        model.addAttribute("eventTypeOptions", EventType.displayOrder());
        model.addAttribute("documentTypes", LegalDocumentType.values());
        model.addAttribute("recaptchaSiteKey", recaptchaSiteKey);
        // "Fill Sample Data" is a dev-only convenience - never show it off a
        // localhost URL, so it can't end up live in production by accident.
        String host = request.getServerName();
        model.addAttribute("showTestingTools", "localhost".equals(host) || "127.0.0.1".equals(host));
        return "vendor/onboarding";
    }

    // Real submission - PATCH /api/v1/users/me/vendor on eventsrus-backend
    // (BackendClient#becomeVendor). Reachable by any logged-in user (see
    // WebMvcConfig), including a brand-new PLANNER - that's exactly who this
    // is for. On success the backend returns a freshly-issued token with
    // role=VENDOR, which replaces the session's current one.
    //
    // AJAX (JSON in/out), not a classic redirect - a <input type="file">
    // can never be repopulated by the browser after a page navigation, so
    // a redirect-on-failure flow would silently throw away every file the
    // vendor had already picked (logo/ID/selfie/legal documents) the moment
    // any validation failed. Submitting via fetch() and staying on the same
    // page (see onboarding.html's script) means a failure never navigates
    // away at all, so nothing the vendor already filled in or picked is lost.
    @PostMapping("/onboarding")
    @ResponseBody
    public OnboardingResult submitOnboarding(
            @ModelAttribute VendorOnboardingForm vendorOnboardingForm,
            @RequestParam(required = false) MultipartFile logo,
            @RequestParam(required = false) MultipartFile idCard,
            @RequestParam(required = false) MultipartFile selfie,
            @RequestParam(required = false) List<MultipartFile> legalDocumentFiles,
            @RequestParam(required = false) List<String> legalDocumentTypes,
            @RequestParam(required = false) List<String> legalDocumentLabels,
            HttpSession session) {
        try {
            BackendAuthResponse response = backendClient.becomeVendor(
                    WebSession.token(session), vendorOnboardingForm, logo, idCard, selfie,
                    legalDocumentFiles, legalDocumentTypes, legalDocumentLabels);
            WebSession.store(session, response);
            return new OnboardingResult(true, null, "/vendor/dashboard");
        } catch (BackendApiException e) {
            return new OnboardingResult(false, e.getMessage(), null);
        }
    }

    public record OnboardingResult(boolean success, String error, String redirectTo) {
    }

    @GetMapping("/leads")
    public String leads(HttpSession session, Model model) {
        String jwt = WebSession.token(session);
        List<VendorLead> leads = backendClient.getLeads(jwt).stream()
                .sorted(Comparator.comparing(VendorLead::lastVisitedAt).reversed())
                .toList();
        model.addAttribute("leads", leads);
        // A lead who's already messaged has a real thread to jump back
        // into; one who's only browsed the storefront doesn't - the
        // template uses this to decide between "Send Message" linking
        // straight to that conversation vs. opening a compose modal.
        Map<Long, Long> conversationIdByEventId = backendClient.getConversations(jwt).stream()
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
        // Mirrors ConversationService#sendVendorMessage's subscription
        // requirement client-side too, so an expired vendor gets a clear
        // message instead of a raw 402/403 from the real call below.
        if (WebSession.isSubscriptionExpired(session)) {
            redirectAttributes.addFlashAttribute(
                    "leadsError", "Your subscription has ended. Renew your plan to message leads.");
            return "redirect:/vendor/leads";
        }
        try {
            backendClient.sendVendorMessage(WebSession.token(session), eventId, message);
            redirectAttributes.addFlashAttribute("leadMessageSent", true);
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("leadsError", e.getMessage());
        }
        return "redirect:/vendor/leads";
    }

    @GetMapping("/bookings")
    public String bookings(HttpSession session, Model model) {
        String jwt = WebSession.token(session);
        List<VendorBooking> bookings = backendClient.getBookings(jwt).stream()
                .sorted(Comparator.comparing(VendorBooking::eventDatetime))
                .toList();
        model.addAttribute("bookings", bookings);
        model.addAttribute("activePage", "bookings");
        model.addAttribute("pageTitle", "Bookings");
        // Actually visiting this page is the "seen it" moment for the
        // sidebar's Bookings badge (see VendorNavBadgeModelAttributes /
        // backend BadgeService).
        backendClient.markVendorBookingsSeen(jwt);
        return "vendor/bookings";
    }

    @PostMapping("/bookings/{bookingId}/acknowledge-payment")
    public String acknowledgeBookingPayment(
            @PathVariable Long bookingId, @RequestParam(required = false) MultipartFile invoice,
            HttpSession session, RedirectAttributes redirectAttributes) {
        // Mirrors BookingService#acknowledgePayment's subscription
        // requirement and invoice-required validation client-side too.
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
        try {
            backendClient.acknowledgeBookingPayment(WebSession.token(session), bookingId, invoice);
            redirectAttributes.addFlashAttribute("paymentAcknowledged", true);
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("bookingsError", e.getMessage());
        }
        return "redirect:/vendor/bookings";
    }

    @PostMapping("/bookings/{bookingId}/reject-payment")
    public String rejectBookingPayment(
            @PathVariable Long bookingId, @RequestParam String reason, HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (reason == null || reason.isBlank()) {
            redirectAttributes.addFlashAttribute("bookingsError", "A rejection reason is required.");
            return "redirect:/vendor/bookings";
        }
        try {
            backendClient.rejectBookingPayment(WebSession.token(session), bookingId, reason);
            redirectAttributes.addFlashAttribute("paymentRejected", true);
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("bookingsError", e.getMessage());
        }
        return "redirect:/vendor/bookings";
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    public String cancelBooking(
            @PathVariable Long bookingId, @RequestParam String reason, HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (reason == null || reason.isBlank()) {
            redirectAttributes.addFlashAttribute("bookingsError", "A cancellation reason is required.");
            return "redirect:/vendor/bookings";
        }
        try {
            backendClient.cancelBooking(WebSession.token(session), bookingId, reason);
            redirectAttributes.addFlashAttribute("bookingCancelled", true);
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("bookingsError", e.getMessage());
        }
        return "redirect:/vendor/bookings";
    }

    // Support tickets - symmetric with PlannerSupportController's /planner/support.
    // Two-pane messenger-style layout (ticket list + selected thread), same
    // shape as /vendor/messages.

    @GetMapping("/support")
    public String support(@RequestParam(required = false) Long ticketId, HttpSession session, Model model) {
        String jwt = WebSession.token(session);
        // GET /api/v1/support-tickets/me is already scoped to the caller -
        // no client-side filtering by user id needed, unlike the old stub.
        List<SupportTicket> tickets = backendClient.getSupportTickets(jwt).stream()
                .sorted(Comparator.comparing(SupportTicket::createdAt).reversed())
                .toList();

        SupportTicket selectedTicket = tickets.stream()
                .filter(t -> t.id() == (ticketId != null ? ticketId : -1))
                .findFirst()
                .orElseGet(() -> tickets.isEmpty() ? null : tickets.get(0));

        List<SupportTicketMessage> thread = selectedTicket == null
                ? List.of()
                : backendClient.getSupportTicketMessages(jwt, selectedTicket.id());

        if (!model.containsAttribute("createTicketForm")) {
            model.addAttribute("createTicketForm", new CreateTicketForm());
        }
        model.addAttribute("tickets", tickets);
        model.addAttribute("selectedTicket", selectedTicket);
        model.addAttribute("thread", thread);
        model.addAttribute("ticketCategories", TicketCategory.values());
        model.addAttribute("currentUserId", WebSession.userId(session));
        model.addAttribute("activePage", "support");
        model.addAttribute("pageTitle", "Support");
        return "vendor/support";
    }

    @PostMapping("/support")
    public String createTicket(
            @ModelAttribute CreateTicketForm createTicketForm,
            @RequestParam(required = false) MultipartFile attachment,
            HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            backendClient.createSupportTicket(
                    WebSession.token(session), createTicketForm.getSubject(),
                    createTicketForm.getCategory() != null ? createTicketForm.getCategory().name() : null,
                    createTicketForm.getMessage(), createTicketForm.getRelatedEventId(), attachment);
            redirectAttributes.addFlashAttribute("ticketCreated", true);
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("supportError", e.getMessage());
        }
        return "redirect:/vendor/support";
    }

    @PostMapping("/support/{ticketId}/reply")
    public String replyToTicket(
            @PathVariable Long ticketId, @RequestParam String message,
            @RequestParam(required = false) MultipartFile attachment,
            HttpSession session, RedirectAttributes redirectAttributes) {
        if (message == null || message.isBlank()) {
            redirectAttributes.addFlashAttribute("supportError", "A message is required.");
        } else {
            try {
                backendClient.replyToSupportTicket(WebSession.token(session), ticketId, message, attachment);
                redirectAttributes.addFlashAttribute("replySent", true);
            } catch (BackendApiException e) {
                redirectAttributes.addFlashAttribute("supportError", e.getMessage());
            }
        }
        redirectAttributes.addAttribute("ticketId", ticketId);
        return "redirect:/vendor/support";
    }

    @GetMapping("/messages")
    public String messages(@RequestParam(required = false) Long conversationId, HttpSession session, Model model) {
        String jwt = WebSession.token(session);
        List<VendorConversation> conversations = backendClient.getConversations(jwt).stream()
                .sorted(Comparator.comparing(VendorConversation::lastMessageAt).reversed())
                .toList();

        VendorConversation selected = conversations.stream()
                .filter(c -> c.id() == (conversationId != null ? conversationId : -1))
                .findFirst()
                .orElseGet(() -> conversations.isEmpty() ? null : conversations.get(0));

        List<VendorConversationMessage> thread = selected == null
                ? List.of()
                : backendClient.getConversationMessages(jwt, selected.id()).stream()
                        .sorted(Comparator.comparing(VendorConversationMessage::createdAt))
                        .toList();

        // Gates the "Create a quote" button in the template - a second
        // independent quotation thread for the same (event, vendor) pair
        // would just be confusing to track (see backend
        // QuotationService#createFromChat, which enforces this same rule
        // server-side too).
        boolean hasQuotationForSelectedEvent = selected != null
                && backendClient.getQuotations(jwt).stream().anyMatch(q -> q.eventId() == selected.eventId());

        model.addAttribute("conversations", conversations);
        model.addAttribute("selectedConversation", selected);
        model.addAttribute("thread", thread);
        model.addAttribute("hasQuotationForSelectedEvent", hasQuotationForSelectedEvent);
        model.addAttribute("vendorUserId", WebSession.userId(session));
        model.addAttribute("activePage", "messages");
        model.addAttribute("pageTitle", "Messages");
        return "vendor/messages";
    }

    /**
     * A vendor starting a brand-new quote directly from a chat thread - for
     * when negotiation happened in conversation with no prior request from
     * the storefront. Refused server-side (see backend
     * QuotationService#createFromChat) if a quotation already exists for
     * this event+vendor - the template only shows this button when
     * hasQuotationForSelectedEvent is false, this is the backstop.
     */
    @PostMapping("/messages/{conversationId}/create-quote")
    public String createQuoteFromChat(
            @PathVariable Long conversationId, @RequestParam(required = false) LocalDate targetDate,
            @RequestParam(required = false) String message, @RequestParam(required = false) List<Long> packageIds,
            @RequestParam(required = false) MultipartFile pdf, @RequestParam BigDecimal quotedAmount,
            HttpSession session, RedirectAttributes redirectAttributes) {
        if (WebSession.isSubscriptionExpired(session)) {
            redirectAttributes.addFlashAttribute(
                    "messagesError", "Your subscription has ended. Renew your plan to send a quote.");
            return "redirect:/vendor/messages?conversationId=" + conversationId;
        }
        if (pdf == null || pdf.isEmpty()) {
            redirectAttributes.addFlashAttribute("messagesError", "A PDF quote is required.");
            return "redirect:/vendor/messages?conversationId=" + conversationId;
        }
        String jwt = WebSession.token(session);
        try {
            VendorConversation conversation = backendClient.getConversations(jwt).stream()
                    .filter(c -> c.id() == conversationId)
                    .findFirst()
                    .orElseThrow(() -> new BackendApiException("Conversation not found", 404));
            backendClient.createQuoteFromChat(jwt, conversation.eventId(), targetDate, message, packageIds, pdf, quotedAmount);
            redirectAttributes.addFlashAttribute("quoteCreatedFromChat", true);
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("messagesError", e.getMessage());
        }
        return "redirect:/vendor/messages?conversationId=" + conversationId;
    }

    @PostMapping("/messages/{conversationId}/reply")
    public String replyToConversation(
            @PathVariable Long conversationId, @RequestParam String body, HttpSession session,
            RedirectAttributes redirectAttributes) {
        // Mirrors ConversationService#sendMessage's vendor-side
        // subscription requirement client-side too.
        if (WebSession.isSubscriptionExpired(session)) {
            redirectAttributes.addFlashAttribute(
                    "messagesError", "Your subscription has ended. Renew your plan to reply to messages.");
            return "redirect:/vendor/messages?conversationId=" + conversationId;
        }
        try {
            backendClient.replyToConversation(WebSession.token(session), conversationId, body);
            redirectAttributes.addFlashAttribute("replySent", true);
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("messagesError", e.getMessage());
        }
        return "redirect:/vendor/messages?conversationId=" + conversationId;
    }

    @GetMapping("/quotations")
    public String quotations(HttpSession session, Model model) {
        String jwt = WebSession.token(session);
        List<VendorQuotation> quotations = backendClient.getQuotations(jwt).stream()
                .sorted(Comparator.comparing(VendorQuotation::createdAt).reversed())
                .toList();
        model.addAttribute("quotations", quotations);
        // Pre-fetched per quotation (same "one extra call per row" pattern
        // already used for leads' conversationIdByEventId) so the "Version
        // History" modal is just server-rendered data already in scope,
        // not a client-side fetch.
        Map<Long, List<BackendQuotationHistoryEntry>> historyByQuotationId = new LinkedHashMap<>();
        for (VendorQuotation quotation : quotations) {
            historyByQuotationId.put(quotation.id(), backendClient.getQuotationHistory(jwt, quotation.id()));
        }
        model.addAttribute("historyByQuotationId", historyByQuotationId);
        model.addAttribute("activePage", "quotations");
        model.addAttribute("pageTitle", "Quotations");
        // Actually visiting this page is the "seen it" moment for the
        // sidebar's Quotations badge (see VendorNavBadgeModelAttributes /
        // backend BadgeService).
        backendClient.markVendorQuotationsSeen(jwt);
        return "vendor/quotations";
    }

    @PostMapping("/quotations/{quotationId}/respond")
    public String respondToQuotation(
            @PathVariable Long quotationId, @RequestParam(required = false) MultipartFile pdf,
            @RequestParam(required = false) String message, @RequestParam(required = false) BigDecimal quotedAmount,
            HttpSession session, RedirectAttributes redirectAttributes) {
        // Same subscription-required check as acknowledgeBookingPayment above
        // (mirrors QuotationService#respondWithPdf's own server-side check) -
        // a friendly message here instead of a confusing failure once the
        // real call rejects it.
        if (WebSession.isSubscriptionExpired(session)) {
            redirectAttributes.addFlashAttribute(
                    "quotationsError", "Your subscription has ended. Renew your plan to respond to quotations.");
            return "redirect:/vendor/quotations";
        }
        if (pdf == null || pdf.isEmpty()) {
            redirectAttributes.addFlashAttribute("quotationsError", "A PDF quote is required to respond.");
            return "redirect:/vendor/quotations";
        }
        if (quotedAmount == null) {
            redirectAttributes.addFlashAttribute("quotationsError", "A quoted amount is required to respond.");
            return "redirect:/vendor/quotations";
        }
        try {
            backendClient.respondToQuotation(WebSession.token(session), quotationId, pdf, message, quotedAmount);
            redirectAttributes.addFlashAttribute("quotationResponded", true);
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("quotationsError", e.getMessage());
        }
        return "redirect:/vendor/quotations";
    }

    @PostMapping("/quotations/{quotationId}/reject-payment")
    public String rejectQuotationPayment(
            @PathVariable Long quotationId, @RequestParam String reason, HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (reason == null || reason.isBlank()) {
            redirectAttributes.addFlashAttribute("quotationsError", "A rejection reason is required.");
            return "redirect:/vendor/quotations";
        }
        try {
            backendClient.rejectQuotationPayment(WebSession.token(session), quotationId, reason);
            redirectAttributes.addFlashAttribute("paymentRejected", true);
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("quotationsError", e.getMessage());
        }
        return "redirect:/vendor/quotations";
    }

    @PostMapping("/quotations/{quotationId}/accept-booking")
    public String acceptBooking(
            @PathVariable Long quotationId, @RequestParam(required = false) String confirmationMessage,
            @RequestParam PaymentType paymentType, @RequestParam(required = false) MultipartFile invoice,
            HttpSession session, RedirectAttributes redirectAttributes) {
        if (WebSession.isSubscriptionExpired(session)) {
            redirectAttributes.addFlashAttribute(
                    "quotationsError", "Your subscription has ended. Renew your plan to confirm bookings.");
            return "redirect:/vendor/quotations";
        }
        if (invoice == null || invoice.isEmpty()) {
            redirectAttributes.addFlashAttribute("quotationsError",
                    "An invoice or receipt document is required to confirm this booking.");
            return "redirect:/vendor/quotations";
        }
        try {
            backendClient.acceptBooking(
                    WebSession.token(session), quotationId, confirmationMessage, paymentType.name(), invoice);
            redirectAttributes.addFlashAttribute("bookingAccepted", true);
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("quotationsError", e.getMessage());
        }
        return "redirect:/vendor/quotations";
    }

    @GetMapping("/calendar")
    public String calendar(HttpSession session, Model model) {
        // An INQUIRY entry (a conversation with no booking yet) has no
        // confirmed date at all - eventDatetime is null - so this needs
        // nullsLast or sorting throws the moment any inquiry is mixed in
        // with dated entries (which is the common case for an active vendor).
        List<VendorCalendarEntry> entries = backendClient.getCalendar(WebSession.token(session)).stream()
                .sorted(Comparator.comparing(VendorCalendarEntry::eventDatetime, Comparator.nullsLast(Comparator.naturalOrder())))
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
                // FullCalendar's own date-grid view has nothing to plot an
                // INQUIRY (no confirmed date yet) against - and calling
                // .toString() on a null eventDatetime below would otherwise
                // throw for every one of them. They still show in the plain
                // list view (the "entries" model attribute), just not here.
                .filter(entry -> entry.eventDatetime() != null)
                .map(entry -> {
                    Map<String, Object> event = new LinkedHashMap<>();
                    event.put("title", entry.eventName());
                    event.put("start", entry.eventDatetime().toString());
                    event.put("display", "block");
                    // Same two-color scheme as the list view's badges (see
                    // vendor/calendar.html's legend) - green once a booking is
                    // actually confirmed, violet for everything short of that
                    // (an inquiry or a quotation at any pre-booked stage).
                    event.put("color", entry.status().equals("BOOKED") ? "#059669" : "#8e70c1");
                    return event;
                })
                .toList();
        return objectMapper.writeValueAsString(events);
    }

    @GetMapping("/packages")
    public String packages(HttpSession session, Model model) {
        List<VendorPackageItem> packages = backendClient.getPackages(WebSession.token(session));
        model.addAttribute("packages", packages);
        model.addAttribute("activePage", "packages");
        model.addAttribute("pageTitle", "Packages");
        if (!model.containsAttribute("vendorPackageForm")) {
            model.addAttribute("vendorPackageForm", new VendorPackageForm());
        }
        model.addAttribute("packageTypes", PackageType.values());
        return "vendor/packages";
    }

    // AJAX (JSON in/out) rather than a redirect - the Add Package modal's
    // photo dropzone lets a vendor drop several photos in right alongside
    // creating the package (see packages.html), and those photos need a
    // real packageId to upload against, which doesn't exist until this
    // call returns. The page's own JS creates the package first via this
    // endpoint, then immediately kicks off the queued photo uploads
    // against the returned packageId, all in one continuous flow.
    @PostMapping("/packages")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addPackage(
            @ModelAttribute VendorPackageForm vendorPackageForm, HttpSession session) {
        try {
            VendorPackageItem added = backendClient.addPackage(WebSession.token(session), vendorPackageForm);
            return ResponseEntity.ok(Map.of("success", true, "packageId", added.id(), "name", added.name()));
        } catch (BackendApiException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/packages/{packageId}")
    public String updatePackage(
            @PathVariable Long packageId, @ModelAttribute VendorPackageForm vendorPackageForm, HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            backendClient.updatePackage(WebSession.token(session), packageId, vendorPackageForm);
            redirectAttributes.addFlashAttribute("packageUpdated", true);
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("packagesError", e.getMessage());
        }
        return "redirect:/vendor/packages";
    }

    // AJAX (JSON in/out via Dropzone), not a redirect - the Edit modal's
    // photo area is a real drag-and-drop dropzone (see packages.html) that
    // lets a vendor drop or select several photos at once, each uploading
    // independently with its own progress bar; a classic redirect-per-file
    // would either force one file at a time or bounce the page mid-drop.
    // The dropzone reloads the page itself (?editPackage={packageId}) once
    // every dropped file has finished, so the grid below always ends up
    // showing the real, saved state.
    @PostMapping("/packages/{packageId}/images")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addPackageImage(
            @PathVariable Long packageId, @RequestParam(required = false) MultipartFile image,
            @RequestParam(required = false) String caption, HttpSession session) {
        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Choose a photo to upload."));
        }
        try {
            backendClient.addPackageImage(WebSession.token(session), packageId, image, caption);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (BackendApiException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // AJAX (JSON), not a redirect - called via fetch() after the vendor
    // confirms in the shared confirmation modal (see packages.html), which
    // then does its own navigate-back-into-this-package's-edit-modal
    // afterward. A redirect response here would just get silently followed
    // and discarded by fetch().
    @PostMapping("/packages/{packageId}/images/{imageId}/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deletePackageImage(
            @PathVariable Long packageId, @PathVariable Long imageId, HttpSession session) {
        try {
            backendClient.deletePackageImage(WebSession.token(session), packageId, imageId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (BackendApiException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // AJAX (JSON), not a redirect - same reasoning as deletePackageImage
    // above: called via fetch() after the vendor confirms in the shared
    // confirmation modal (see packages.html), which then reloads the page
    // itself. Discontinuing (active=false) only hides the package from the
    // storefront (see backend VendorDirectoryService's isActive() filter) -
    // it's never deleted, so reactivating (active=true) brings it right back
    // with all its photos and history intact.
    @PostMapping("/packages/{packageId}/active")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> setPackageActive(
            @PathVariable Long packageId, @RequestParam boolean active, HttpSession session) {
        try {
            backendClient.setPackageActive(WebSession.token(session), packageId, active);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (BackendApiException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Public storefront - what a planner sees when they open "View My Page"
    // (the vendor's own page) or click "View Storefront" from a planner
    // event's Overview tab (a specific slugged vendor - see
    // PlannerEvent/PlannerVendorSuggestion). Opens in its own tab, so it's a
    // standalone page (no vendor sidebar), same pattern as the onboarding page.
    // GET /api/v1/vendors/{slug} is publicly reachable (see SecurityConfig),
    // so /storefront/{slug} is excluded from WebMvcConfig's VENDOR gate -
    // jwt may be null there (a genuinely anonymous visitor).

    @GetMapping("/storefront")
    public String storefront(HttpSession session, Model model) {
        String jwt = WebSession.token(session);
        // There's no authenticated "my own public profile" endpoint - the
        // vendor's slug only comes back on their own settings response.
        String slug = backendClient.getSettings(jwt).slug();
        loadStorefront(model, slug, null, jwt, session);
        return "vendor/storefront";
    }

    @GetMapping("/storefront/{slug}")
    public String storefrontBySlug(
            @PathVariable String slug, @RequestParam(required = false) Long eventId, HttpSession session, Model model) {
        String jwt = WebSession.isLoggedIn(session) ? WebSession.token(session) : null;
        loadStorefront(model, slug, eventId, jwt, session);
        return "vendor/storefront";
    }

    private void loadStorefront(Model model, String slug, Long eventId, String jwt, HttpSession session) {
        // A planner who arrived here from one of their own events already
        // has both of these on hand - pre-filling saves them re-typing
        // their name and re-picking a date they already set on the event.
        // Only applies on a fresh page load (not after a failed submission,
        // where the form is already re-populated with what they typed).
        String plannerName = WebSession.fullName(session);
        LocalDate eventDate = eventId != null && jwt != null ? eventDateOrNull(jwt, eventId) : null;

        if (!model.containsAttribute("quotationRequestForm")) {
            QuotationRequestForm form = new QuotationRequestForm();
            form.setPlannerName(plannerName);
            form.setTargetDate(eventDate);
            model.addAttribute("quotationRequestForm", form);
        }
        if (!model.containsAttribute("inquiryForm")) {
            InquiryForm form = new InquiryForm();
            form.setPlannerName(plannerName);
            form.setTargetDate(eventDate);
            model.addAttribute("inquiryForm", form);
        }
        VendorPublicProfile profile = backendClient.getVendorProfile(jwt, slug, eventId);
        model.addAttribute("profile", profile);
        model.addAttribute("redirectSlug", slug);
        model.addAttribute("eventId", eventId);
        // A vendor previewing their OWN storefront ("View My Page") has no
        // business requesting a quotation or inquiry from themselves - the
        // form fields stay visible (so they can see what a planner would
        // see) but the submit buttons are disabled, see the template.
        Long viewerUserId = WebSession.userId(session);
        model.addAttribute("isOwnStorefront", viewerUserId != null && viewerUserId.equals(profile.vendorUserId()));
    }

    // Best-effort - a missing/inaccessible event just means no date
    // prefill, never a broken storefront page.
    private LocalDate eventDateOrNull(String jwt, Long eventId) {
        try {
            return backendClient.getEvent(jwt, eventId).eventDate();
        } catch (BackendApiException e) {
            return null;
        }
    }

    @PostMapping("/storefront/quotation-request")
    public String submitQuotationRequest(
            @ModelAttribute QuotationRequestForm quotationRequestForm,
            @RequestParam(required = false) String redirectSlug,
            @RequestParam(required = false) Long eventId,
            @RequestParam Long vendorUserId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        String loginError = requirePlannerLogin(session, eventId);
        if (loginError != null) {
            redirectAttributes.addFlashAttribute("storefrontFormError", loginError);
            return redirectAfterStorefrontFailure(redirectSlug, eventId);
        }
        try {
            backendClient.submitQuotationRequest(
                    WebSession.token(session), eventId, vendorUserId, quotationRequestForm.getPlannerName(),
                    quotationRequestForm.getTargetDate(), quotationRequestForm.getMessage(),
                    quotationRequestForm.getPackageIds());
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("storefrontFormError", e.getMessage());
            return redirectAfterStorefrontFailure(redirectSlug, eventId);
        }
        return redirectAfterStorefrontSubmit(redirectSlug, eventId, "quotationRequestSubmitted", redirectAttributes);
    }

    @PostMapping("/storefront/inquiry")
    public String submitInquiry(
            @ModelAttribute InquiryForm inquiryForm,
            @RequestParam(required = false) String redirectSlug,
            @RequestParam(required = false) Long eventId,
            @RequestParam Long vendorUserId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        String loginError = requirePlannerLogin(session, eventId);
        if (loginError != null) {
            redirectAttributes.addFlashAttribute("storefrontFormError", loginError);
            return redirectAfterStorefrontFailure(redirectSlug, eventId);
        }
        try {
            backendClient.submitInquiry(
                    WebSession.token(session), eventId, vendorUserId, inquiryForm.getPlannerName(),
                    inquiryForm.getTargetDate(), inquiryForm.getMessage());
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("storefrontFormError", e.getMessage());
            return redirectAfterStorefrontFailure(redirectSlug, eventId);
        }
        return redirectAfterStorefrontSubmit(redirectSlug, eventId, "inquirySubmitted", redirectAttributes);
    }

    // Both real endpoints behind these two forms are
    // /api/v1/events/{eventId}/vendors/{vendorUserId}/... - eventId is a
    // required path variable on the real backend, and the call needs the
    // visiting PLANNER's own jwt (not the vendor's, even on their own
    // storefront). A storefront is publicly browsable, so a genuinely
    // anonymous visitor - or one who arrived without an eventId (browsing
    // directly rather than from one of their events) - can't submit either
    // form; this returns a human-readable reason why, or null when both
    // checks pass.
    private String requirePlannerLogin(HttpSession session, Long eventId) {
        if (!WebSession.isLoggedIn(session)) {
            return "Please log in as a planner to send this.";
        }
        if (eventId == null) {
            return "Please reach this vendor from one of your events (Overview tab -> Suggested Vendors) so this can be linked to the right event.";
        }
        return null;
    }

    // storefrontFormError is already flashed by the caller before this runs -
    // this just picks where to land (same page either way; there's no
    // "chats tab" to send a failure back to, unlike a successful submit).
    private String redirectAfterStorefrontFailure(String redirectSlug, Long eventId) {
        return redirectSlug != null ? "redirect:/vendor/storefront/" + redirectSlug : "redirect:/vendor/storefront";
    }

    // Stays on the storefront either way (a submission doesn't navigate the
    // planner away to their Chats tab anymore - they land right back where
    // they were, with the existing inquirySubmitted/quotationRequestSubmitted
    // alert on this same page confirming it went through), preserving
    // eventId in the URL so the page keeps its "linked to my event" context
    // (isOwnStorefront check, hidden eventId field on these same forms) for
    // anything submitted next.
    private String redirectAfterStorefrontSubmit(
            String redirectSlug, Long eventId, String plainFlashKey, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute(plainFlashKey, true);
        String base = redirectSlug != null ? "redirect:/vendor/storefront/" + redirectSlug : "redirect:/vendor/storefront";
        return eventId != null ? base + "?eventId=" + eventId : base;
    }

    @GetMapping("/settings")
    public String settings(HttpSession session, Model model) {
        String jwt = WebSession.token(session);
        BackendVendorSettingsResponse settings = backendClient.getSettings(jwt);
        if (!model.containsAttribute("vendorSettingsForm")) {
            model.addAttribute("vendorSettingsForm", toSettingsForm(settings));
        }
        model.addAttribute("settingsDocuments", new VendorSettingsDocuments(
                settings.logoImageUrl(), settings.idCardUrl(), settings.selfieUrl(),
                settings.cancellationPolicyUrl(), settings.refundTermsUrl(),
                settings.verified(), settings.verifiedAt()));
        if (!model.containsAttribute("vendorPaymentMethodForm")) {
            model.addAttribute("vendorPaymentMethodForm", new VendorPaymentMethodForm());
        }
        model.addAttribute("paymentMethods", backendClient.getPaymentMethods(jwt));
        if (!model.containsAttribute("vendorLegalDocumentForm")) {
            model.addAttribute("vendorLegalDocumentForm", new VendorLegalDocumentForm());
        }
        model.addAttribute("legalDocuments", backendClient.getLegalDocuments(jwt));
        model.addAttribute("documentTypes", LegalDocumentType.values());
        model.addAttribute("businessTypes", BusinessType.displayOrder());
        model.addAttribute("provinces", PhilippineProvinces.ALL);
        model.addAttribute("operatingAreaOptions", PhilippineProvinces.OPERATING_AREA_OPTIONS);
        model.addAttribute("eventTypeOptions", EventType.displayOrder());
        model.addAttribute("activePage", "settings");
        model.addAttribute("pageTitle", "Account Settings");
        return "vendor/settings";
    }

    private VendorSettingsForm toSettingsForm(BackendVendorSettingsResponse settings) {
        VendorSettingsForm form = new VendorSettingsForm();
        form.setBusinessName(settings.businessName());
        form.setOwnerName(settings.ownerName());
        form.setBusinessType(settings.businessType());
        form.setContactEmail(settings.contactEmail());
        form.setPhoneNumber(settings.phoneNumber());
        form.setAddressLine1(settings.addressLine1());
        form.setAddressLine2(settings.addressLine2());
        form.setCity(settings.city());
        form.setState(settings.state());
        form.setPostalCode(settings.postalCode());
        form.setCountry(settings.country());
        form.setPrimaryCategory(settings.primaryCategory());
        form.setMaxGuestCapacity(settings.maxGuestCapacity());
        form.setMaxCustomersPerDay(settings.maxCustomersPerDay());
        form.setBasePrice(settings.basePrice());
        form.setLeadTimeDays(settings.leadTimeDays());
        form.setStorefrontOverview(settings.storefrontOverview());
        form.setOperatingAreas(settings.operatingAreas());
        form.setCateredEventTypes(settings.cateredEventTypes());
        form.setPaymentInstructions(settings.paymentInstructions());
        return form;
    }

    // businessPermit is gone from this form - legal documents (any number of
    // them) are added/removed one at a time via the endpoints below, same
    // pattern as payment methods, instead of being folded into this big
    // Save Changes submit.
    @PostMapping("/settings")
    public String updateSettings(
            @ModelAttribute VendorSettingsForm vendorSettingsForm,
            @RequestParam(required = false) MultipartFile logo,
            @RequestParam(required = false) MultipartFile idCard,
            @RequestParam(required = false) MultipartFile selfie,
            @RequestParam(required = false) MultipartFile cancellationPolicyFile,
            @RequestParam(required = false) MultipartFile refundTermsFile,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        // Mirrors eventsrus-backend's own PDF-only validation on this
        // endpoint (VendorSettingsController/UserService#requirePdf) as a
        // fast client-side check before even calling the real endpoint.
        if (!isPdfOrEmpty(cancellationPolicyFile) || !isPdfOrEmpty(refundTermsFile)) {
            redirectAttributes.addFlashAttribute("settingsError", "Only PDF files are accepted for cancellation policy and refund terms.");
            return "redirect:/vendor/settings";
        }
        try {
            backendClient.updateSettings(
                    WebSession.token(session), vendorSettingsForm, logo, idCard, selfie,
                    cancellationPolicyFile, refundTermsFile);
            redirectAttributes.addFlashAttribute("settingsSaved", true);
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("settingsError", e.getMessage());
        }
        return "redirect:/vendor/settings";
    }

    // AJAX (JSON), not a redirect - called via fetch() from the Add Document
    // modal's queue-then-save script (see settings.html), which only fires
    // this once the vendor actually clicks the page's real Save Changes
    // button, not when the modal's own "Add Document" button is clicked.
    @PostMapping("/settings/legal-documents")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addLegalDocument(
            @ModelAttribute VendorLegalDocumentForm vendorLegalDocumentForm,
            @RequestParam(required = false) MultipartFile file,
            HttpSession session) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "A document file is required."));
        }
        try {
            backendClient.addLegalDocument(WebSession.token(session), vendorLegalDocumentForm, file);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (BackendApiException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // AJAX (JSON), not a redirect - called via fetch() from a plain button
    // (see settings.html's .settings-delete-btn script), which then does
    // its own navigate-back-to-settings afterward. A redirect response
    // here would just get silently followed and discarded by fetch(),
    // consuming the one-shot flash attribute before the vendor's own
    // browser ever saw it.
    @PostMapping("/settings/legal-documents/{documentId}/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteLegalDocument(@PathVariable Long documentId, HttpSession session) {
        try {
            backendClient.deleteLegalDocument(WebSession.token(session), documentId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (BackendApiException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private boolean isPdfOrEmpty(MultipartFile file) {
        return file == null || file.isEmpty() || "application/pdf".equals(file.getContentType());
    }

    // AJAX (JSON) - same reasoning as addLegalDocument above.
    @PostMapping("/settings/payment-methods")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addPaymentMethod(
            @ModelAttribute VendorPaymentMethodForm vendorPaymentMethodForm,
            @RequestParam(required = false) MultipartFile qrImage,
            HttpSession session) {
        // Mirrors eventsrus-backend's own image-type validation on this
        // endpoint (VendorPaymentMethodController/Service#requireImage) as a
        // fast client-side check before even calling the real endpoint.
        if (qrImage == null || qrImage.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "A QR code image is required."));
        }
        if (!isPngOrJpeg(qrImage)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only PNG or JPEG images are accepted for payment method QR codes."));
        }
        try {
            backendClient.addPaymentMethod(WebSession.token(session), vendorPaymentMethodForm, qrImage);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (BackendApiException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private boolean isPngOrJpeg(MultipartFile file) {
        String contentType = file.getContentType();
        return "image/png".equals(contentType) || "image/jpeg".equals(contentType);
    }

    @PostMapping("/settings/payment-methods/{paymentMethodId}/delete")
    // AJAX (JSON) for the same reason as deleteLegalDocument above.
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deletePaymentMethod(@PathVariable Long paymentMethodId, HttpSession session) {
        try {
            backendClient.deletePaymentMethod(WebSession.token(session), paymentMethodId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (BackendApiException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
