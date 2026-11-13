package com.web.eventsrus.controller;

import com.web.eventsrus.admin.AdminSession;
import com.web.eventsrus.admin.CloudflareTrafficService;
import com.web.eventsrus.backend.BackendApiException;
import com.web.eventsrus.backend.BackendClient;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Admin "Analytics" page: our own sign-up / listing / booking numbers (from eventsrus-backend)
 * next to website visitors (from Cloudflare Web Analytics), with the Philippines called out
 * because that is the only market for now.
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final BackendClient backendClient;
    private final CloudflareTrafficService cloudflare;

    @Value("${analytics.goal.suppliers-with-listing:50}")
    private int goalSuppliers;

    @Value("${analytics.goal.planners:100}")
    private int goalPlanners;

    @GetMapping("/analytics")
    public String analytics(@RequestParam(defaultValue = "30") int days, HttpSession session, Model model) {
        int range = days <= 7 ? 7 : days <= 30 ? 30 : 90;
        model.addAttribute("activePage", "analytics");
        model.addAttribute("days", range);
        model.addAttribute("goalSuppliers", goalSuppliers);
        model.addAttribute("goalPlanners", goalPlanners);
        model.addAttribute("traffic", cloudflare.traffic(range));
        model.addAttribute("beaconInstalled", cloudflare.beaconInstalled());

        String jwt = AdminSession.token(session);
        if (jwt == null) {
            model.addAttribute("backendUnavailable", true);
            return "admin/analytics";
        }
        try {
            var a = backendClient.getAdminAnalytics(jwt, range);
            model.addAttribute("backendUnavailable", false);
            model.addAttribute("a", a);
            long maxSignup = a.signupsByDay().stream().mapToLong(d -> d.planners() + d.suppliers()).max().orElse(0);
            model.addAttribute("maxSignup", Math.max(maxSignup, 1));
            model.addAttribute("suppliersPct", pct(a.totals().suppliersWithListing(), goalSuppliers));
            model.addAttribute("plannersPct", pct(a.totals().planners(), goalPlanners));
        } catch (BackendApiException e) {
            model.addAttribute("backendUnavailable", true);
        }
        return "admin/analytics";
    }

    private static long pct(long value, int goal) {
        return goal <= 0 ? 0 : Math.min(100, value * 100 / goal);
    }
}
