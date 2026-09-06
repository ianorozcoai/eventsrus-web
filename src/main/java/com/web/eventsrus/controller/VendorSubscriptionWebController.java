package com.web.eventsrus.controller;

import com.web.eventsrus.backend.BackendApiException;
import com.web.eventsrus.backend.BackendClient;
import com.web.eventsrus.backend.BackendCreateSubscriptionResponse;
import com.web.eventsrus.backend.BackendSubscriptionStatus;
import com.web.eventsrus.backend.WebSession;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The vendor-facing plan status, renewal, and billing history page - the
 * one page in eventsrus-web backed entirely by real eventsrus-backend data
 * (via BackendClient), gated by WebMvcConfig's login requirement.
 */
@Controller
@RequestMapping("/vendor/subscription")
@RequiredArgsConstructor
public class VendorSubscriptionWebController {

    private final BackendClient backendClient;

    @GetMapping
    public String view(HttpSession session, Model model) {
        String jwt = WebSession.token(session);
        BackendSubscriptionStatus status = backendClient.getSubscriptionStatus(jwt);
        WebSession.storeSubscription(session, status.plan(), status.expiresAt(), status.expiringSoon(), status.expired());

        model.addAttribute("status", status);
        model.addAttribute("history", backendClient.getBillingHistory(jwt));
        model.addAttribute("firstName", session.getAttribute(WebSession.FIRST_NAME));
        return "vendor/subscription";
    }

    @PostMapping("/subscribe")
    public String subscribe(
            @RequestParam String billingCycle, HttpSession session, RedirectAttributes redirectAttributes) {
        String jwt = WebSession.token(session);
        try {
            BackendCreateSubscriptionResponse created = backendClient.createSubscription(jwt, "PRO", billingCycle);
            return "redirect:" + created.approvalUrl();
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("subscriptionError", e.getMessage());
            return "redirect:/vendor/subscription";
        }
    }
}
