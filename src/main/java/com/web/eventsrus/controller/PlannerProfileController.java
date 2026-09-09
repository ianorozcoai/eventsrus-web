package com.web.eventsrus.controller;

import com.web.eventsrus.backend.BackendApiException;
import com.web.eventsrus.backend.BackendAuthResponse;
import com.web.eventsrus.backend.BackendClient;
import com.web.eventsrus.backend.WebSession;
import com.web.eventsrus.model.PhilippineProvinces;
import com.web.eventsrus.model.PlannerProfileForm;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The planner's own Profile page - reached from the Account dropdown in
 * fragments/planner-shell.html. A sibling of PlannerController's own
 * /planner/events (kept as a separate controller/class-level mapping
 * rather than folded into PlannerController, since that one's mapped at
 * "/planner/events" and Spring MVC concatenates class- and method-level
 * mappings - a method here can't "escape" back up to a /planner/profile
 * sibling path from inside that class). Real GET/PUT /api/v1/users/me/profile
 * now (UserController) - PlannerProfileForm's fields already matched that
 * DTO field-for-field even back when this was stub-only.
 */
@Controller
public class PlannerProfileController {

    private final BackendClient backendClient;

    public PlannerProfileController(BackendClient backendClient) {
        this.backendClient = backendClient;
    }

    @GetMapping("/planner/profile")
    public String profile(HttpSession session, Model model) {
        String jwt = WebSession.token(session);
        if (!model.containsAttribute("plannerProfileForm")) {
            model.addAttribute("plannerProfileForm", backendClient.getProfile(jwt));
        }
        model.addAttribute("provinces", PhilippineProvinces.ALL);
        // Same event list as /planner/events, so the sidebar looks
        // identical no matter which planner page is open - nothing is
        // "active" here since no event is selected on this page.
        model.addAttribute("events", backendClient.listEvents(jwt));
        model.addAttribute("activeEventId", null);
        return "planner/profile";
    }

    @PostMapping("/planner/profile")
    public String updateProfile(
            @ModelAttribute PlannerProfileForm plannerProfileForm, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            BackendAuthResponse response = backendClient.updateProfile(WebSession.token(session), plannerProfileForm);
            // A changed email reissues the JWT (it's the token's own
            // subject) - the session's old token would fail to resolve to
            // any user on the very next request otherwise.
            WebSession.store(session, response);
            redirectAttributes.addFlashAttribute("profileSaved", true);
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("profileError", e.getMessage());
        }
        return "redirect:/planner/profile";
    }
}
