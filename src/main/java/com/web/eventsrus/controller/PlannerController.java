package com.web.eventsrus.controller;

import com.web.eventsrus.backend.BackendApiException;
import com.web.eventsrus.backend.BackendClient;
import com.web.eventsrus.backend.BackendQuotationHistoryEntry;
import com.web.eventsrus.backend.WebSession;
import com.web.eventsrus.model.BusinessType;
import com.web.eventsrus.model.EventType;
import com.web.eventsrus.model.PhilippineProvinces;
import com.web.eventsrus.model.PlannerEvent;
import com.web.eventsrus.model.PlannerEventSummary;
import com.web.eventsrus.model.PlannerIntakeForm;
import com.web.eventsrus.model.PlannerVendorSuggestion;
import com.web.eventsrus.model.VendorBooking;
import com.web.eventsrus.model.VendorConversation;
import com.web.eventsrus.model.VendorConversationMessage;
import com.web.eventsrus.model.VendorQuotation;
import static com.web.eventsrus.model.BusinessType.ARCADE;
import static com.web.eventsrus.model.BusinessType.BRIDAL_GOWN_DESIGNER;
import static com.web.eventsrus.model.BusinessType.CAKE_AND_PASTRIES;
import static com.web.eventsrus.model.BusinessType.CATERING;
import static com.web.eventsrus.model.BusinessType.DECORATION_PRODUCTION;
import static com.web.eventsrus.model.BusinessType.ENTERTAINMENT;
import static com.web.eventsrus.model.BusinessType.EVENT_COORDINATOR;
import static com.web.eventsrus.model.BusinessType.EVENT_HOST;
import static com.web.eventsrus.model.BusinessType.FLORAL_SERVICES;
import static com.web.eventsrus.model.BusinessType.FOOD_CARTS_GRAZING;
import static com.web.eventsrus.model.BusinessType.HAIR_AND_MAKEUP;
import static com.web.eventsrus.model.BusinessType.INFLATABLES;
import static com.web.eventsrus.model.BusinessType.INTERACTIVE_BAR_MIXOLOGY_SERVICES;
import static com.web.eventsrus.model.BusinessType.INVITATIONS;
import static com.web.eventsrus.model.BusinessType.LED_WALL_VISUAL_PROJECTION_RENTALS;
import static com.web.eventsrus.model.BusinessType.LIGHTS_AND_SOUNDS;
import static com.web.eventsrus.model.BusinessType.LIVE_EVENT_PAINTERS_SKETCH_ARTISTS;
import static com.web.eventsrus.model.BusinessType.MOBILE_PLAYGROUND;
import static com.web.eventsrus.model.BusinessType.PERFORMERS;
import static com.web.eventsrus.model.BusinessType.PHOTO_AND_VIDEO;
import static com.web.eventsrus.model.BusinessType.PHOTO_BOOTHS;
import static com.web.eventsrus.model.BusinessType.POWER_GENERATOR_SERVICES;
import static com.web.eventsrus.model.BusinessType.SECURITY_CROWD_CONTROL;
import static com.web.eventsrus.model.BusinessType.SOUVENIR_GIVEAWAYS;
import static com.web.eventsrus.model.BusinessType.SPECIAL_EFFECTS;
import static com.web.eventsrus.model.BusinessType.STAGING_TRUSSING_FLOORING_RENTALS;
import static com.web.eventsrus.model.BusinessType.SUIT_RENTALS;
import static com.web.eventsrus.model.BusinessType.TRANSPORT_SHUTTLE_FLEET_SERVICES;
import static com.web.eventsrus.model.BusinessType.VENUE;
import static com.web.eventsrus.model.BusinessType.WARDROBE_STYLISTS_DRESSERS;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

