package com.web.eventsrus.controller;

import com.web.eventsrus.model.BookFromQuotationForm;
import com.web.eventsrus.model.EventType;
import com.web.eventsrus.model.PhilippineProvinces;
import com.web.eventsrus.model.PlannerChatMessage;
import com.web.eventsrus.model.PlannerEvent;
import com.web.eventsrus.model.PlannerIntakeForm;
import com.web.eventsrus.model.PlannerVendorSuggestion;
import com.web.eventsrus.model.VendorBooking;
import com.web.eventsrus.model.VendorConversation;
import com.web.eventsrus.model.VendorConversationMessage;
import com.web.eventsrus.model.VendorQuotation;
import com.web.eventsrus.stub.PlannerCoordinatorService;
import com.web.eventsrus.stub.PlannerEventSessionService;
import com.web.eventsrus.stub.StubDataService;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
 * Stub-only, same as every other planner/vendor screen this session: no
 * real backend calls, no real login. To make "previous events" mean
 * something without a database, the planner's event list lives in the
 * HTTP session - seeded from stubs/planner-events.json on first visit,
 * and a new event submitted mid-session is actually appended to it, so
 * the sidebar behaves believably for the length of one visit. This is a
 * deliberate middle ground, not real persistence - it resets when the
 * session ends.
 */
@Controller
@RequestMapping("/planner/events")
public class PlannerController {

