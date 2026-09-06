package com.web.eventsrus.controller;

import com.web.eventsrus.model.PhilippineProvinces;
import com.web.eventsrus.model.PlannerProfileForm;
import com.web.eventsrus.stub.PlannerEventSessionService;
import com.web.eventsrus.stub.StubDataService;
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
 * sibling path from inside that class).
 * Stub-only for now, same as every other planner-facing form in this app -
 * eventsrus-backend already has the real GET/PUT /api/v1/users/me/profile
 * endpoints (UserController), this just doesn't call them yet.
 */
@Controller
public class PlannerProfileController {

    private final StubDataService stubDataService;
    private final PlannerEventSessionService plannerEventSessionService;

    public PlannerProfileController(
            StubDataService stubDataService, PlannerEventSessionService plannerEventSessionService) {
        this.stubDataService = stubDataService;
        this.plannerEventSessionService = plannerEventSessionService;
    }

    @GetMapping("/planner/profile")
    public String profile(HttpSession session, Model model) {
        if (!model.containsAttribute("plannerProfileForm")) {
            model.addAttribute(
                    "plannerProfileForm",
                    stubDataService.load("planner-profile.json", PlannerProfileForm.class));
        }
        model.addAttribute("provinces", PhilippineProvinces.ALL);
        // Same event list as /planner/events, so the sidebar looks
        // identical no matter which planner page is open - nothing is
        // "active" here since no event is selected on this page.
        model.addAttribute("events", plannerEventSessionService.events(session));
        model.addAttribute("activeEventId", null);
        return "planner/profile";
    }

    @PostMapping("/planner/profile")
    public String updateProfile(
            @ModelAttribute PlannerProfileForm plannerProfileForm, RedirectAttributes redirectAttributes) {
        // Stub only for now - proves the round trip; wires up to
        // PUT /api/v1/users/me/profile on eventsrus-backend later.
        redirectAttributes.addFlashAttribute("profileSaved", true);
        return "redirect:/planner/profile";
    }
}
