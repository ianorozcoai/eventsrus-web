package com.web.eventsrus.controller;

import com.web.eventsrus.backend.BackendApiException;
import com.web.eventsrus.backend.BackendClient;
import com.web.eventsrus.backend.WebSession;
import com.web.eventsrus.model.CreateTicketForm;
import com.web.eventsrus.model.SupportTicket;
import com.web.eventsrus.model.SupportTicketMessage;
import com.web.eventsrus.model.TicketCategory;
import jakarta.servlet.http.HttpSession;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The planner's Support page - reached from the Account dropdown in
 * fragments/planner-shell.html. A sibling of PlannerController's own
 * /planner/events (kept as a separate controller, same reasoning as
 * PlannerProfileController: PlannerController is class-level mapped at
 * "/planner/events", so a method there can't "escape" to a /planner/support
 * sibling path). Symmetric with VendorController's /vendor/support - real
 * eventsrus-backend data now, including screenshot attachments.
 */
@Controller
public class PlannerSupportController {

    private final BackendClient backendClient;

    public PlannerSupportController(BackendClient backendClient) {
        this.backendClient = backendClient;
    }

    @GetMapping("/planner/support")
    public String support(@RequestParam(required = false) Long ticketId, HttpSession session, Model model) {
        String jwt = WebSession.token(session);
        if (!model.containsAttribute("createTicketForm")) {
            model.addAttribute("createTicketForm", new CreateTicketForm());
        }
        model.addAttribute("ticketCategories", TicketCategory.values());
        model.addAttribute("currentUserId", WebSession.userId(session));
        // Same event list as /planner/events, so the sidebar looks
        // identical no matter which planner page is open.
        model.addAttribute("events", backendClient.listEvents(jwt));
        model.addAttribute("activeEventId", null);

        try {
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
            model.addAttribute("backendUnavailable", false);
            model.addAttribute("tickets", tickets);
            model.addAttribute("selectedTicket", selectedTicket);
            model.addAttribute("thread", thread);
        } catch (BackendApiException e) {
            model.addAttribute("backendUnavailable", true);
            model.addAttribute("tickets", List.of());
        }
        return "planner/support";
    }

    @PostMapping("/planner/support")
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
        return "redirect:/planner/support";
    }

    @PostMapping("/planner/support/{ticketId}/reply")
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
        return "redirect:/planner/support";
    }
}
