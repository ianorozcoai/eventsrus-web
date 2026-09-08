package com.web.eventsrus.controller;

import com.web.eventsrus.admin.AdminSession;
import com.web.eventsrus.backend.BackendApiException;
import com.web.eventsrus.backend.BackendClient;
import com.web.eventsrus.model.SupportTicket;
import com.web.eventsrus.model.SupportTicketMessage;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Admin's support-ticket inboxes - deliberately two separate pages/nav items
 * (Vendor Complaints, Planner Complaints), not one combined list with a
 * filter, since the two audiences and what they complain about are
 * different enough to want their own queue (same reasoning as
 * AdminVerificationController being its own page rather than folded into
 * Vendors). Both share one template (admin/support.html) - only the model's
 * raisedByRole/activePage/queueTitle differ. Same two-pane messenger layout
 * as vendor/support.html; viewing a thread, replying, and changing status
 * all reuse the exact same real eventsrus-backend endpoints the vendor-side
 * page calls (any admin can already act on any ticket there, not just their
 * own - see SupportTicketService), so nothing new was needed on that end
 * beyond the one thing that was actually missing: AdminSupportTicketController
 * on eventsrus-backend, which lists every ticket instead of just the
 * caller's own.
 */
@Controller
@RequestMapping("/admin/support")
@RequiredArgsConstructor
public class AdminSupportTicketController {

    private final BackendClient backendClient;

    @GetMapping("/vendors")
    public String vendorTickets(@RequestParam(required = false) Long ticketId, HttpSession session, Model model) {
        return loadInbox("VENDOR", "Vendor Complaints", "support-vendors", ticketId, session, model);
    }

    @GetMapping("/planners")
    public String plannerTickets(@RequestParam(required = false) Long ticketId, HttpSession session, Model model) {
        return loadInbox("PLANNER", "Planner Complaints", "support-planners", ticketId, session, model);
    }

    private String loadInbox(
            String raisedByRole, String queueTitle, String activePage, Long ticketId, HttpSession session, Model model) {
        model.addAttribute("activePage", activePage);
        model.addAttribute("queueTitle", queueTitle);
        model.addAttribute("redirectTo", "/admin/support/" + (raisedByRole.equals("VENDOR") ? "vendors" : "planners"));
        // Always set explicitly (never left absent/null) - the template
        // combines it with "and" in a single th:if expression
        // (${not backendUnavailable and ...}), and SpringEL's "not" throws
        // rather than treating a null/missing attribute as falsy the way
        // Thymeleaf's own th:if/th:unless would on their own.
        String jwt = AdminSession.token(session);
        if (jwt == null) {
            model.addAttribute("backendUnavailable", true);
            model.addAttribute("tickets", List.of());
            return "admin/support";
        }
        try {
            List<SupportTicket> tickets = backendClient.getSupportTicketsForAdmin(jwt, raisedByRole);
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
        return "admin/support";
    }

    @PostMapping("/{ticketId}/reply")
    public String reply(
            @PathVariable Long ticketId, @RequestParam String message,
            @RequestParam(required = false) MultipartFile attachment,
            @RequestParam String redirectTo, HttpSession session, RedirectAttributes redirectAttributes) {
        String jwt = requireBackendToken(session, redirectAttributes);
        if (jwt != null) {
            try {
                backendClient.replyToSupportTicket(jwt, ticketId, message, attachment);
                redirectAttributes.addFlashAttribute("replySent", true);
            } catch (BackendApiException e) {
                redirectAttributes.addFlashAttribute("supportError", e.getMessage());
            }
        }
        redirectAttributes.addAttribute("ticketId", ticketId);
        return "redirect:" + redirectTo;
    }

    @PostMapping("/{ticketId}/status")
    public String updateStatus(
            @PathVariable Long ticketId, @RequestParam String status,
            @RequestParam String redirectTo, HttpSession session, RedirectAttributes redirectAttributes) {
        String jwt = requireBackendToken(session, redirectAttributes);
        if (jwt != null) {
            try {
                backendClient.updateSupportTicketStatus(jwt, ticketId, status);
                redirectAttributes.addFlashAttribute("statusUpdated", true);
            } catch (BackendApiException e) {
                redirectAttributes.addFlashAttribute("supportError", e.getMessage());
            }
        }
        redirectAttributes.addAttribute("ticketId", ticketId);
        return "redirect:" + redirectTo;
    }

    private String requireBackendToken(HttpSession session, RedirectAttributes redirectAttributes) {
        String jwt = AdminSession.token(session);
        if (jwt == null) {
            redirectAttributes.addFlashAttribute(
                    "supportError", "Not connected to eventsrus-backend - log out and back in to retry.");
        }
        return jwt;
    }
}
