package com.web.eventsrus.controller;

import com.web.eventsrus.backend.BackendClient;
import com.web.eventsrus.backend.WebSession;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** The vendor-facing referral program page - real eventsrus-backend data (see BackendClient#getReferralOverview). */
@Controller
@RequestMapping("/vendor/referrals")
@RequiredArgsConstructor
public class VendorReferralWebController {

    private final BackendClient backendClient;

    @GetMapping
    public String overview(HttpSession session, Model model) {
        model.addAttribute("overview", backendClient.getReferralOverview(WebSession.token(session)));
        model.addAttribute("activePage", "referrals");
        return "vendor/referrals";
    }
}