/**
 * The "logged in" planner experience - a ChatGPT-style event history
 * sidebar plus a per-event Overview/Chats/Quotations/Bookings workspace.
 * Real eventsrus-backend data now: WebMvcConfig already gates every
 * /planner/** route to a logged-in user, so WebSession.token(session) is
 * always present here. An event's AI-generated idea/matched suppliers come
 * from the real one-shot AiSuggestionService/VendorSearchService pipeline
 * at creation time (EventService#createEvent). Follow-up questions go
 * through a separate, deliberately narrow "Events Coordinator" (real Claude
 * call via CoordinatorController) - ideas/advice only, scoped to one event's
 * basic facts, never vendor/quotation/booking data; it explicitly redirects
 * anything vendor-specific back to the real planner-vendor conversation
 * rather than answering on the vendor's behalf.
 */
@Controller
@RequestMapping("/planner/events")
public class PlannerController {

    // Manila, not UTC/system default - every date this app collects (event
    // dates, target dates, ...) is a plain calendar date with no time zone
    // of its own; booking's real eventDatetime (Instant) needs SOME zone to
    // anchor "midnight" to, and this is a Philippines-only marketplace.
    private static final ZoneId MANILA = ZoneId.of("Asia/Manila");

    private final BackendClient backendClient;

    public PlannerController(BackendClient backendClient) {
        this.backendClient = backendClient;
    }

    /**
     * Marks a bell-dropdown notification read. Unlike the vendor side,
     * planner pages are all scoped to one event at a time and a
     * notification doesn't carry which event it's about, so there's no
     * single right place to deep-link to - back to the event list, where
     * they can pick.
     */
    @PostMapping("/notifications/{id}/read")
    public String markNotificationRead(@PathVariable Long id, HttpSession session) {
        try {
            backendClient.markNotificationRead(WebSession.token(session), id);
        } catch (BackendApiException e) {
            // Already read, not this planner's, or the backend hiccuped -
            // still send them back rather than showing an error.
        }
        return "redirect:/planner/events";
    }

    @GetMapping
    public String events(
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long conversationId,
            @RequestParam(required = false, defaultValue = "overview") String tab,
            HttpSession session,
            Model model) {
        String jwt = WebSession.token(session);
        List<PlannerEventSummary> events = backendClient.listEvents(jwt);
        model.addAttribute("events", events);
        model.addAttribute("activeEventId", eventId);
        model.addAttribute("activeTab", tab);
        model.addAttribute("plannerUserId", WebSession.userId(session));

        boolean hasSelection = eventId != null && events.stream().anyMatch(e -> e.id() == eventId);
        PlannerEvent selected = hasSelection ? backendClient.getEvent(jwt, eventId) : null;
        model.addAttribute("selectedEvent", selected);
        // Needed on both branches now: the intake form's location select
        // when no event is selected yet, and the always-on supplier
        // location/date filter (Overview tab) once one is.
        model.addAttribute("provinces", PhilippineProvinces.ALL);

        if (selected == null) {
            if (!model.containsAttribute("plannerIntakeForm")) {
                model.addAttribute("plannerIntakeForm", new PlannerIntakeForm());
            }
            model.addAttribute("eventTypes", EventType.displayOrder());
            return "planner/events";
        }

        List<VendorQuotation> quotations = backendClient.getPlannerQuotations(jwt).stream()
                .filter(q -> q.eventId() == selected.id())
                .toList();
        List<VendorBooking> bookings = backendClient.getPlannerBookings(jwt).stream()
                .filter(b -> b.eventId() == selected.id())
                .toList();
        List<VendorConversation> conversations = backendClient.getConversations(jwt).stream()
                .filter(c -> c.eventId() == selected.id())
                .sorted(Comparator.comparing(VendorConversation::lastMessageAt).reversed())
                .toList();

        VendorConversation selectedConversation = conversations.stream()
                .filter(c -> c.id() == (conversationId != null ? conversationId : -1))
                .findFirst()
                .orElseGet(() -> conversations.isEmpty() ? null : conversations.get(0));

        List<VendorConversationMessage> thread = selectedConversation == null
                ? List.of()
                : backendClient.getConversationMessages(jwt, selectedConversation.id());

        // Same "one extra call per row" pre-fetch as VendorController#quotations
        // - the full timeline is server-rendered per quotation, no separate
        // client-side fetch for the "Version History" modal.
        Map<Long, List<BackendQuotationHistoryEntry>> historyByQuotationId = new LinkedHashMap<>();
        for (VendorQuotation quotation : quotations) {
            historyByQuotationId.put(quotation.id(), backendClient.getQuotationHistory(jwt, quotation.id()));
        }
        model.addAttribute("quotations", quotations);
        model.addAttribute("historyByQuotationId", historyByQuotationId);
        model.addAttribute("bookings", bookings);
        model.addAttribute("conversations", conversations);
        model.addAttribute("selectedConversation", selectedConversation);
        model.addAttribute("thread", thread);
        model.addAttribute("coordinatorHistory", backendClient.getCoordinatorHistory(jwt, selected.id()));
        model.addAttribute("businessTypes", BusinessType.displayOrder());
        addSupplierPanelAttributes(model, selected);
        return "planner/events";
    }

