package com.web.eventsrus.controller;

import com.web.eventsrus.model.CreateTicketForm;
import com.web.eventsrus.model.SupportTicket;
import com.web.eventsrus.model.SupportTicketMessage;
import com.web.eventsrus.model.TicketCategory;
import com.web.eventsrus.stub.PlannerEventSessionService;
import com.web.eventsrus.stub.StubDataService;
import jakarta.servlet.http.HttpSession;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The planner's Support page - reached from the Account dropdown in
 * fragments/planner-shell.html. A sibling of PlannerController's own
 * /planner/events (kept as a separate controller, same reasoning as
 * PlannerProfileController: PlannerController is class-level mapped at
 * "/planner/events", so a method there can't "escape" to a /planner/support
 * sibling path). Symmetric with VendorController's /vendor/support.
 * Stub-only for now - eventsrus-backend's SupportTicketController already
 * does this for real, this just doesn't call it yet.
 */
@Controller
public class PlannerSupportController {

    // Matches PlannerController's own PLANNER_USER_ID - the stub "logged in" planner ("Ivs Phil").
    private static final long PLANNER_USER_ID = 101L;

    private final StubDataService stubDataService;
    private final PlannerEventSessionService plannerEventSessionService;

    public PlannerSupportController(
            StubDataService stubDataService, PlannerEventSessionService plannerEventSessionService) {
        this.stubDataService = stubDataService;
        this.plannerEventSessionService = plannerEventSessionService;
    }

    @GetMapping("/planner/support")
    public String support(@RequestParam(required = false) Long ticketId, HttpSession session, Model model) {
        List<SupportTicket> tickets = stubDataService.loadList("support-tickets.json", SupportTicket.class).stream()
                .filter(t -> t.raisedByUserId() == PLANNER_USER_ID)
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
        model.addAttribute("currentUserId", PLANNER_USER_ID);
        // Same event list as /planner/events, so the sidebar looks
        // identical no matter which planner page is open.
        model.addAttribute("events", plannerEventSessionService.events(session));
        model.addAttribute("activeEventId", null);
        return "planner/support";
    }

    @PostMapping("/planner/support")
    public String createTicket(@ModelAttribute CreateTicketForm createTicketForm, RedirectAttributes redirectAttributes) {
        // Stub only for now - proves the round trip; wires up to
        // POST /api/v1/support-tickets on eventsrus-backend later
        // (SupportTicketService#createTicket already does this for real).
        redirectAttributes.addFlashAttribute("ticketCreated", true);
        return "redirect:/planner/support";
    }

    @PostMapping("/planner/support/{ticketId}/reply")
    public String replyToTicket(
            @PathVariable Long ticketId, @RequestParam String message, RedirectAttributes redirectAttributes) {
        // Stub only for now - wires up to
        // POST /api/v1/support-tickets/{id}/messages on eventsrus-backend
        // later (SupportTicketService#reply already does this for real).
        if (message == null || message.isBlank()) {
            redirectAttributes.addFlashAttribute("supportError", "A message is required.");
        } else {
            redirectAttributes.addFlashAttribute("replySent", true);
        }
        redirectAttributes.addAttribute("ticketId", ticketId);
        return "redirect:/planner/support";
    }
}
