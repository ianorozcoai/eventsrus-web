package com.web.eventsrus.controller;

import com.web.eventsrus.backend.BackendApiException;
import com.web.eventsrus.backend.BackendClient;
import com.web.eventsrus.backend.WebSession;
import jakarta.servlet.http.HttpSession;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

/**
 * The vendor-facing "Connect Google Calendar" flow (see vendor/calendar.html)
 * - a genuinely separate OAuth dance from Google Sign-In (AuthWebController):
 * Sign-In is a client-side ID-token exchange with no server redirect at all,
 * while this needs a real three-legged authorization-code flow (the vendor
 * has to see and approve Google's consent screen for the calendar.events
 * scope) that ends with Google redirecting the BROWSER back to callback()
 * below. Lives here, not in eventsrus-backend, because this app already
 * owns the vendor's session/identity and the redirect-back-to-a-real-page
 * UX - the backend's GoogleCalendarController is purely the data layer this
 * flow calls once it already has a refresh token in hand.
 *
 * access_type=offline + prompt=consent on the authorize URL are both
 * required to reliably get a refresh_token back on every connection, not
 * just a vendor's very first-ever consent - Google only issues one
 * otherwise. The "state" param is a random per-attempt nonce stashed in the
 * session and checked on callback, standard OAuth CSRF protection (without
 * it, an attacker could trick a victim's browser into completing an OAuth
 * flow using the attacker's own authorization code).
 */
@Controller
public class GoogleCalendarOAuthController {

    private static final String SCOPE = "https://www.googleapis.com/auth/calendar.events";
    private static final String AUTHORIZE_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String STATE_SESSION_KEY = "googleCalendar.oauthState";

    private final BackendClient backendClient;
    private final RestClient googleRestClient = RestClient.create();

    @Value("${google.calendar.client-id}")
    private String clientId;

    @Value("${google.calendar.client-secret}")
    private String clientSecret;

    @Value("${google.calendar.redirect-uri}")
    private String redirectUri;

    public GoogleCalendarOAuthController(BackendClient backendClient) {
        this.backendClient = backendClient;
    }

    @GetMapping("/vendor/settings/google-calendar/connect")
    public RedirectView connect(HttpSession session) {
        String state = generateState();
        session.setAttribute(STATE_SESSION_KEY, state);

        String url = AUTHORIZE_URL
                + "?client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(redirectUri)
                + "&response_type=code"
                + "&scope=" + encode(SCOPE)
                + "&access_type=offline"
                + "&prompt=consent"
                + "&state=" + encode(state);
        return new RedirectView(url);
    }

    @GetMapping("/vendor/settings/google-calendar/callback")
    public String callback(
            @RequestParam(required = false) String code, @RequestParam(required = false) String state,
            @RequestParam(required = false) String error, HttpSession session, RedirectAttributes redirectAttributes) {
        Object expectedState = session.getAttribute(STATE_SESSION_KEY);
        session.removeAttribute(STATE_SESSION_KEY);

        if (error != null) {
            redirectAttributes.addFlashAttribute("googleCalendarError", "Google Calendar wasn't connected: " + error);
            return "redirect:/vendor/calendar";
        }
        if (code == null || expectedState == null || !expectedState.equals(state)) {
            redirectAttributes.addFlashAttribute("googleCalendarError", "Could not verify the Google Calendar connection request. Please try again.");
            return "redirect:/vendor/calendar";
        }

        try {
            String refreshToken = exchangeCodeForRefreshToken(code);
            // "primary" is Google's own alias for the authenticated user's
            // primary calendar - no separate calendars.list call needed to
            // discover a real id.
            backendClient.saveGoogleCalendarConnection(WebSession.token(session), refreshToken, "primary");
            redirectAttributes.addFlashAttribute("googleCalendarConnected", true);
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("googleCalendarError", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("googleCalendarError", "Could not connect Google Calendar. Please try again.");
        }
        return "redirect:/vendor/calendar";
    }

    @PostMapping("/vendor/settings/google-calendar/disconnect")
    public String disconnect(HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            backendClient.disconnectGoogleCalendar(WebSession.token(session));
            redirectAttributes.addFlashAttribute("googleCalendarDisconnected", true);
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("googleCalendarError", e.getMessage());
        }
        return "redirect:/vendor/calendar";
    }

    private String exchangeCodeForRefreshToken(String code) {
        Map<String, Object> response = googleRestClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("code=" + encode(code)
                        + "&client_id=" + encode(clientId)
                        + "&client_secret=" + encode(clientSecret)
                        + "&redirect_uri=" + encode(redirectUri)
                        + "&grant_type=authorization_code")
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                });
        if (response == null || response.get("refresh_token") == null) {
            // Most common real cause: the vendor had already connected once
            // before and this consent didn't actually re-prompt (shouldn't
            // happen with prompt=consent above, but Google's behavior here
            // isn't 100% guaranteed) - Google only returns a refresh_token
            // when consent is genuinely (re-)granted.
            throw new IllegalStateException("Google did not return a refresh token - please try connecting again");
        }
        return (String) response.get("refresh_token");
    }

    private String generateState() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
