package com.web.eventsrus.controller;

import com.web.eventsrus.admin.AdminSession;
import com.web.eventsrus.backend.BackendApiException;
import com.web.eventsrus.backend.BackendClient;
import com.web.eventsrus.backend.BackendSubscriptionPayment;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.function.Predicate;
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
 * The admin module's GCash payment-verification queue - a vendor who paid
 * via GCash (see fragments/common.html :: gcashPaymentModal) shows up here
 * until an admin verifies or rejects the screenshot they uploaded. Same
 * shape as AdminVerificationController's own review queue.
 */
@Controller
@RequestMapping("/admin/subscription-payments")
@RequiredArgsConstructor
public class AdminSubscriptionPaymentController {

    private final BackendClient backendClient;

    @GetMapping
    public String list(HttpSession session, Model model) {
        model.addAttribute("activePage", "subscription-payments");
        String jwt = AdminSession.token(session);
        if (jwt == null) {
            model.addAttribute("backendUnavailable", true);
            addEmptyTabs(model);
            return "admin/subscription-payments";
        }
        try {
            List<BackendSubscriptionPayment> payments = backendClient.listSubscriptionPayments(jwt);
            model.addAttribute("pendingPayments", byStatus(payments, "PAYMENT_VERIFICATION"::equals));
            model.addAttribute("rejectedPayments", byStatus(payments, "PAYMENT_REJECTED"::equals));
            model.addAttribute("verifiedPayments", byStatus(payments,
                    status -> !"PAYMENT_VERIFICATION".equals(status) && !"PAYMENT_REJECTED".equals(status)));
        } catch (BackendApiException e) {
            model.addAttribute("backendUnavailable", true);
            addEmptyTabs(model);
        }
        return "admin/subscription-payments";
    }

    private List<BackendSubscriptionPayment> byStatus(
            List<BackendSubscriptionPayment> payments, Predicate<String> statusMatches) {
        return payments.stream().filter(p -> statusMatches.test(p.status())).collect(Collectors.toList());
    }

    private void addEmptyTabs(Model model) {
        model.addAttribute("pendingPayments", List.of());
        model.addAttribute("verifiedPayments", List.of());
        model.addAttribute("rejectedPayments", List.of());
    }

    @PostMapping("/{userId}/verify")
    public String verify(@PathVariable Long userId, HttpSession session, RedirectAttributes redirectAttributes) {
        String jwt = requireBackendToken(session, redirectAttributes);
        if (jwt == null) {
            return "redirect:/admin/subscription-payments";
        }
        try {
            backendClient.verifySubscriptionPayment(jwt, userId);
            redirectAttributes.addFlashAttribute("paymentVerified", true);
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("paymentError", e.getMessage());
        }
        return "redirect:/admin/subscription-payments";
    }

    @PostMapping("/{userId}/reject")
    public String reject(
            @PathVariable Long userId, @RequestParam String reason,
            HttpSession session, RedirectAttributes redirectAttributes) {
        String jwt = requireBackendToken(session, redirectAttributes);
        if (jwt == null) {
            return "redirect:/admin/subscription-payments";
        }
        try {
            backendClient.rejectSubscriptionPayment(jwt, userId, reason);
            redirectAttributes.addFlashAttribute("paymentRejected", true);
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("paymentError", e.getMessage());
        }
        return "redirect:/admin/subscription-payments";
    }

    private String requireBackendToken(HttpSession session, RedirectAttributes redirectAttributes) {
        String jwt = AdminSession.token(session);
        if (jwt == null) {
            redirectAttributes.addFlashAttribute(
                    "paymentError", "Not connected to eventsrus-backend - log out and back in to retry.");
        }
        return jwt;
    }
}
