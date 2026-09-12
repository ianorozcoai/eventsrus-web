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

        HttpSession session = request.getSession(false);
        if (session != null && "VENDOR".equals(WebSession.role(session))) {
            try {
                var dashboard = backendClient.getDashboard(WebSession.token(session));
                leads = dashboard.newLeadsCount();
                messages = dashboard.newInquiriesCount();
                quotations = dashboard.newQuotationsCount();
                bookings = dashboard.bookingsNeedingActionCount();
                vendorBusinessName = backendClient.getSettings(WebSession.token(session)).businessName();
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
    }
}
