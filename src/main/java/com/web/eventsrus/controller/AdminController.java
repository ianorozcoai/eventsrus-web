package com.web.eventsrus.controller;

import com.web.eventsrus.admin.AdminSession;
import com.web.eventsrus.backend.BackendAdminVendorListItem;
import com.web.eventsrus.backend.BackendApiException;
import com.web.eventsrus.backend.BackendAuthResponse;
import com.web.eventsrus.backend.BackendClient;
import com.web.eventsrus.backend.WebSession;
import com.web.eventsrus.model.BusinessType;
import com.web.eventsrus.model.PhilippineProvinces;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
 * (an admin session's real ADMIN-role JWT, issued at login time - see
 * AdminSession/AdminAuthController) - falls back to a friendly "backend
 * unavailable" notice rather than a stack trace if that token is missing.
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final BackendClient backendClient;

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
            model.addAttribute("incompleteVendorSignupCount", stats.incompleteVendorSignupCount());
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
        // Static reference data for the client-side business-type/operating-
        // area filters below the Vendors table - not a backend call, so
        // these populate even if the backend itself is unreachable.
        model.addAttribute("businessTypes", BusinessType.displayOrder());
        model.addAttribute("operatingAreaOptions", PhilippineProvinces.OPERATING_AREA_OPTIONS);
        String jwt = AdminSession.token(session);
        if (jwt == null) {
            model.addAttribute("backendUnavailable", true);
            model.addAttribute("vendors", List.of());
            model.addAttribute("testVendors", List.of());
            model.addAttribute("incompleteSignups", List.of());
            return "admin/vendors";
        }
        try {
            model.addAttribute("backendUnavailable", false);
            // Test/fake accounts (see User.fakeAccount, AdminVendorImpersonation
            // work) get their own tab rather than mixing into the real Vendors
            // list - admins reviewing real vendors shouldn't have to mentally
            // filter out seeded test data.
            Map<Boolean, List<BackendAdminVendorListItem>> partitioned = backendClient.listVendorsForAdmin(jwt).stream()
                    .collect(Collectors.partitioningBy(BackendAdminVendorListItem::fakeAccount));
            model.addAttribute("vendors", partitioned.get(false));
            model.addAttribute("testVendors", partitioned.get(true));
            model.addAttribute("incompleteSignups", backendClient.listIncompleteVendorSignups(jwt));
        } catch (BackendApiException e) {
            model.addAttribute("backendUnavailable", true);
            model.addAttribute("vendors", List.of());
            model.addAttribute("testVendors", List.of());
            model.addAttribute("incompleteSignups", List.of());
        }
        return "admin/vendors";
    }

    /**
     * "View Dashboard" - opens this vendor's real dashboard for the admin,
     * without that vendor's own Google login (see AdminVendorController
     * #impersonate on the backend). Stores a completely normal vendor
     * WebSession, plus one extra marker so the vendor-shell can show a
     * "Viewing as ... - Exit" banner (see fragments/common.html) - the admin's
     * own AdminSession is untouched throughout, so exiting is a plain
     * WebSession clear, not a re-login.
     */
    @PostMapping("/vendors/{userId}/view-dashboard")
    public String viewVendorDashboard(@PathVariable Long userId, HttpSession session, RedirectAttributes redirectAttributes) {
        String jwt = AdminSession.token(session);
        if (jwt == null) {
            redirectAttributes.addFlashAttribute("vendorsError", "Not connected to eventsrus-backend right now - log out and back in to retry.");
            return "redirect:/admin/vendors";
        }
        try {
            BackendAuthResponse auth = backendClient.impersonateVendor(jwt, userId, AdminSession.username(session));
            WebSession.store(session, auth);
            WebSession.markImpersonating(session);
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("vendorsError", e.getMessage());
            return "redirect:/admin/vendors";
        }
        return "redirect:/vendor/dashboard";
    }

    @PostMapping("/vendors/exit-impersonation")
    public String exitImpersonation(HttpSession session) {
        WebSession.clear(session);
        return "redirect:/admin/vendors";
    }

    @GetMapping("/admins")
    public String admins(HttpSession session, Model model) {
        model.addAttribute("activePage", "admins");
        String jwt = AdminSession.token(session);
        if (jwt == null) {
            model.addAttribute("backendUnavailable", true);
            model.addAttribute("admins", List.of());
            return "admin/admins";
        }
        try {
            model.addAttribute("backendUnavailable", false);
            model.addAttribute("admins", backendClient.listAdminAccounts(jwt));
        } catch (BackendApiException e) {
            model.addAttribute("backendUnavailable", true);
            model.addAttribute("admins", List.of());
        }
        return "admin/admins";
    }

    @PostMapping("/admins")
    public String createAdmin(
            @RequestParam String username, @RequestParam String password,
            HttpSession session, RedirectAttributes redirectAttributes) {
        String jwt = AdminSession.token(session);
        if (jwt == null) {
            redirectAttributes.addFlashAttribute("adminsError", "Not connected to eventsrus-backend right now.");
            return "redirect:/admin/admins";
        }
        String trimmedUsername = username == null ? "" : username.trim();
        if (trimmedUsername.isBlank() || password == null || password.length() < 8) {
            redirectAttributes.addFlashAttribute(
                    "adminsError", "Username is required and password must be at least 8 characters.");
            return "redirect:/admin/admins";
        }
        try {
            backendClient.createAdminAccount(jwt, trimmedUsername, password);
            redirectAttributes.addFlashAttribute("adminCreated", true);
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("adminsError", e.getMessage());
        }
        return "redirect:/admin/admins";
    }

    @PostMapping("/admins/password")
    public String changePassword(
            @RequestParam String currentPassword, @RequestParam String newPassword,
            HttpSession session, RedirectAttributes redirectAttributes) {
        String jwt = AdminSession.token(session);
        if (jwt == null) {
            redirectAttributes.addFlashAttribute("passwordError", "Not connected to eventsrus-backend right now.");
            return "redirect:/admin/admins";
        }
        if (newPassword == null || newPassword.length() < 8) {
            redirectAttributes.addFlashAttribute("passwordError", "New password must be at least 8 characters.");
            return "redirect:/admin/admins";
        }
        try {
            backendClient.changeAdminPassword(jwt, currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("passwordChanged", true);
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("passwordError", e.getMessage());
        }
        return "redirect:/admin/admins";
    }

    @GetMapping("/settings")
    public String settings(HttpSession session, Model model) {
        model.addAttribute("activePage", "settings");
        String jwt = AdminSession.token(session);
        if (jwt == null) {
            model.addAttribute("backendUnavailable", true);
            model.addAttribute("settings", List.of());
            return "admin/settings";
        }
        try {
            model.addAttribute("backendUnavailable", false);
            model.addAttribute("settings", backendClient.listSystemSettings(jwt));
        } catch (BackendApiException e) {
            model.addAttribute("backendUnavailable", true);
            model.addAttribute("settings", List.of());
        }
        return "admin/settings";
    }

    @PostMapping("/settings/{key}")
    public String updateSetting(
            @PathVariable String key, @RequestParam String value,
            HttpSession session, RedirectAttributes redirectAttributes) {
        String jwt = AdminSession.token(session);
        if (jwt == null) {
            redirectAttributes.addFlashAttribute("settingsError", "Not connected to eventsrus-backend right now.");
            return "redirect:/admin/settings";
        }
        try {
            backendClient.updateSystemSetting(jwt, key, value);
            redirectAttributes.addFlashAttribute("settingUpdated", true);
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("settingsError", e.getMessage());
        }
        return "redirect:/admin/settings";
    }
}
