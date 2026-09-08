package com.web.eventsrus.controller;

import com.web.eventsrus.admin.AdminAccountService;
import com.web.eventsrus.admin.AdminSession;
import com.web.eventsrus.backend.BackendApiException;
import com.web.eventsrus.backend.BackendClient;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The admin module - dashboard totals, a read-only Planner directory and
 * Vendor directory, and admin-account management. Real eventsrus-backend
 * data now, same pattern as AdminVerificationController/AdminSupportTicketController
 * (an admin session's real ADMIN-role JWT via the internal bridge - see
 * AdminSession/AdminAuthController) - falls back to a friendly "backend
 * unavailable" notice rather than a stack trace if that bridge didn't
 * produce a token.
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final BackendClient backendClient;
    private final AdminAccountService adminAccountService;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        model.addAttribute("activePage", "dashboard");
        String jwt = AdminSession.token(session);
        if (jwt == null) {
            model.addAttribute("backendUnavailable", true);
            return "admin/dashboard";
        }
        try {
            var stats = backendClient.getAdminDashboardStats(jwt);
            model.addAttribute("backendUnavailable", false);
            model.addAttribute("plannerCount", stats.plannerCount());
            model.addAttribute("vendorCount", stats.vendorCount());
            model.addAttribute("vendorTicketCount", stats.vendorTicketCount());
            model.addAttribute("newVendorTicketCount", stats.newVendorTicketCount());
            model.addAttribute("newPlannerTicketCount", stats.newPlannerTicketCount());
        } catch (BackendApiException e) {
            model.addAttribute("backendUnavailable", true);
        }
        return "admin/dashboard";
    }

    @GetMapping("/planners")
    public String planners(HttpSession session, Model model) {
        model.addAttribute("activePage", "planners");
        String jwt = AdminSession.token(session);
        if (jwt == null) {
            model.addAttribute("backendUnavailable", true);
            model.addAttribute("planners", List.of());
            return "admin/planners";
        }
        try {
            model.addAttribute("backendUnavailable", false);
            model.addAttribute("planners", backendClient.listPlannersForAdmin(jwt));
        } catch (BackendApiException e) {
            model.addAttribute("backendUnavailable", true);
            model.addAttribute("planners", List.of());
        }
        return "admin/planners";
    }

    @GetMapping("/planners/{id}")
    public String plannerProfile(@PathVariable long id, HttpSession session, Model model) {
        model.addAttribute("activePage", "planners");
        String jwt = AdminSession.token(session);
        if (jwt == null) {
            return "redirect:/admin/planners";
        }
        try {
            model.addAttribute("planner", backendClient.getPlannerForAdmin(jwt, id));
        } catch (BackendApiException e) {
            return "redirect:/admin/planners";
        }
        return "admin/planner-profile";
    }

    @GetMapping("/vendors")
    public String vendors(HttpSession session, Model model) {
        model.addAttribute("activePage", "vendors");
        String jwt = AdminSession.token(session);
        if (jwt == null) {
            model.addAttribute("backendUnavailable", true);
            model.addAttribute("vendors", List.of());
            return "admin/vendors";
        }
        try {
            model.addAttribute("backendUnavailable", false);
            model.addAttribute("vendors", backendClient.listVendorsForAdmin(jwt));
        } catch (BackendApiException e) {
            model.addAttribute("backendUnavailable", true);
            model.addAttribute("vendors", List.of());
        }
        return "admin/vendors";
    }

    @GetMapping("/admins")
    public String admins(Model model) {
        model.addAttribute("admins", adminAccountService.listAll());
        model.addAttribute("activePage", "admins");
        return "admin/admins";
    }

    @PostMapping("/admins")
    public String createAdmin(
            @RequestParam String username, @RequestParam String password, RedirectAttributes redirectAttributes) {
        String trimmedUsername = username == null ? "" : username.trim();
        if (trimmedUsername.isBlank() || password == null || password.length() < 8) {
            redirectAttributes.addFlashAttribute(
                    "adminsError", "Username is required and password must be at least 8 characters.");
            return "redirect:/admin/admins";
        }
        if (adminAccountService.usernameExists(trimmedUsername)) {
            redirectAttributes.addFlashAttribute("adminsError", "That username is already taken.");
            return "redirect:/admin/admins";
        }
        adminAccountService.create(trimmedUsername, password);
        redirectAttributes.addFlashAttribute("adminCreated", true);
        return "redirect:/admin/admins";
    }
}
