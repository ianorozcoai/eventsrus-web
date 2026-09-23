package com.web.eventsrus.controller;

import com.web.eventsrus.backend.BackendApiException;
import com.web.eventsrus.backend.BackendClient;
import com.web.eventsrus.backend.BackendCreateSubscriptionResponse;
import com.web.eventsrus.backend.BackendSubscriptionStatus;
import com.web.eventsrus.backend.WebSession;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The vendor-facing plan status, renewal, and billing history page - the
 * one page in eventsrus-web backed entirely by real eventsrus-backend data
 * (via BackendClient), gated by WebMvcConfig's login requirement.
 */
@Slf4j
@Controller
@RequestMapping("/vendor/subscription")
@RequiredArgsConstructor
public class VendorSubscriptionWebController {

    private final BackendClient backendClient;

    @GetMapping
    public String view(HttpSession session, Model model) {
        String jwt = WebSession.token(session);
        BackendSubscriptionStatus status = backendClient.getSubscriptionStatus(jwt);
        WebSession.storeSubscription(session, status.plan(), status.expiresAt(), status.expiringSoon(), status.expired(),
                status.inGracePeriod(), status.graceEndsAt(), status.billingSource());

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

    /**
     * "Upload Payment Screenshot" on the GCash popup (see
     * fragments/common.html :: gcashPaymentModal) - a plain multipart form
     * post (unlike the PayPal flow, no client-side SDK involved), landing
     * back on the dashboard where the "awaiting verification" notice takes
     * over from the required picker.
     */
    @PostMapping("/gcash-payment")
    public String gcashPayment(
            @RequestParam String billingCycle, @RequestParam MultipartFile screenshot,
            @RequestParam(required = false) String vendorRemarks,
            HttpSession session, RedirectAttributes redirectAttributes) {
        String jwt = WebSession.token(session);
        try {
            BackendSubscriptionStatus status = backendClient.submitGcashPayment(jwt, billingCycle, screenshot, vendorRemarks);
            WebSession.storeSubscription(session, status.plan(), status.expiresAt(), status.expiringSoon(), status.expired(),
                    status.inGracePeriod(), status.graceEndsAt(), status.billingSource());
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("subscriptionError", e.getMessage());
        }
        return "redirect:/vendor/dashboard";
    }

    /** "Got it" on the one-time Welcome to PRO popup - see fragments/common.html :: proWelcomeModal. */
    @PostMapping("/welcome-shown")
    public String welcomeShown(HttpSession session) {
        String jwt = WebSession.token(session);
        try {
            backendClient.markProWelcomeShown(jwt);
        } catch (BackendApiException e) {
            log.warn("Failed to mark pro-welcome-shown - popup will just show again next login", e);
        }
        return "redirect:/vendor/dashboard";
    }

    /**
     * Records a subscription PayPal's JS SDK Smart Buttons already created
     * client-side (see fragments/common.html :: proPlanPickerForm's
     * onApprove) - called via fetch() from that same script, so this
     * returns JSON rather than redirecting; the script itself navigates the
     * browser away once this succeeds.
     */
    @PostMapping("/paypal-approve")
    @ResponseBody
    public PaypalApproveResult paypalApprove(@RequestBody PaypalApproveRequest request, HttpSession session) {
        String jwt = WebSession.token(session);
        try {
            BackendSubscriptionStatus status = backendClient.recordApprovedSubscription(jwt, request.paypalSubscriptionId());
            WebSession.storeSubscription(session, status.plan(), status.expiresAt(), status.expiringSoon(), status.expired(),
                    status.inGracePeriod(), status.graceEndsAt(), status.billingSource());
            return new PaypalApproveResult(true, null);
        } catch (BackendApiException e) {
            return new PaypalApproveResult(false, e.getMessage());
        }
    }

    public record PaypalApproveRequest(String paypalSubscriptionId) {
    }

    public record PaypalApproveResult(boolean success, String error) {
    }
}