    // Which vendor categories matter for a given kind of event, most
    // important first. Drives the order categories appear in and which ones
    // sit up top vs. behind "show more service categories". A category not
    // in the list still shows (as a match) - just down in the long tail.
    private static final List<BusinessType> WEDDING_TYPES = List.of(
            VENUE, CATERING, PHOTO_AND_VIDEO, EVENT_COORDINATOR, HAIR_AND_MAKEUP, FLORAL_SERVICES,
            CAKE_AND_PASTRIES, BRIDAL_GOWN_DESIGNER, SUIT_RENTALS, DECORATION_PRODUCTION, LIGHTS_AND_SOUNDS,
            ENTERTAINMENT, EVENT_HOST, INVITATIONS, PHOTO_BOOTHS, SOUVENIR_GIVEAWAYS,
            WARDROBE_STYLISTS_DRESSERS, TRANSPORT_SHUTTLE_FLEET_SERVICES, SPECIAL_EFFECTS);

    private static final List<BusinessType> PARTY_TYPES = List.of(
            VENUE, CATERING, PHOTO_AND_VIDEO, EVENT_HOST, CAKE_AND_PASTRIES, DECORATION_PRODUCTION,
            ENTERTAINMENT, PHOTO_BOOTHS, FOOD_CARTS_GRAZING, SOUVENIR_GIVEAWAYS, INFLATABLES,
            MOBILE_PLAYGROUND, ARCADE, PERFORMERS, LIGHTS_AND_SOUNDS, INTERACTIVE_BAR_MIXOLOGY_SERVICES,
            INVITATIONS, SPECIAL_EFFECTS);

    private static final List<BusinessType> MILESTONE_TYPES = List.of(
            VENUE, CATERING, PHOTO_AND_VIDEO, EVENT_HOST, EVENT_COORDINATOR, CAKE_AND_PASTRIES,
            DECORATION_PRODUCTION, FLORAL_SERVICES, ENTERTAINMENT, PHOTO_BOOTHS, HAIR_AND_MAKEUP,
            LIGHTS_AND_SOUNDS, FOOD_CARTS_GRAZING, INTERACTIVE_BAR_MIXOLOGY_SERVICES, SOUVENIR_GIVEAWAYS,
            INVITATIONS);

    private static final List<BusinessType> CORPORATE_TYPES = List.of(
            VENUE, CATERING, LIGHTS_AND_SOUNDS, LED_WALL_VISUAL_PROJECTION_RENTALS,
            STAGING_TRUSSING_FLOORING_RENTALS, EVENT_HOST, EVENT_COORDINATOR, PHOTO_AND_VIDEO,
            ENTERTAINMENT, PERFORMERS, DECORATION_PRODUCTION, POWER_GENERATOR_SERVICES,
            SECURITY_CROWD_CONTROL, TRANSPORT_SHUTTLE_FLEET_SERVICES, INTERACTIVE_BAR_MIXOLOGY_SERVICES,
            LIVE_EVENT_PAINTERS_SKETCH_ARTISTS, SPECIAL_EFFECTS, INVITATIONS, SOUVENIR_GIVEAWAYS);

