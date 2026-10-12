package com.web.eventsrus.controller;

import com.web.eventsrus.backend.BackendApiException;
import com.web.eventsrus.backend.BackendClient;
import com.web.eventsrus.backend.BackendQuotationHistoryEntry;
import com.web.eventsrus.backend.WebSession;
import com.web.eventsrus.model.BusinessType;
import com.web.eventsrus.model.CategoryRecommendation;
import com.web.eventsrus.model.EventType;
import com.web.eventsrus.model.PhilippineProvinces;
import com.web.eventsrus.model.PlannerChecklistItem;
import com.web.eventsrus.model.PlannerEvent;
import com.web.eventsrus.model.PlannerEventSummary;
import com.web.eventsrus.model.PlannerIntakeForm;
import com.web.eventsrus.model.PlannerVendorSuggestion;
import com.web.eventsrus.model.VendorBooking;
import com.web.eventsrus.model.VendorConversation;
import com.web.eventsrus.model.VendorConversationMessage;
import com.web.eventsrus.model.VendorQuotation;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
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
@Slf4j
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
            log.warn("markNotificationRead failed for notification {}: {} {}", id, e.getStatus(), e.getMessage());
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
        // Unseen-since-last-visit badges for this event's own Quotations/
        // Bookings tabs (see backend BadgeService) - read-only here; the
        // tabs themselves mark seen via JS the moment the planner actually
        // clicks one (see the shown.bs.tab wiring at the bottom of this
        // page), since both tabs' content is already sitting in this same
        // server-rendered page rather than a separate navigation.
        model.addAttribute("quotationsUnseenCount", backendClient.getUnseenQuotationsCountForEvent(jwt, selected.id()));
        model.addAttribute("bookingsUnseenCount", backendClient.getUnseenBookingsCountForEvent(jwt, selected.id()));
        model.addAttribute("conversations", conversations);
        model.addAttribute("selectedConversation", selectedConversation);
        model.addAttribute("thread", thread);
        model.addAttribute("coordinatorHistory", backendClient.getCoordinatorHistory(jwt, selected.id()));
        addSupplierPanelAttributes(model, selected);
        return "planner/events";
    }

    /**
     * Turns the backend's flat list of suggestions into "Search Result"'s
     * model: one SupplierGroup per category the planner selected during
     * the create-event wizard (see the "no event selected" branch's
     * essential/optional split there - RecommendationController's matrix,
     * read before any event/SupplierGroup exists). Also builds
     * supplierGroupByType for the Checklist tab's live match-count badges.
     */
    private void addSupplierPanelAttributes(Model model, PlannerEvent event) {
        Map<BusinessType, List<PlannerVendorSuggestion>> byType = new LinkedHashMap<>();
        for (PlannerVendorSuggestion s : event.suggestions()) {
            byType.computeIfAbsent(s.vendorType(), k -> new ArrayList<>()).add(s);
        }

        List<SupplierGroup> essentialGroups = new ArrayList<>();
        List<SupplierGroup> otherGroups = new ArrayList<>();
        for (BusinessType t : BusinessType.displayOrder()) {
            List<PlannerVendorSuggestion> rows = byType.get(t);
            if (rows == null || rows.isEmpty()) {
                continue;
            }
            SupplierGroup group = toSupplierGroup(t, rows);
            (group.essential() ? essentialGroups : otherGroups).add(group);
        }

        List<SupplierGroup> allGroups = new ArrayList<>(essentialGroups);
        allGroups.addAll(otherGroups);
        List<SupplierGroup> selectedGroups = allGroups.stream().filter(SupplierGroup::selected).toList();

        // "Search Result" (the Overview tab) only ever shows categories the
        // planner actually selected in the create-event wizard's step 2 -
        // essential/otherGroups themselves are no longer rendered anywhere
        // (that selection now happens pre-creation, via RecommendationController
        // directly, before any event/SupplierGroup exists).
        model.addAttribute("selectedSupplierGroups", selectedGroups);
        // The complement - for the "Add category" pop-up, so a planner who
        // skipped a category during creation (or just changed their mind)
        // can bring it into Search Result later without starting over.
        model.addAttribute("unselectedSupplierGroups", allGroups.stream().filter(g -> !g.selected()).toList());

        addChecklistPanelAttributes(model, event);
    }

    /**
     * Groups the flat checklist into one bucket per category (both a
     * category's own "Book a X vendor" item and any "Follow up with Y"
     * bookmark items for that category land in the same bucket - see
     * EventService#setVendorBookmark's category-context resolution), plus
     * a flat bucket for planner-typed custom items (no category at all).
     * Category order follows BusinessType#displayOrder, not insertion
     * order, so it reads the same way every time regardless of which
     * category the planner happened to check first.
     */
    private void addChecklistPanelAttributes(Model model, PlannerEvent event) {
        Map<BusinessType, List<PlannerChecklistItem>> byCategory = new LinkedHashMap<>();
        List<PlannerChecklistItem> uncategorized = new ArrayList<>();
        for (PlannerChecklistItem item : event.checklist()) {
            if (item.vendorType() != null) {
                byCategory.computeIfAbsent(item.vendorType(), k -> new ArrayList<>()).add(item);
            } else {
                uncategorized.add(item);
            }
        }
        Map<BusinessType, List<PlannerChecklistItem>> orderedByCategory = new LinkedHashMap<>();
        for (BusinessType t : BusinessType.displayOrder()) {
            if (byCategory.containsKey(t)) {
                orderedByCategory.put(t, byCategory.get(t));
            }
        }
        model.addAttribute("checklistByCategory", orderedByCategory);
        model.addAttribute("uncategorizedChecklistItems", uncategorized);
    }

    private SupplierGroup toSupplierGroup(BusinessType type, List<PlannerVendorSuggestion> rows) {
        List<PlannerVendorSuggestion> matches = rows.stream().filter(r -> r.vendorProfileId() != null).toList();
        // Every row in this category carries the same categorySelected/categoryBudget/essential
        // (see SuggestedVendorResponse javadoc on the backend) - read off the first.
        PlannerVendorSuggestion first = rows.get(0);
        return new SupplierGroup(
                type, type.getLabel(), first.essential(), matches.size(), first.categorySelected(),
                first.categoryBudget(), matches);
    }

    /** One business-type card in the Overview's Suggested Suppliers panel / Browse Suppliers modal. */
    public record SupplierGroup(
            BusinessType type,
            String label,
            boolean essential,
            int matchCount,
            boolean selected,
            java.math.BigDecimal budget,
            List<PlannerVendorSuggestion> vendors) {
    }

    /**
     * STEP 1 -> STEP 2 of the new-event wizard (see events.html's script
     * block): essential/optional categories for an event type, with no
     * event created yet - just a thin proxy onto the backend's standalone
     * RecommendationController, so step 2 can render its checkbox list
     * before anything is saved.
     */
    @GetMapping("/recommendations")
    @ResponseBody
    public CategoryOptionsResponse recommendations(@RequestParam EventType eventType, HttpSession session) {
        CategoryRecommendation rec = backendClient.getRecommendations(WebSession.token(session), eventType);
        return new CategoryOptionsResponse(toCategoryOptions(rec.essential()), toCategoryOptions(rec.optional()));
    }

    private List<CategoryOption> toCategoryOptions(Set<BusinessType> types) {
        return BusinessType.displayOrder().stream()
                .filter(types::contains)
                .map(t -> new CategoryOption(t.name(), t.getLabel()))
                .toList();
    }

    public record CategoryOption(String type, String label) {
    }

    public record CategoryOptionsResponse(List<CategoryOption> essential, List<CategoryOption> optional) {
    }

    /**
     * STEP 2's "Next" (see events.html's script block) - this is the actual
     * save point for the whole wizard. Creates the event from step 1's
     * fields; the categories checked in step 2 are saved right after, one
     * #setSupplierTypeSelection call per category, all behind the same
     * "Processing data" spinner (see PlannerController's REST call chain
     * in JS - no new backend endpoint needed for that part).
     */
    @PostMapping("/create")
    @ResponseBody
    public QuickEventResult createEventAjax(@ModelAttribute PlannerIntakeForm plannerIntakeForm, HttpSession session) {
        String jwt = WebSession.token(session);
        try {
            PlannerEvent created = backendClient.createEvent(
                    jwt, plannerIntakeForm.getName(), plannerIntakeForm.getEventType(), plannerIntakeForm.getEventDate(),
                    plannerIntakeForm.getLocation(), plannerIntakeForm.getDescription(),
                    plannerIntakeForm.getBudget(), plannerIntakeForm.getGuestCount());
            return new QuickEventResult(true, created.id(), null);
        } catch (BackendApiException e) {
            return new QuickEventResult(false, null, e.getMessage());
        }
    }

    // JSON variant of the same create-then-save pair above, for the
    // storefront's guided signup flow (see VendorController#loadStorefront
    // and storefront.html's Modal C) - a brand-new or event-less planner
    // who just wants to request a quote from one vendor doesn't need the
    // full intake form's description field (optional on the real backend
    // request, same as submitIntake leaves it); they can fill that in
    // normally later. name/eventType/eventDate/location are all mandatory
    // on the real backend request same as everywhere else, so Modal C
    // collects all four. Driven by fetch() from a modal rather than a page
    // form post, so this returns JSON instead of redirecting -
    // WebMvcConfig already gates every /planner/** route to a logged-in
    // session, so this is only ever reachable once the storefront's
    // embedded Google sign-in has actually succeeded.
    @PostMapping("/quick-create")
    @ResponseBody
    public QuickEventResult quickCreate(
            @RequestParam String name, @RequestParam EventType eventType,
            @RequestParam LocalDate eventDate, @RequestParam String location, HttpSession session) {
        String jwt = WebSession.token(session);
        try {
            PlannerEvent created = backendClient.createEvent(jwt, name, eventType, eventDate, location, null, null, null);
            return new QuickEventResult(true, created.id(), null);
        } catch (BackendApiException e) {
            return new QuickEventResult(false, null, e.getMessage());
        }
    }

    public record QuickEventResult(boolean success, Long eventId, String error) {
    }

    // For the storefront's guided flow's "is this for an existing event?"
    // step (see VendorController#loadStorefront and storefront.html's
    // Modal D) - a logged-in planner with events already on file gets to
    // pick one instead of always being pushed into creating a new one.
    // "Upcoming" = not already in the past; an event with no date at all
    // (never picked one) counts as upcoming too, since there's nothing to
    // say it isn't.
    @GetMapping("/upcoming")
    @ResponseBody
    public List<UpcomingEventOption> upcomingEvents(HttpSession session) {
        String jwt = WebSession.token(session);
        LocalDate today = LocalDate.now(MANILA);
        return backendClient.listEvents(jwt).stream()
                .filter(e -> e.eventDate() == null || !e.eventDate().isBefore(today))
                .sorted(Comparator.comparing(PlannerEventSummary::eventDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(e -> new UpcomingEventOption(
                        e.id(), e.name(), e.eventType() != null ? e.eventType().getLabel() : null, e.eventDate()))
                .toList();
    }

    public record UpcomingEventOption(long id, String name, String eventTypeLabel, LocalDate eventDate) {
    }

    /**
     * Opts a suggested category in/out of active browsing, with an optional
     * per-category budget earmark. Only ever called from the create-event
     * wizard's step 2 "Next" now (see events.html's script block for the
     * "no event selected" branch) - chained once per checked category,
     * right after the event itself is created. The response body is never
     * used by that caller, just whether the request succeeded.
     */
    @PostMapping("/{eventId}/supplier-types/{vendorType}")
    public ResponseEntity<Void> setSupplierTypeSelection(
            @PathVariable Long eventId,
            @PathVariable BusinessType vendorType,
            @RequestParam boolean selected,
            @RequestParam(required = false) java.math.BigDecimal budget,
            HttpSession session) {
        try {
            backendClient.setSupplierTypeSelection(WebSession.token(session), eventId, vendorType, selected, budget);
            return ResponseEntity.ok().build();
        } catch (BackendApiException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Removes a category from Search Result outright, deleting its
     * checklist items too - see BackendClient#removeCategorySelection /
     * EventService#removeCategorySelection for why this is distinct from
     * #setSupplierTypeSelection(selected=false).
     */
    @DeleteMapping("/{eventId}/supplier-types/{vendorType}")
    public ResponseEntity<Void> removeCategorySelection(
            @PathVariable Long eventId, @PathVariable BusinessType vendorType, HttpSession session) {
        try {
            backendClient.removeCategorySelection(WebSession.token(session), eventId, vendorType);
            return ResponseEntity.ok().build();
        } catch (BackendApiException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Deliberate name search for a specific supplier the planner already has
     * in mind - distinct from "Search Result"'s own category rows, which
     * only ever surface whoever matched a category the planner already
     * selected. Returns just the results fragment.
     */
    @GetMapping("/{eventId}/suppliers/search")
    public String searchSuppliers(
            @PathVariable Long eventId,
            @RequestParam String query,
            HttpSession session,
            Model model) {
        model.addAttribute("supplierSearchQuery", query);
        model.addAttribute("eventIdForSearch", eventId);
        try {
            model.addAttribute("supplierSearchResults",
                    backendClient.searchVendors(WebSession.token(session), query, null));
        } catch (BackendApiException e) {
            model.addAttribute("supplierSearchError", "Couldn't search suppliers. Please try again.");
        }
        return "planner/events :: supplierSearchResults";
    }

    /**
     * Read-only refresh of just the checklist fragment - called after
     * checking a supplier category (see events.html's JS), since
     * #setSupplierTypeSelection's own response only swaps the suppliers
     * panel and checking a category also creates a new checklist item
     * server-side.
     */
    @GetMapping("/{eventId}/checklist")
    public String getChecklist(@PathVariable Long eventId, HttpSession session, Model model) {
        String jwt = WebSession.token(session);
        try {
            PlannerEvent event = backendClient.getEvent(jwt, eventId);
            model.addAttribute("selectedEvent", event);
            addSupplierPanelAttributes(model, event);
        } catch (BackendApiException e) {
            log.warn("getChecklist failed for event {}: {} {}", eventId, e.getStatus(), e.getMessage());
            model.addAttribute("checklistError", "Couldn't load checklist. Please try again.");
        }
        return "planner/events :: checklistResults";
    }

    /**
     * Read-only refresh of just the Search Result fragment - called after
     * adding a category from the "Add category" pop-up (see events.html's
     * JS), same reasoning as #getChecklist: #setSupplierTypeSelection's own
     * response isn't a renderable fragment, just a success/failure signal.
     */
    @GetMapping("/{eventId}/search-result")
    public String getSearchResult(@PathVariable Long eventId, HttpSession session, Model model) {
        String jwt = WebSession.token(session);
        try {
            PlannerEvent event = backendClient.getEvent(jwt, eventId);
            model.addAttribute("selectedEvent", event);
            addSupplierPanelAttributes(model, event);
        } catch (BackendApiException e) {
            model.addAttribute("suppliersError", "Couldn't load Search Result. Please try again.");
        }
        return "planner/events :: searchResultContent";
    }

    /**
     * Adds a custom checklist item (the system-suggested "Book a X vendor"
     * ones come from #setSupplierTypeSelection instead). Re-renders just the
     * checklist fragment, same re-fetch-and-swap pattern as the supplier
     * panel's endpoints above - the checklist endpoints only return the
     * mutated ChecklistItemResponse, not a full event, so a fresh getEvent
     * is what rebuilds the rest of the fragment's model.
     */
    @PostMapping("/{eventId}/checklist")
    public String addChecklistItem(
            @PathVariable Long eventId,
            @RequestParam String label,
            HttpSession session,
            Model model) {
        String jwt = WebSession.token(session);
        try {
            backendClient.addChecklistItem(jwt, eventId, label);
            PlannerEvent updated = backendClient.getEvent(jwt, eventId);
            model.addAttribute("selectedEvent", updated);
            addSupplierPanelAttributes(model, updated);
        } catch (BackendApiException e) {
            model.addAttribute("checklistError", "Couldn't add checklist item. Please try again.");
        }
        return "planner/events :: checklistResults";
    }

    /** status is one of TODO/DONE/DISMISSED - see EventService#updateChecklistItemStatus on the backend; this endpoint just forwards it. */
    @PostMapping("/{eventId}/checklist/{itemId}/toggle")
    public String toggleChecklistItem(
            @PathVariable Long eventId,
            @PathVariable Long itemId,
            @RequestParam String status,
            HttpSession session,
            Model model) {
        String jwt = WebSession.token(session);
        try {
            backendClient.updateChecklistItemStatus(jwt, eventId, itemId, status);
            PlannerEvent updated = backendClient.getEvent(jwt, eventId);
            model.addAttribute("selectedEvent", updated);
            addSupplierPanelAttributes(model, updated);
        } catch (BackendApiException e) {
            model.addAttribute("checklistError", "Couldn't update checklist item. Please try again.");
        }
        return "planner/events :: checklistResults";
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

    /**
     * Planner accepts a QUOTE_SENT/REVISION_SENT quote - the quote freezes
     * from here (see backend QuotationStatus javadoc). A payment screenshot
     * can be attached right in this same step (optional, same combined UX
     * the old "Book This" modal had) - the backend auto-resolves to
     * PENDING_DEPOSIT or PAYMENT_REVIEW depending on whether one was sent.
     * No Booking record exists yet at this point - that only happens once
     * the vendor confirms via QuotationService#acceptBooking.
     */
    @PostMapping("/{eventId}/quotations/{quotationId}/accept")
    public String acceptQuote(
            @PathVariable Long eventId,
            @PathVariable Long quotationId,
            @RequestParam(required = false) Integer acceptedVersion,
            @RequestParam(required = false) String message,
            @RequestParam(required = false) MultipartFile screenshot,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (screenshot != null && !screenshot.isEmpty() && !isPngOrJpeg(screenshot)) {
            redirectAttributes.addFlashAttribute("bookingsError", "Only PNG or JPEG images are accepted for payment screenshots.");
            redirectAttributes.addAttribute("eventId", eventId);
            redirectAttributes.addAttribute("tab", "quotations");
            return "redirect:/planner/events";
        }
        try {
            backendClient.acceptQuote(WebSession.token(session), quotationId, acceptedVersion, message, screenshot);
            redirectAttributes.addFlashAttribute("quotationAccepted", true);
            redirectAttributes.addFlashAttribute("bookingIncludedScreenshot", screenshot != null && !screenshot.isEmpty());
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("bookingsError", e.getMessage());
        }
        redirectAttributes.addAttribute("eventId", eventId);
        redirectAttributes.addAttribute("tab", "quotations");
        return "redirect:/planner/events";
    }

    /** Standalone upload - for a planner who accepted without a screenshot and comes back once ready to pay, or resubmitting after a rejection. */
    @PostMapping("/{eventId}/quotations/{quotationId}/payment-screenshot")
    public String submitQuotationPaymentScreenshot(
            @PathVariable Long eventId,
            @PathVariable Long quotationId,
            @RequestParam(required = false) MultipartFile screenshot,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (screenshot == null || screenshot.isEmpty()) {
            redirectAttributes.addFlashAttribute("bookingsError", "A payment screenshot is required.");
        } else if (!isPngOrJpeg(screenshot)) {
            redirectAttributes.addFlashAttribute("bookingsError", "Only PNG or JPEG images are accepted for payment screenshots.");
        } else {
            try {
                backendClient.submitQuotationPaymentScreenshot(WebSession.token(session), quotationId, screenshot);
                redirectAttributes.addFlashAttribute("screenshotSubmitted", true);
            } catch (BackendApiException e) {
                redirectAttributes.addFlashAttribute("bookingsError", e.getMessage());
            }
        }
        redirectAttributes.addAttribute("eventId", eventId);
        redirectAttributes.addAttribute("tab", "quotations");
        return "redirect:/planner/events";
    }

    // Fired by JS (see planner/events.html's shown.bs.tab wiring) the moment
    // the planner actually clicks the Quotations/Bookings tab for this
    // event - not tied to the page's own initial load, since both tabs'
    // content is already sitting in this same server-rendered page (see
    // events() above) rather than a separate navigation the way it can be
    // for the vendor's own /vendor/quotations and /vendor/bookings pages.
    @PostMapping("/{eventId}/quotations/mark-seen")
    @ResponseBody
    public ResponseEntity<Void> markQuotationsTabSeen(@PathVariable Long eventId, HttpSession session) {
        backendClient.markEventQuotationsSeen(WebSession.token(session), eventId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{eventId}/bookings/mark-seen")
    @ResponseBody
    public ResponseEntity<Void> markBookingsTabSeen(@PathVariable Long eventId, HttpSession session) {
        backendClient.markEventBookingsSeen(WebSession.token(session), eventId);
        return ResponseEntity.ok().build();
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
            @RequestParam(required = false) List<MultipartFile> images,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (message == null || message.isBlank()) {
            redirectAttributes.addFlashAttribute("bookingsError", "A message describing what you'd like changed is required.");
            redirectAttributes.addAttribute("eventId", eventId);
            redirectAttributes.addAttribute("tab", "quotations");
            return "redirect:/planner/events";
        }
        try {
            backendClient.reviseQuotation(WebSession.token(session), quotationId, targetDate, message, null, images);
            redirectAttributes.addFlashAttribute("quotationRevised", true);
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("bookingsError", e.getMessage());
        }
        redirectAttributes.addAttribute("eventId", eventId);
        redirectAttributes.addAttribute("tab", "quotations");
        return "redirect:/planner/events";
    }

    @PostMapping("/{eventId}/quotations/{quotationId}/attachments")
    public String addQuotationAttachment(
            @PathVariable Long eventId,
            @PathVariable Long quotationId,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String message,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("bookingsError", "A file is required to send an attachment.");
            redirectAttributes.addAttribute("eventId", eventId);
            redirectAttributes.addAttribute("tab", "quotations");
            return "redirect:/planner/events";
        }
        try {
            backendClient.sendQuotationAttachment(WebSession.token(session), quotationId, file, message);
            redirectAttributes.addFlashAttribute("attachmentSent", true);
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
            @RequestParam(required = false) MultipartFile attachment,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (body == null || body.isBlank()) {
            redirectAttributes.addFlashAttribute("bookingsError", "A message is required.");
        } else {
            try {
                backendClient.replyToConversation(WebSession.token(session), conversationId, body, attachment);
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
