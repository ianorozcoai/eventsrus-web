package com.web.eventsrus.controller;

import com.web.eventsrus.backend.BackendApiException;
import com.web.eventsrus.backend.BackendAuthResponse;
import com.web.eventsrus.backend.BackendClient;
import com.web.eventsrus.backend.WebSession;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * The one real login flow in eventsrus-web - Google Identity Services in
 * the browser produces an ID token, which gets POSTed here and forwarded
 * server-side to eventsrus-backend's real /api/v1/auth/google. Serves both
 * personas: /planner/ and /vendor/ render the same login page with a
 * different "intent", which /login/google uses to decide where a
 * successful sign-in lands (see loginWithGoogle's Javadoc for the branch).
 */
@Controller
@RequiredArgsConstructor
public class AuthWebController {

    private final BackendClient backendClient;

    @Value("${google.oauth.client-id}")
    private String googleClientId;

    @GetMapping({"/planner", "/planner/"})
    public String plannerLogin(HttpSession session, Model model) {
        if (WebSession.isLoggedIn(session)) {
            return "redirect:/planner/dashboard";
        }
        return loginPage(model, "planner");
    }

    @GetMapping({"/vendor", "/vendor/"})
    public String vendorLogin(
            @RequestParam(required = false) String ref, HttpSession session, Model model) {
        // A referral link (see VendorReferralService) looks like
        // /vendor/?ref=CODE - stash it now so it survives the Google
        // sign-in redirect and reaches the onboarding form afterward.
        WebSession.stashReferralCodeIfAbsent(session, ref);
        if (WebSession.isLoggedIn(session)) {
            return "redirect:" + ("VENDOR".equals(WebSession.role(session)) ? "/vendor/dashboard" : "/vendor/onboarding");
        }
        return loginPage(model, "vendor");
    }

    private String loginPage(Model model, String intent) {
        model.addAttribute("googleClientId", googleClientId);
        model.addAttribute("intent", intent);
        return "login";
    }

    /**
     * Called by the browser's Google Sign-In callback with the ID token it
     * received, plus which entry page (intent) it was submitted from - the
     * backend itself has no notion of "intent", so this is purely a web-layer
     * routing decision:
     *   intent=planner -> always /planner/dashboard (any role may plan events)
     *   intent=vendor  -> role==VENDOR means "already onboarded" -> dashboard;
     *                     otherwise (a brand-new or planner-only account) ->
     *                     /vendor/onboarding, since role only ever becomes
     *                     VENDOR inside a successful becomeVendor call.
     */
    @PostMapping("/login/google")
    @ResponseBody
    public LoginResult loginWithGoogle(
            @RequestParam("idToken") String idToken, @RequestParam("intent") String intent, HttpSession session) {
        try {
            BackendAuthResponse auth = backendClient.loginWithGoogle(idToken);
            WebSession.store(session, auth);
            String redirectTo = "planner".equals(intent)
                    ? "/planner/dashboard"
                    : ("VENDOR".equals(auth.role()) ? "/vendor/dashboard" : "/vendor/onboarding");
            return new LoginResult(true, null, redirectTo);
        } catch (BackendApiException e) {
            return new LoginResult(false, e.getMessage(), null);
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    public record LoginResult(boolean success, String error, String redirectTo) {
    }
}
