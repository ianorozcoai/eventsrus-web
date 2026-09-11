package com.web.eventsrus.controller;

import com.web.eventsrus.backend.WebSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * "/" is the app's one entry point for a brand-new visitor: a role-picker
 * ("I'm a Planner" / "I'm a Vendor", see templates/landing.html) leading
 * into the real Google login at /planner/ or /vendor/ (see
 * AuthWebController). A visitor who's already logged in skips the picker
 * entirely and goes straight to their dashboard - this page has nothing to
 * offer them a second time.
 */
@Controller
public class NavController {

    @GetMapping("/")
    public String index(HttpSession session) {
        if (WebSession.isLoggedIn(session)) {
            // signupIntent, not role: someone mid-vendor-onboarding still
            // has role=PLANNER (that only flips once onboarding finishes),
            // but they're locked to the vendor identity - send them back to
            // finish onboarding, not into the planner dashboard as if
            // they'd never started. See WebMvcConfig for the same rule
            // applied to /planner/** directly.
            if ("VENDOR".equals(WebSession.signupIntent(session))) {
                return "redirect:" + ("VENDOR".equals(WebSession.role(session)) ? "/vendor/dashboard" : "/vendor/onboarding");
            }
            return "redirect:/planner/dashboard";
        }
        return "landing";
    }

    @GetMapping("/terms")
    public String terms() {
        return "terms";
    }

    // PlannerController owns the real page logic at /planner/events (class-level
    // @RequestMapping there, so it can't itself expose a plain /planner/dashboard
    // path) - this just gives the literal URL asked for in the login flow.
    @GetMapping("/planner/dashboard")
    public String plannerDashboard() {
        return "forward:/planner/events";
    }
}