    private static final String SESSION_CHAT_KEY = "plannerChatThreads";
    private static final DateTimeFormatter SUMMARY_DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);
    // The stub "logged in" planner - matches the planner id already
    // cross-referenced across messages.json/quotations.json/bookings.json
    // for events 501/504/510 ("Ivs Phil").
    private static final long PLANNER_USER_ID = 101L;
    // Matches VendorController's single stub vendor (VENDOR_USER_ID = 9001L).
    private static final String VENDOR_BUSINESS_NAME = "Ian's Premium Events";
    private static final String VENDOR_SLUG = "ians-premium-events";

    private final StubDataService stubDataService;
    private final PlannerCoordinatorService plannerCoordinatorService;
    private final PlannerEventSessionService plannerEventSessionService;

    public PlannerController(
            StubDataService stubDataService,
            PlannerCoordinatorService plannerCoordinatorService,
            PlannerEventSessionService plannerEventSessionService) {
        this.stubDataService = stubDataService;
        this.plannerCoordinatorService = plannerCoordinatorService;
        this.plannerEventSessionService = plannerEventSessionService;
    }

    @GetMapping
    public String events(
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long conversationId,
            @RequestParam(required = false, defaultValue = "overview") String tab,
            HttpSession session,
            Model model) {
        List<PlannerEvent> events = plannerEventSessionService.events(session);
        model.addAttribute("events", events);
        model.addAttribute("activeEventId", eventId);
        model.addAttribute("activeTab", tab);

        PlannerEvent selected = eventId == null
                ? null
                : events.stream().filter(e -> e.id() == eventId).findFirst().orElse(null);
        model.addAttribute("selectedEvent", selected);

        if (selected == null) {
            if (!model.containsAttribute("plannerIntakeForm")) {
                model.addAttribute("plannerIntakeForm", new PlannerIntakeForm());
            }
            model.addAttribute("eventTypes", EventType.values());
            model.addAttribute("provinces", PhilippineProvinces.ALL);
            return "planner/events";
        }

        List<VendorQuotation> quotations = stubDataService.loadList("quotations.json", VendorQuotation.class).stream()
                .filter(q -> q.eventId() == selected.id())
                .toList();
        List<VendorBooking> bookings = stubDataService.loadList("bookings.json", VendorBooking.class).stream()
                .filter(b -> b.eventId() == selected.id())
                .toList();
        List<VendorConversation> conversations = stubDataService.loadList("messages.json", VendorConversation.class).stream()
                .filter(c -> c.eventId() == selected.id())
                .sorted(Comparator.comparing(VendorConversation::lastMessageAt).reversed())
                .toList();

        VendorConversation selectedConversation = conversations.stream()
                .filter(c -> c.id() == (conversationId != null ? conversationId : -1))
                .findFirst()
                .orElseGet(() -> conversations.isEmpty() ? null : conversations.get(0));

        Map<String, List<VendorConversationMessage>> threads =
                stubDataService.loadMapOfLists("conversation-messages.json", VendorConversationMessage.class);
        List<VendorConversationMessage> thread = selectedConversation == null
                ? List.of()
                : threads.getOrDefault(String.valueOf(selectedConversation.id()), List.of()).stream()
                        .sorted(Comparator.comparing(VendorConversationMessage::createdAt))
                        .toList();

        model.addAttribute("quotations", quotations);
        model.addAttribute("bookings", bookings);
        model.addAttribute("conversations", conversations);
        model.addAttribute("selectedConversation", selectedConversation);
        model.addAttribute("thread", thread);
        model.addAttribute("chatThread", chatThreadFor(session, selected));
        model.addAttribute("plannerUserId", PLANNER_USER_ID);
        // Every conversation in this stub universe is with the one vendor
        // this whole eventsrus-web app is built around (VENDOR_USER_ID in
        // VendorController) - messages.json's own otherPartyName field is
        // vendor-relative (authored for /vendor/messages, where "other
        // party" means the planner), so it's wrong to show verbatim here;
        // this is what "who you're chatting with" should say instead.
        model.addAttribute("vendorBusinessName", VENDOR_BUSINESS_NAME);
        model.addAttribute("vendorSlug", VENDOR_SLUG);
        return "planner/events";
    }

    @PostMapping
    public String submitIntake(
            @ModelAttribute PlannerIntakeForm plannerIntakeForm, HttpSession session, RedirectAttributes redirectAttributes) {
        List<PlannerVendorSuggestion> suggestions = plannerCoordinatorService.matchSuppliers(
                plannerIntakeForm.getEventType(), plannerIntakeForm.getDescription(), plannerIntakeForm.getLocation());
        String coordinatorReply = plannerCoordinatorService.coordinatorReply(plannerIntakeForm.getEventType());

        PlannerEvent newEvent = new PlannerEvent(
                System.currentTimeMillis(),
                plannerIntakeForm.getName(),
                plannerIntakeForm.getEventType(),
                plannerIntakeForm.getEventDate(),
                plannerIntakeForm.getLocation(),
                plannerIntakeForm.getDescription(),
                coordinatorReply,
                suggestions);

        List<PlannerEvent> events = plannerEventSessionService.events(session);
        events.add(0, newEvent);

        redirectAttributes.addAttribute("eventId", newEvent.id());
        return "redirect:/planner/events";
    }

    @PostMapping("/{eventId}/quotations/{quotationId}/book")
    public String bookFromQuotation(
            @PathVariable Long eventId,
            @PathVariable Long quotationId,
            @ModelAttribute BookFromQuotationForm bookFromQuotationForm,
            @RequestParam(required = false) MultipartFile screenshot,
            RedirectAttributes redirectAttributes) {
        // Stub only for now - proves the round trip; wires up to
        // POST /api/v1/quotations/{quotationId}/book (then, if a screenshot
        // was attached, POST /api/v1/bookings/{bookingId}/payment-screenshot)
        // on eventsrus-backend later - both already do this for real
        // (BookingService#bookFromQuotation/submitPaymentScreenshot).
        // Doesn't actually create a booking or store the file here.
        if (screenshot != null && !screenshot.isEmpty() && !isPngOrJpeg(screenshot)) {
            redirectAttributes.addFlashAttribute("bookingsError", "Only PNG or JPEG images are accepted for payment screenshots.");
            redirectAttributes.addAttribute("eventId", eventId);
            redirectAttributes.addAttribute("tab", "quotations");
            return "redirect:/planner/events";
        }
        redirectAttributes.addFlashAttribute("bookingSubmitted", true);
        redirectAttributes.addFlashAttribute("bookingIncludedScreenshot", screenshot != null && !screenshot.isEmpty());
        redirectAttributes.addAttribute("eventId", eventId);
        redirectAttributes.addAttribute("tab", "quotations");
        return "redirect:/planner/events";
    }

    @PostMapping("/{eventId}/quotations/{quotationId}/decline")
    public String declineQuotation(
            @PathVariable Long eventId, @PathVariable Long quotationId, RedirectAttributes redirectAttributes) {
        // Stub only for now - proves the round trip; wires up to
        // PUT /api/v1/quotations/{quotationId}/decline on eventsrus-backend
        // later (QuotationService#declineQuotation already does this for
        // real). Doesn't actually flip the quotation's status here.
        redirectAttributes.addFlashAttribute("quotationDeclined", true);
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
            RedirectAttributes redirectAttributes) {
        // Stub only for now - proves the round trip; wires up to
        // POST /api/v1/quotations/{quotationId}/revise on eventsrus-backend
        // later (QuotationService#requestRevision already does this for
        // real - sends the quotation back to REQUESTED with the updated ask
        // instead of starting a whole new thread). Doesn't actually reset
        // the quotation's status here.
        if (message == null || message.isBlank()) {
            redirectAttributes.addFlashAttribute("bookingsError", "A message describing what you'd like changed is required.");
            redirectAttributes.addAttribute("eventId", eventId);
            redirectAttributes.addAttribute("tab", "quotations");
            return "redirect:/planner/events";
        }
        redirectAttributes.addFlashAttribute("quotationRevised", true);
        redirectAttributes.addAttribute("eventId", eventId);
        redirectAttributes.addAttribute("tab", "quotations");
        return "redirect:/planner/events";
    }

    @PostMapping("/{eventId}/bookings/{bookingId}/cancel")
    public String cancelBooking(
            @PathVariable Long eventId,
            @PathVariable Long bookingId,
            @RequestParam String reason,
            RedirectAttributes redirectAttributes) {
        // Stub only for now - proves the round trip; wires up to
        // PUT /api/v1/bookings/{bookingId}/cancel on eventsrus-backend later
        // (BookingService#cancel already does this for real). Doesn't
        // actually flip the booking's status here.
        if (reason == null || reason.isBlank()) {
            redirectAttributes.addFlashAttribute("bookingsError", "A cancellation reason is required.");
        } else {
            redirectAttributes.addFlashAttribute("bookingCancelled", true);
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
            RedirectAttributes redirectAttributes) {
        // Mirrors eventsrus-backend's own image-type validation on this
        // endpoint (BookingService#submitPaymentScreenshot) - stub only for
        // now otherwise, doesn't actually store the file or flip the
        // booking's status.
        if (screenshot == null || screenshot.isEmpty()) {
            redirectAttributes.addFlashAttribute("bookingsError", "A payment screenshot is required.");
        } else if (!isPngOrJpeg(screenshot)) {
            redirectAttributes.addFlashAttribute("bookingsError", "Only PNG or JPEG images are accepted for payment screenshots.");
        } else {
            redirectAttributes.addFlashAttribute("screenshotSubmitted", true);
        }
        redirectAttributes.addAttribute("eventId", eventId);
        redirectAttributes.addAttribute("tab", "bookings");
        return "redirect:/planner/events";
    }

    @PostMapping("/{eventId}/coordinator-chat")
    public String sendCoordinatorChatMessage(
            @PathVariable Long eventId,
            @RequestParam String message,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        List<PlannerEvent> events = plannerEventSessionService.events(session);
        PlannerEvent event = events.stream().filter(e -> e.id() == eventId).findFirst().orElse(null);
        if (event != null && message != null && !message.isBlank()) {
            List<PlannerChatMessage> thread = chatThreadFor(session, event);
            thread.add(new PlannerChatMessage(true, message, Instant.now()));
            thread.add(new PlannerChatMessage(false, plannerCoordinatorService.followUpReply(message), Instant.now()));
        }
        redirectAttributes.addAttribute("eventId", eventId);
        redirectAttributes.addAttribute("tab", "overview");
        return "redirect:/planner/events";
    }

    @PostMapping("/{eventId}/conversations/{conversationId}/reply")
    public String replyToConversation(
            @PathVariable Long eventId,
            @PathVariable Long conversationId,
            @RequestParam String body,
            RedirectAttributes redirectAttributes) {
        // Stub only for now - proves the round trip; wires up to
        // POST /api/v1/conversations/{id}/messages on eventsrus-backend
        // later. Doesn't actually append to the thread (StubDataService
        // re-reads the static JSON every request), so the reply won't
        // appear after redirect - same limitation as the vendor side.
        redirectAttributes.addFlashAttribute("replySent", true);
        redirectAttributes.addAttribute("eventId", eventId);
        redirectAttributes.addAttribute("tab", "chats");
        redirectAttributes.addAttribute("conversationId", conversationId);
        return "redirect:/planner/events";
    }

    private boolean isPngOrJpeg(MultipartFile file) {
        String contentType = file.getContentType();
        return "image/png".equals(contentType) || "image/jpeg".equals(contentType);
    }

    // Lazily seeds an event's Overview-tab chat thread from its own
    // description/coordinatorReply the first time it's viewed, so there's
    // one source of truth rather than duplicating that text into
    // planner-events.json a second time. Returns the live, mutable list
    // already held in the session, so callers can append directly to it.
    @SuppressWarnings("unchecked")
    private List<PlannerChatMessage> chatThreadFor(HttpSession session, PlannerEvent event) {
        Map<Long, List<PlannerChatMessage>> threads = (Map<Long, List<PlannerChatMessage>>) session.getAttribute(SESSION_CHAT_KEY);
        if (threads == null) {
            threads = new HashMap<>();
            session.setAttribute(SESSION_CHAT_KEY, threads);
        }
        return threads.computeIfAbsent(event.id(), id -> {
            List<PlannerChatMessage> seeded = new ArrayList<>();
            seeded.add(new PlannerChatMessage(true, summarize(event), Instant.now()));
            seeded.add(new PlannerChatMessage(false, event.coordinatorReply(), Instant.now()));
            return seeded;
        });
    }

    private String summarize(PlannerEvent event) {
        StringBuilder summary = new StringBuilder(event.eventType() != null ? event.eventType().getLabel() : "Event")
                .append(" - \"").append(event.name()).append("\"");
        if (event.eventDate() != null) {
            summary.append(" on ").append(event.eventDate().format(SUMMARY_DATE_FORMAT));
        }
        if (event.location() != null && !event.location().isBlank()) {
            summary.append(" in ").append(event.location());
        }
        summary.append(".");
        if (event.description() != null && !event.description().isBlank()) {
            summary.append(' ').append(event.description());
        }
        return summary.toString();
    }
}
