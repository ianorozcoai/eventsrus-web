package com.web.eventsrus.controller;

import com.web.eventsrus.admin.AdminAccountService;
import com.web.eventsrus.model.AdminPlannerSummary;
import com.web.eventsrus.model.PlannerVendorSuggestion;
import com.web.eventsrus.stub.StubDataService;
import java.util.Comparator;
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
 * Vendor directory, and admin-account management. Stub-data-backed, same as
 * every other non-subscription feature in this app: there's no real admin
 * directory/stats endpoint on eventsrus-backend yet for this to call.
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final StubDataService stubDataService;
    private final AdminAccountService adminAccountService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute(
                "plannerCount", stubDataService.loadList("admin-planners.json", AdminPlannerSummary.class).size());
        model.addAttribute(
                "vendorCount", stubDataService.loadList("vendor-directory.json", PlannerVendorSuggestion.class).size());
        model.addAttribute("activePage", "dashboard");
        return "admin/dashboard";
    }

    @GetMapping("/planners")
    public String planners(Model model) {
        List<AdminPlannerSummary> planners = stubDataService
                .loadList("admin-planners.json", AdminPlannerSummary.class).stream()
                .sorted(Comparator.comparing(AdminPlannerSummary::joinedAt).reversed())
                .toList();
        model.addAttribute("planners", planners);
        model.addAttribute("activePage", "planners");
        return "admin/planners";
    }

    @GetMapping("/planners/{id}")
    public String plannerProfile(@PathVariable long id, Model model) {
        AdminPlannerSummary planner = stubDataService
                .loadList("admin-planners.json", AdminPlannerSummary.class).stream()
                .filter(p -> p.id() == id)
                .findFirst()
                .orElse(null);
        if (planner == null) {
            return "redirect:/admin/planners";
        }
        model.addAttribute("planner", planner);
        model.addAttribute("activePage", "planners");
        return "admin/planner-profile";
    }

    @GetMapping("/vendors")
    public String vendors(Model model) {
        model.addAttribute("vendors", stubDataService.loadList("vendor-directory.json", PlannerVendorSuggestion.class));
        model.addAttribute("activePage", "vendors");
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
