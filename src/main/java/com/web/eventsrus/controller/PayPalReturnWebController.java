package com.web.eventsrus.controller;

import com.web.eventsrus.backend.BackendApiException;
import com.web.eventsrus.backend.BackendClient;
import com.web.eventsrus.backend.BackendSubscriptionStatus;
import com.web.eventsrus.backend.WebSession;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * eventsrus-backend's VendorSubscriptionService builds PayPal's return/
 * cancel URLs itself as {app.frontend-base-url}/subscription/return and
 * /subscription/plans - a plain top-level controller (not nested under
 * VendorSubscriptionWebController's /vendor/subscription) so these paths
 * match exactly what the backend already constructs, without changing that
 * contract. app.frontend-base-url must point at this app (port 8081) for
 * these to actually get hit - see application-local.properties.
 */
@Controller
@RequiredArgsConstructor
public class PayPalReturnWebController {

    private final BackendClient backendClient;

    @GetMapping("/subscription/return")
    public String confirm(
            @RequestParam("vendorSubscriptionId") Long vendorSubscriptionId,
            HttpSession session, RedirectAttributes redirectAttributes) {
        if (!WebSession.isLoggedIn(session)) {
            return "redirect:/login";
        }
        String jwt = WebSession.token(session);
        try {
            BackendSubscriptionStatus status = backendClient.confirmSubscription(jwt, vendorSubscriptionId);
            WebSession.storeSubscription(session, status.plan(), status.expiresAt(), status.expiringSoon(), status.expired());
            redirectAttributes.addFlashAttribute("subscriptionConfirmed", true);
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("subscriptionError", e.getMessage());
        }
        return "redirect:/vendor/subscription";
    }

    /** PayPal sends the vendor here if they cancel out of the approval flow. */
    @GetMapping("/subscription/plans")
    public String cancelled() {
        return "redirect:/vendor/subscription";
    }
}