    private static final List<BusinessType> PRODUCTION_TYPES = List.of(
            VENUE, LIGHTS_AND_SOUNDS, STAGING_TRUSSING_FLOORING_RENTALS, LED_WALL_VISUAL_PROJECTION_RENTALS,
            POWER_GENERATOR_SERVICES, SECURITY_CROWD_CONTROL, PERFORMERS, ENTERTAINMENT, EVENT_HOST,
            FOOD_CARTS_GRAZING, CATERING, TRANSPORT_SHUTTLE_FLEET_SERVICES, SPECIAL_EFFECTS,
            PHOTO_AND_VIDEO, INTERACTIVE_BAR_MIXOLOGY_SERVICES);

    private static List<BusinessType> relevantTypesFor(EventType type) {
        if (type == null) {
            return MILESTONE_TYPES;
        }
        return switch (type) {
            case WEDDING -> WEDDING_TYPES;
            case BIRTHDAY, DEBUT, PARTY, BABY_SHOWER -> PARTY_TYPES;
            case SEMINAR, NETWORKING_EVENT, CORPORATE_EVENT, PRODUCT_LAUNCH, TEAM_BUILDING, CORPORATE_RETREAT,
                    TRADE_SHOW, GALA, FUNDRAISER, EXHIBITION -> CORPORATE_TYPES;
            case CONCERT, FESTIVAL, SPORTS_EVENT -> PRODUCTION_TYPES;
            default -> MILESTONE_TYPES;
        };
    }

    /**
     * Turns the backend's flat list of suggestions into the Overview
     * panel's model: a compact, relevance-ordered set of category groups
     * (each already ranked Top Vendor -> Verified -> rating by the backend),
     * the categories relevant to this event type up front and the rest
     * flagged for the "show more service categories" fold, plus a single
     * list of category labels that matched nothing so the template can
     * render one line instead of a dozen empty cards.
     */
    private void addSupplierPanelAttributes(Model model, PlannerEvent event) {
        Map<BusinessType, List<PlannerVendorSuggestion>> byType = new LinkedHashMap<>();
        for (PlannerVendorSuggestion s : event.suggestions()) {
            byType.computeIfAbsent(s.vendorType(), k -> new ArrayList<>());
            if (s.vendorProfileId() != null) {
                byType.get(s.vendorType()).add(s);
            }
        }

        List<BusinessType> relevantOrder = relevantTypesFor(event.eventType());
        Set<BusinessType> emitted = new LinkedHashSet<>();
        List<SupplierGroup> groups = new ArrayList<>();

        for (BusinessType t : relevantOrder) {
            List<PlannerVendorSuggestion> vendors = byType.get(t);
            if (vendors != null && !vendors.isEmpty()) {
                groups.add(toSupplierGroup(t, vendors, true));
                emitted.add(t);
            }
        }
        // Categories that matched but aren't relevant to this event type sit
        // behind "show more service categories" - alphabetical there (no
        // relevance ranking applies), not raw enum/insertion order.
        List<Map.Entry<BusinessType, List<PlannerVendorSuggestion>>> remaining = byType.entrySet().stream()
                .filter(e -> !emitted.contains(e.getKey()) && !e.getValue().isEmpty())
                .sorted(Comparator.comparing(e -> e.getKey().getLabel()))
                .toList();
        for (Map.Entry<BusinessType, List<PlannerVendorSuggestion>> e : remaining) {
            groups.add(toSupplierGroup(e.getKey(), e.getValue(), false));
            emitted.add(e.getKey());
        }

        List<String> emptyTypeLabels = new ArrayList<>();
        for (BusinessType t : BusinessType.displayOrder()) {
            List<PlannerVendorSuggestion> vendors = byType.get(t);
            if (vendors == null || vendors.isEmpty()) {
                emptyTypeLabels.add(t.getLabel());
            }
        }

        int matchTotal = groups.stream().mapToInt(SupplierGroup::matchCount).sum();
        int verifiedTotal = groups.stream().mapToInt(SupplierGroup::verifiedCount).sum();
        int moreCatCount = (int) groups.stream().filter(g -> !g.relevant()).count();

        model.addAttribute("supplierGroups", groups);
        model.addAttribute("supplierEmptyTypes", emptyTypeLabels);
        model.addAttribute("supplierMatchTotal", matchTotal);
        model.addAttribute("supplierVerifiedTotal", verifiedTotal);
        model.addAttribute("supplierMoreCatCount", moreCatCount);
    }

