package com.web.eventsrus.controller;

import com.web.eventsrus.admin.AdminSession;
import com.web.eventsrus.backend.BackendAdminVendorListItem;
import com.web.eventsrus.backend.BackendApiException;
import com.web.eventsrus.backend.BackendClient;
import com.web.eventsrus.backend.BackendVendorVerificationDocuments;
import jakarta.servlet.http.HttpSession;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Vendor identity/document verification review queue - the first real-backend
 * page in the admin module (see AdminSession#token / AdminAuthController#login
 * for how an admin session gets a real ADMIN-role JWT via the internal
 * bridge). Uploading documents at onboarding no longer implies a vendor is
 * verified - an admin has to actually look at what was submitted here and
 * click Verify before the storefront's "Verified Vendor" badge appears.
 *
 * Every method bails to a friendly "backend unavailable" notice rather than
 * a stack trace when the bridge didn't produce a token (backend
 * unreachable at login time, or the shared key isn't configured on this
 * environment yet) - the rest of the admin module is local-only and
 * shouldn't be blocked by that.
 */
@Controller
@RequestMapping("/admin/verifications")
@RequiredArgsConstructor
public class AdminVerificationController {

    private final BackendClient backendClient;

    @GetMapping
    public String list(HttpSession session, Model model) {
        model.addAttribute("activePage", "verifications");
        String jwt = AdminSession.token(session);
        if (jwt == null) {
            model.addAttribute("backendUnavailable", true);
            model.addAttribute("vendors", List.of());
            return "admin/verifications";
        }
        try {
            // Unverified first (newest submission first within that group) -
            // that's the actual review queue; verified vendors sink to the
            // bottom as a reference list rather than needing a second tab.
            List<BackendAdminVendorListItem> vendors = backendClient.listVendorsForAdmin(jwt).stream()
                    .sorted(Comparator.comparing(BackendAdminVendorListItem::verified)
                            .thenComparing(BackendAdminVendorListItem::createdAt, Comparator.reverseOrder()))
                    .toList();
            model.addAttribute("vendors", vendors);
        } catch (BackendApiException e) {
            model.addAttribute("backendUnavailable", true);
            model.addAttribute("vendors", List.of());
        }
        return "admin/verifications";
    }

    @GetMapping("/{userId}")
    public String detail(@PathVariable Long userId, HttpSession session, Model model) {
        model.addAttribute("activePage", "verifications");
        model.addAttribute("userId", userId);
        String jwt = AdminSession.token(session);
        if (jwt == null) {
            model.addAttribute("backendUnavailable", true);
            return "admin/verification-detail";
        }
        try {
            model.addAttribute("documents", backendClient.getVendorVerificationDocuments(jwt, userId));
        } catch (BackendApiException e) {
            model.addAttribute("loadError", e.getMessage());
        }
        return "admin/verification-detail";
    }

    @PostMapping("/{userId}/verify")
    public String verify(@PathVariable Long userId, HttpSession session, RedirectAttributes redirectAttributes) {
        String jwt = requireBackendToken(session, redirectAttributes);
        if (jwt == null) {
            return "redirect:/admin/verifications/" + userId;
        }
        try {
            backendClient.verifyVendor(jwt, userId, AdminSession.username(session));
            redirectAttributes.addFlashAttribute("vendorVerified", true);
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("verificationError", e.getMessage());
        }
        return "redirect:/admin/verifications/" + userId;
    }

    @PostMapping("/{userId}/unverify")
    public String unverify(@PathVariable Long userId, HttpSession session, RedirectAttributes redirectAttributes) {
        String jwt = requireBackendToken(session, redirectAttributes);
        if (jwt == null) {
            return "redirect:/admin/verifications/" + userId;
        }
        try {
            backendClient.unverifyVendor(jwt, userId);
            redirectAttributes.addFlashAttribute("vendorUnverified", true);
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("verificationError", e.getMessage());
        }
        return "redirect:/admin/verifications/" + userId;
    }

    private String requireBackendToken(HttpSession session, RedirectAttributes redirectAttributes) {
        String jwt = AdminSession.token(session);
        if (jwt == null) {
            redirectAttributes.addFlashAttribute(
                    "verificationError", "Not connected to eventsrus-backend - log out and back in to retry.");
        }
        return jwt;
    }
}
