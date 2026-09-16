package com.web.eventsrus.config;

import com.web.eventsrus.backend.BackendApiException;
import com.web.eventsrus.backend.BackendClient;
import com.web.eventsrus.backend.WebSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Exposes "how many need your attention" counts for the vendor sidebar's
 * Leads/Bookings/Messages/Quotations nav items as plain model attributes,
 * so fragments/common.html's sidebar fragment can show a badge on any
 * vendor-shell page without its controller needing to fetch this itself.
 * Same always-safe defaults-to-zero pattern as PaywallModelAttributes -
 * this runs on every request, not just vendor pages, so it stays a no-op
 * (no backend call at all) unless the session is actually a logged-in
 * vendor.
 */
@ControllerAdvice
public class VendorNavBadgeModelAttributes {

    private final BackendClient backendClient;

    public VendorNavBadgeModelAttributes(BackendClient backendClient) {
        this.backendClient = backendClient;
    }

    @ModelAttribute
    public void addNavBadgeCounts(HttpServletRequest request, Model model) {
        long leads = 0;
        long messages = 0;
        long quotations = 0;
        long bookings = 0;
        // Shown in the vendor topnavbar next to the first name (see
        // fragments/common.html) - with dozens of seed/test vendor accounts
        // around, knowing which business you're actually logged in as at a
        // glance is otherwise surprisingly easy to lose track of.
        String vendorBusinessName = null;
        // The profile-dropdown name itself, for a vendor, should read as the
        // storefront's "Owner name" (a business-profile field the vendor
        // sets in Settings, e.g. "Juan Dela Cruz") - not whichever Google
        // account happens to be signed in, which is often a developer/admin
        // account testing a seeded business. AuthModelAttributes#currentUserFullName
        // stays the fallback used for planners (no owner-name concept there).
        String vendorOwnerName = null;

        HttpSession session = request.getSession(false);
        if (session != null && "VENDOR".equals(WebSession.role(session))) {
            try {
                var dashboard = backendClient.getDashboard(WebSession.token(session));
                leads = dashboard.newLeadsCount();
                messages = dashboard.newInquiriesCount();
                // Unseen-since-last-visit (see backend BadgeService), not
                // "needs a response" - a badge here should also reflect the
                // vendor's own actions (e.g. just confirmed a booking
                // themselves) and pending planner-proposed amendments, which
                // a pure open-status count would miss entirely.
                quotations = dashboard.quotationsUnseenCount();
                bookings = dashboard.bookingsUnseenCount();
                var settings = backendClient.getSettings(WebSession.token(session));
                vendorBusinessName = settings.businessName();
                vendorOwnerName = settings.ownerName();
            } catch (BackendApiException e) {
                // A badge count failing to load shouldn't break the page
                // it's decorating - just show no badge (same reasoning as
                // PaywallModelAttributes defaulting to false on no session).
            }
        }

        model.addAttribute("navLeadsCount", leads);
        model.addAttribute("navMessagesCount", messages);
        model.addAttribute("navQuotationsCount", quotations);
        model.addAttribute("navBookingsCount", bookings);
        model.addAttribute("vendorBusinessName", vendorBusinessName);
        model.addAttribute("vendorOwnerName", vendorOwnerName);
    }
}