    private SupplierGroup toSupplierGroup(BusinessType type, List<PlannerVendorSuggestion> ranked, boolean relevant) {
        int verified = (int) ranked.stream().filter(PlannerVendorSuggestion::verified).count();
        return new SupplierGroup(type, type.getLabel(), relevant, ranked.size(), verified, ranked);
    }

    /** One business-type card in the Overview's Recommended Suppliers panel. */
    public record SupplierGroup(
            BusinessType type,
            String label,
            boolean relevant,
            int matchCount,
            int verifiedCount,
            List<PlannerVendorSuggestion> vendors) {
    }

    @PostMapping
    public String submitIntake(
            @ModelAttribute PlannerIntakeForm plannerIntakeForm, HttpSession session, RedirectAttributes redirectAttributes) {
        String jwt = WebSession.token(session);
        try {
            PlannerEvent created = backendClient.createEvent(
                    jwt, plannerIntakeForm.getEventType(), plannerIntakeForm.getEventDate(),
                    plannerIntakeForm.getLocation(), plannerIntakeForm.getDescription());
            // The real CreateEventRequest has no name field (that's a
            // separate "save" step) - naming it immediately here keeps the
            // vendor-facing UX exactly what it always was: a new event
            // shows up in the sidebar, named, right away.
            backendClient.saveEvent(jwt, created.id(), plannerIntakeForm.getName());
            redirectAttributes.addAttribute("eventId", created.id());
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("eventCreationError", e.getMessage());
            redirectAttributes.addFlashAttribute("plannerIntakeForm", plannerIntakeForm);
        }
        return "redirect:/planner/events";
    }

