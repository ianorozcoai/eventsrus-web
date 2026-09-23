package com.web.eventsrus.config;

import com.web.eventsrus.backend.BackendApiException;
import com.web.eventsrus.backend.BackendClient;
import com.web.eventsrus.backend.BackendSubscriptionStatus;
import com.web.eventsrus.backend.WebSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Exposes the logged-in vendor's live subscription state to every view as
 * plain model attributes (paywallExpiringSoon/paywallExpired/paywallInGracePeriod/
 * paywallGraceEndsAt/paywallAlreadyShown/paywallRequiresPlanSelection/
 * paywallAwaitingGcashVerification/paywallPaymentFailed/showProWelcomeModal/
 * status/paypalClientId), so
 * fragments/common.html's paywall modal fragments (including the PayPal JS
 * SDK Smart Buttons picker, proPlanPickerForm) can be a drop-in include on
 * any vendor-shell page without their controller needing to know about it.
 *
 * Fetches fresh from eventsrus-backend on every request for a logged-in
 * vendor (rather than trusting whatever WebSession already has cached) -
 * deliberately not relying on a page controller to have refreshed the
 * session first. Spring runs @ModelAttribute advice methods like this one
 * BEFORE the actual handler method, so if this class had instead read
 * session state a controller was expected to populate, it would always see
 * last-login's stale snapshot on the very first page view of a session (the
 * dashboard, right after onboarding - exactly the case that matters most
 * here) and only catch up from the second page view onward. Still writes
 * the fresh result back into the session (WebSession.storeSubscription) so
 * other code that reads it directly (WebSession.isSubscriptionExpired, etc.)
 * stays consistent too.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class PaywallModelAttributes {

    private final BackendClient backendClient;

    @Value("${paypal.client-id}")
    private String paypalClientId;

    @ModelAttribute
    public void addPaywallState(HttpServletRequest request, Model model) {
        // Most pages in this app have no session at all (login is scoped only
        // to /vendor/subscription) - default everything to false rather than
        // leaving these unset, or Thymeleaf's "not paywallAlreadyShown" on a
        // missing model attribute blows up with a null-to-boolean SpEL error.
        boolean expiringSoon = false;
        boolean expired = false;
        boolean inGracePeriod = false;
        java.time.Instant graceEndsAt = null;
        boolean alreadyShown = false;
        boolean requiresPlanSelection = false;
        boolean awaitingGcashVerification = false;
        boolean paymentFailed = false;
        boolean showProWelcomeModal = false;

        HttpSession session = request.getSession(false);
        // Only a real vendor has a subscription concept at all - a planner
        // or an admin session (or no session) skips the backend call
        // entirely, both for correctness (that JWT isn't authorized for
        // /api/v1/vendors/** anyway) and to avoid a wasted request on every
        // non-vendor page.
        if (session != null && "VENDOR".equals(WebSession.role(session))) {
            alreadyShown = Boolean.TRUE.equals(session.getAttribute(WebSession.PAYWALL_SHOWN));

            BackendSubscriptionStatus status = fetchStatus(WebSession.token(session));
            if (status != null) {
                expiringSoon = status.expiringSoon();
                expired = status.expired();
                inGracePeriod = status.inGracePeriod();
                graceEndsAt = status.graceEndsAt();

                // A GCash submission still under review holds a real
                // temporary period (see VendorSubscriptionService#
                // submitGcashPayment), so plan() is already non-null - a
                // dismissible informational notice, not a forced choice,
                // since the vendor has already done their part. A rejected
                // submission has its period cleared back to null, so it
                // falls through to requiresPlanSelection below instead.
                // gcashAwaitingVerification comes straight from the backend
                // (the real subscription row's status), not a date-math
                // proxy computed here.
                awaitingGcashVerification = status.gcashAwaitingVerification();
                // A rejected GCash submission - shown as its own distinct
                // "payment verification failed" notice (with the admin's
                // reason and a link back to /vendor/subscription to
                // re-upload), never the forced plan-selection picker below:
                // this vendor already chose a plan and tried to pay, that
                // choice just didn't go through.
                paymentFailed = status.gcashRejected();
                // No subscription row exists at all yet (never subscribed,
                // still APPROVAL_PENDING - see VendorPlanService#
                // getEffectivePlan) - excludes a rejected GCash submission
                // (paymentFailed above), which also has plan()==null at this
                // point but gets its own notice instead.
                requiresPlanSelection = status.plan() == null && status.expiresAt() == null
                        && !expiringSoon && !expired && !inGracePeriod && !paymentFailed;
                showProWelcomeModal = status.showWelcomePopup();

                WebSession.storeSubscription(session, status.plan(), status.expiresAt(), expiringSoon, expired,
                        inGracePeriod, graceEndsAt, status.billingSource());
                model.addAttribute("status", status);
            }

            boolean onSubscriptionPage = request.getRequestURI().startsWith(request.getContextPath() + "/vendor/subscription");
            if ((expiringSoon || expired || inGracePeriod || requiresPlanSelection || awaitingGcashVerification || paymentFailed)
                    && !alreadyShown && !onSubscriptionPage) {
                session.setAttribute(WebSession.PAYWALL_SHOWN, true);
            }
        }

        model.addAttribute("paywallExpiringSoon", expiringSoon);
        model.addAttribute("paywallExpired", expired);
        model.addAttribute("paywallInGracePeriod", inGracePeriod);
        model.addAttribute("paywallGraceEndsAt", graceEndsAt);
        model.addAttribute("paywallAlreadyShown", alreadyShown);
        model.addAttribute("paywallRequiresPlanSelection", requiresPlanSelection);
        model.addAttribute("paywallAwaitingGcashVerification", awaitingGcashVerification);
        model.addAttribute("paywallPaymentFailed", paymentFailed);
        model.addAttribute("showProWelcomeModal", showProWelcomeModal);
        model.addAttribute("paypalClientId", paypalClientId);
    }

    /** Never lets a backend hiccup break rendering of the page itself - just skips the paywall state for this request. */
    private BackendSubscriptionStatus fetchStatus(String jwt) {
        try {
            return backendClient.getSubscriptionStatus(jwt);
        } catch (BackendApiException e) {
            return null;
        }
    }
}
