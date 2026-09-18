package com.web.eventsrus.config;

import com.web.eventsrus.backend.WebSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Exposes the logged-in vendor's subscription-warning state to every view as
 * plain model attributes (paywallExpiringSoon/paywallExpired/paywallInGracePeriod/
 * paywallGraceEndsAt/paywallAlreadyShown), so fragments/common.html's paywall
 * modal fragment can be a drop-in include on any vendor-shell page without
 * its controller needing to know about it.
 *
 * Flips WebSession.PAYWALL_SHOWN the first time the modal would actually be
 * shown, so it appears once per login - except on /vendor/subscription
 * itself (where AuthWebController sends every fresh login), since showing
 * the popup on the page that already IS the renewal screen would just
 * "use up" the one-time flag before the vendor ever sees it anywhere else.
 */
@ControllerAdvice
public class PaywallModelAttributes {

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

        HttpSession session = request.getSession(false);
        if (session != null) {
            expiringSoon = Boolean.TRUE.equals(session.getAttribute(WebSession.SUBSCRIPTION_EXPIRING_SOON));
            expired = Boolean.TRUE.equals(session.getAttribute(WebSession.SUBSCRIPTION_EXPIRED));
            inGracePeriod = Boolean.TRUE.equals(session.getAttribute(WebSession.SUBSCRIPTION_IN_GRACE_PERIOD));
            graceEndsAt = (java.time.Instant) session.getAttribute(WebSession.SUBSCRIPTION_GRACE_ENDS_AT);
            alreadyShown = Boolean.TRUE.equals(session.getAttribute(WebSession.PAYWALL_SHOWN));

            boolean onSubscriptionPage = request.getRequestURI().startsWith(request.getContextPath() + "/vendor/subscription");
            if ((expiringSoon || expired || inGracePeriod) && !alreadyShown && !onSubscriptionPage) {
                session.setAttribute(WebSession.PAYWALL_SHOWN, true);
            }
        }

        model.addAttribute("paywallExpiringSoon", expiringSoon);
        model.addAttribute("paywallExpired", expired);
        model.addAttribute("paywallInGracePeriod", inGracePeriod);
        model.addAttribute("paywallGraceEndsAt", graceEndsAt);
        model.addAttribute("paywallAlreadyShown", alreadyShown);
    }
}