    /**
     * Live vendor-matching endpoint behind the always-on location/date
     * filter on the Overview tab (see events.html's script block and
     * #suppliersResults fragment) - called via fetch() on field change, not
     * a form submit. Persists the new location/date onto the event, then
     * returns just the results fragment (not a full page/redirect) with
     * vendor matches computed fresh against whatever's now on the event -
     * see EventService#buildSuggestions on the backend.
     */
    @PostMapping("/{eventId}/suppliers")
    public String refreshSuppliers(
            @PathVariable Long eventId,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) LocalDate eventDate,
            HttpSession session,
            Model model) {
        try {
            PlannerEvent updated = backendClient.updateEventDetails(WebSession.token(session), eventId, eventDate, location);
            model.addAttribute("selectedEvent", updated);
            addSupplierPanelAttributes(model, updated);
        } catch (BackendApiException e) {
            model.addAttribute("suppliersError", "Couldn't update vendor matches. Please try again.");
        }
        return "planner/events :: suppliersResults";
    }

    @PostMapping("/{eventId}/coordinator/ask")
    public String askCoordinator(
            @PathVariable Long eventId,
            @RequestParam String question,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (question == null || question.isBlank()) {
            redirectAttributes.addFlashAttribute("coordinatorError", "Type a question first.");
        } else {
            try {
                backendClient.askCoordinator(WebSession.token(session), eventId, question);
            } catch (BackendApiException e) {
                // 429 = the real "you've hit today's limit" message, which is
                // planner-facing and fine to show as-is. Anything else (the
                // AI service unreachable/misconfigured) gets a generic
                // message instead of surfacing internal config details.
                redirectAttributes.addFlashAttribute("coordinatorError",
                        e.getStatus() == 429 ? e.getMessage()
                                : "The events coordinator is temporarily unavailable. Please try again later.");
            }
        }
        redirectAttributes.addAttribute("eventId", eventId);
        redirectAttributes.addAttribute("tab", "overview");
        return "redirect:/planner/events";
    }

    @PostMapping("/{eventId}/quotations/{quotationId}/book")
    public String bookFromQuotation(
            @PathVariable Long eventId,
            @PathVariable Long quotationId,
            @RequestParam BigDecimal price,
            @RequestParam LocalDate eventDatetime,
            @RequestParam(required = false) String agreementDetails,
            @RequestParam(required = false) MultipartFile screenshot,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (screenshot != null && !screenshot.isEmpty() && !isPngOrJpeg(screenshot)) {
            redirectAttributes.addFlashAttribute("bookingsError", "Only PNG or JPEG images are accepted for payment screenshots.");
            redirectAttributes.addAttribute("eventId", eventId);
            redirectAttributes.addAttribute("tab", "quotations");
            return "redirect:/planner/events";
        }
        String jwt = WebSession.token(session);
        try {
            VendorBooking booking = backendClient.bookFromQuotation(
                    jwt, quotationId, price, eventDatetime.atStartOfDay(MANILA).toInstant(), agreementDetails);
            boolean includedScreenshot = screenshot != null && !screenshot.isEmpty();
            if (includedScreenshot) {
                backendClient.submitPaymentScreenshot(jwt, booking.id(), screenshot);
            }
            redirectAttributes.addFlashAttribute("bookingSubmitted", true);
            redirectAttributes.addFlashAttribute("bookingIncludedScreenshot", includedScreenshot);
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("bookingsError", e.getMessage());
        }
        redirectAttributes.addAttribute("eventId", eventId);
        redirectAttributes.addAttribute("tab", "quotations");
        return "redirect:/planner/events";
    }

    @PostMapping("/{eventId}/quotations/{quotationId}/decline")
    public String declineQuotation(
            @PathVariable Long eventId, @PathVariable Long quotationId, HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            backendClient.declineQuotation(WebSession.token(session), quotationId);
            redirectAttributes.addFlashAttribute("quotationDeclined", true);
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("bookingsError", e.getMessage());
        }
        redirectAttributes.addAttribute("eventId", eventId);
        redirectAttributes.addAttribute("tab", "quotations");
        return "redirect:/planner/events";
    }

    @PostMapping("/{eventId}/quotations/{quotationId}/revise")
    public String reviseQuotation(
            @PathVariable Long eventId,
            @PathVariable Long quotationId,
            @RequestParam String message,
            @RequestParam(required = false) LocalDate targetDate,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (message == null || message.isBlank()) {
            redirectAttributes.addFlashAttribute("bookingsError", "A message describing what you'd like changed is required.");
            redirectAttributes.addAttribute("eventId", eventId);
            redirectAttributes.addAttribute("tab", "quotations");
            return "redirect:/planner/events";
        }
        try {
            backendClient.reviseQuotation(WebSession.token(session), quotationId, targetDate, message, null);
            redirectAttributes.addFlashAttribute("quotationRevised", true);
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("bookingsError", e.getMessage());
        }
        redirectAttributes.addAttribute("eventId", eventId);
        redirectAttributes.addAttribute("tab", "quotations");
        return "redirect:/planner/events";
    }

    @PostMapping("/{eventId}/bookings/{bookingId}/cancel")
    public String cancelBooking(
            @PathVariable Long eventId,
            @PathVariable Long bookingId,
            @RequestParam String reason,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (reason == null || reason.isBlank()) {
            redirectAttributes.addFlashAttribute("bookingsError", "A cancellation reason is required.");
        } else {
            try {
                backendClient.cancelBooking(WebSession.token(session), bookingId, reason);
                redirectAttributes.addFlashAttribute("bookingCancelled", true);
            } catch (BackendApiException e) {
                redirectAttributes.addFlashAttribute("bookingsError", e.getMessage());
            }
        }
        redirectAttributes.addAttribute("eventId", eventId);
        redirectAttributes.addAttribute("tab", "bookings");
        return "redirect:/planner/events";
    }

    @PostMapping("/{eventId}/bookings/{bookingId}/payment-screenshot")
    public String submitPaymentScreenshot(
            @PathVariable Long eventId,
            @PathVariable Long bookingId,
            @RequestParam(required = false) MultipartFile screenshot,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (screenshot == null || screenshot.isEmpty()) {
            redirectAttributes.addFlashAttribute("bookingsError", "A payment screenshot is required.");
        } else if (!isPngOrJpeg(screenshot)) {
            redirectAttributes.addFlashAttribute("bookingsError", "Only PNG or JPEG images are accepted for payment screenshots.");
        } else {
            try {
                backendClient.submitPaymentScreenshot(WebSession.token(session), bookingId, screenshot);
                redirectAttributes.addFlashAttribute("screenshotSubmitted", true);
            } catch (BackendApiException e) {
                redirectAttributes.addFlashAttribute("bookingsError", e.getMessage());
            }
        }
        redirectAttributes.addAttribute("eventId", eventId);
        redirectAttributes.addAttribute("tab", "bookings");
        return "redirect:/planner/events";
    }

    @PostMapping("/{eventId}/bookings/{bookingId}/review")
    public String submitReview(
            @PathVariable Long eventId,
            @PathVariable Long bookingId,
            @RequestParam int rating,
            @RequestParam String comment,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (rating < 1 || rating > 5 || comment == null || comment.isBlank()) {
            redirectAttributes.addFlashAttribute("bookingsError", "A star rating and a written review are both required.");
        } else {
            try {
                backendClient.submitReview(WebSession.token(session), bookingId, rating, comment);
                redirectAttributes.addFlashAttribute("reviewSubmitted", true);
            } catch (BackendApiException e) {
                redirectAttributes.addFlashAttribute("bookingsError", e.getMessage());
            }
        }
        redirectAttributes.addAttribute("eventId", eventId);
        redirectAttributes.addAttribute("tab", "bookings");
        return "redirect:/planner/events";
    }

    @PostMapping("/{eventId}/reviews/{reviewId}")
    public String updateReview(
            @PathVariable Long eventId,
            @PathVariable Long reviewId,
            @RequestParam int rating,
            @RequestParam String comment,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (rating < 1 || rating > 5 || comment == null || comment.isBlank()) {
            redirectAttributes.addFlashAttribute("bookingsError", "A star rating and a written review are both required.");
        } else {
            try {
                backendClient.updateReview(WebSession.token(session), reviewId, rating, comment);
                redirectAttributes.addFlashAttribute("reviewSubmitted", true);
            } catch (BackendApiException e) {
                redirectAttributes.addFlashAttribute("bookingsError", e.getMessage());
            }
        }
        redirectAttributes.addAttribute("eventId", eventId);
        redirectAttributes.addAttribute("tab", "bookings");
        return "redirect:/planner/events";
    }

    @PostMapping("/{eventId}/conversations/{conversationId}/reply")
    public String replyToConversation(
            @PathVariable Long eventId,
            @PathVariable Long conversationId,
            @RequestParam String body,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (body == null || body.isBlank()) {
            redirectAttributes.addFlashAttribute("bookingsError", "A message is required.");
        } else {
            try {
                backendClient.replyToConversation(WebSession.token(session), conversationId, body);
                redirectAttributes.addFlashAttribute("replySent", true);
            } catch (BackendApiException e) {
                redirectAttributes.addFlashAttribute("bookingsError", e.getMessage());
            }
        }
        redirectAttributes.addAttribute("eventId", eventId);
        redirectAttributes.addAttribute("tab", "chats");
        redirectAttributes.addAttribute("conversationId", conversationId);
        return "redirect:/planner/events";
    }

    private boolean isPngOrJpeg(MultipartFile file) {
        String contentType = file.getContentType();
        return "image/png".equals(contentType) || "image/jpeg".equals(contentType);
    }
}
