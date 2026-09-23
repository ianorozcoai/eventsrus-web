package com.web.eventsrus.controller;

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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The admin module's referral oversight page - every referral regardless of
 * status (PENDING/CONVERTED/COMMISSION_PAID), with a "Mark Paid" action on
 * CONVERTED rows once the commission has actually been sent. The backend
 * side of this (VendorReferralService#listAllForAdmin/markPaid,
 * AdminReferralController) already existed before this page did.
 */
@Controller
@RequestMapping("/admin/referrals")
@RequiredArgsConstructor
public class AdminReferralWebController {

    private final BackendClient backendClient;

    @GetMapping
    public String list(HttpSession session, Model model) {
        model.addAttribute("activePage", "referrals");
        String jwt = AdminSession.token(session);
        if (jwt == null) {
            model.addAttribute("backendUnavailable", true);
            model.addAttribute("referrals", List.of());
            return "admin/referrals";
        }
        try {
            model.addAttribute("referrals", backendClient.listReferralsForAdmin(jwt));
        } catch (BackendApiException e) {
            model.addAttribute("backendUnavailable", true);
            model.addAttribute("referrals", List.of());
        }
        return "admin/referrals";
    }

    @PostMapping("/{id}/mark-paid")
    public String markPaid(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks,
            @RequestParam(required = false) MultipartFile proof,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        String jwt = AdminSession.token(session);
        if (jwt == null) {
            redirectAttributes.addFlashAttribute(
                    "referralError", "Not connected to eventsrus-backend - log out and back in to retry.");
            return "redirect:/admin/referrals";
        }
        try {
            backendClient.markReferralPaid(jwt, id, remarks, proof);
            redirectAttributes.addFlashAttribute("referralMarkedPaid", true);
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("referralError", e.getMessage());
        }
        return "redirect:/admin/referrals";
    }
}
