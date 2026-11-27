package com.web.eventsrus.controller;

import com.web.eventsrus.backend.WebSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * The 3-page supplier recruitment funnel that Ian sends as one link: /partner/ (hook) -> /partner/opportunity
 * -> /partner/join (register on the website or download the app). The registration itself stays on /vendor/,
 * so these pages carry no sign-in code; they only remember ?ref= / ?promo= exactly like /vendor/ does.
 */
@Controller
public class PartnerFunnelController {

    @Value("${google.oauth.client-id}")
    private String googleClientId;

    @GetMapping({"/partner", "/partner/"})
    public String hook(@RequestParam(required = false) String ref, @RequestParam(required = false) String promo,
            HttpSession session) {
        remember(session, ref, promo);
        return "partner/hook";
    }

    @GetMapping({"/partner/opportunity", "/partner/opportunity/"})
    public String opportunity(@RequestParam(required = false) String ref, @RequestParam(required = false) String promo,
            HttpSession session) {
        remember(session, ref, promo);
        return "partner/opportunity";
    }

    @GetMapping({"/partner/join", "/partner/join/"})
    public String join(@RequestParam(required = false) String ref, @RequestParam(required = false) String promo,
            HttpSession session, Model model) {
        remember(session, ref, promo);
        // Feeds the sign-in pop-up (same one /vendor/ opens).
        model.addAttribute("googleClientId", googleClientId);
        model.addAttribute("intent", "vendor");
        return "partner/join";
    }

    private static void remember(HttpSession session, String ref, String promo) {
        WebSession.stashReferralCodeIfAbsent(session, ref);
        WebSession.stashPromoCode(session, promo);
    }
}
