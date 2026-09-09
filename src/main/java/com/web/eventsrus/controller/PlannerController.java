package com.web.eventsrus.controller;

import com.web.eventsrus.backend.BackendApiException;
import com.web.eventsrus.backend.BackendClient;
import com.web.eventsrus.backend.WebSession;
import com.web.eventsrus.model.EventType;
import com.web.eventsrus.model.PhilippineProvinces;
import com.web.eventsrus.model.PlannerEvent;
import com.web.eventsrus.model.PlannerEventSummary;
import com.web.eventsrus.model.PlannerIntakeForm;
import com.web.eventsrus.model.VendorBooking;
import com.web.eventsrus.model.VendorConversation;
import com.web.eventsrus.model.VendorConversationMessage;
import com.web.eventsrus.model.VendorQuotation;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
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
            model.addAttribute("eventTypes", EventType.values());
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

        model.addAttribute("quotations", quotations);
        model.addAttribute("bookings", bookings);
        model.addAttribute("conversations", conversations);
        model.addAttribute("selectedConversation", selectedConversation);
        model.addAttribute("thread", thread);
        model.addAttribute("coordinatorHistory", backendClient.getCoordinatorHistory(jwt, selected.id()));
        return "planner/events";
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
