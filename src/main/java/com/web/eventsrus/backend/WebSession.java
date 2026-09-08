package com.web.eventsrus.backend;

import jakarta.servlet.http.HttpSession;

/**
 * Session attribute keys for the one real-login flow in eventsrus-web (see
 * AuthWebController). Every other page in this app has no concept of "who's
 * logged in" at all - this is deliberately narrow, not a general auth layer.
 */
public final class WebSession {

    public static final String TOKEN = "auth.token";
    public static final String USER_ID = "auth.userId";
    public static final String EMAIL = "auth.email";
    public static final String FIRST_NAME = "auth.firstName";
    public static final String ROLE = "auth.role";
    public static final String SUBSCRIPTION_PLAN = "auth.subscriptionPlan";
    public static final String SUBSCRIPTION_EXPIRES_AT = "auth.subscriptionExpiresAt";
    public static final String SUBSCRIPTION_EXPIRING_SOON = "auth.subscriptionExpiringSoon";
    public static final String SUBSCRIPTION_EXPIRED = "auth.subscriptionExpired";
    // Shows the paywall modal once per login, not on every page navigation.
    public static final String PAYWALL_SHOWN = "auth.paywallShown";
    // Shows the "add your first package" nudge modal once per login too -
    // otherwise it would pop up on every single dashboard visit for as long
    // as the vendor has zero packages, which gets old fast.
    public static final String FIRST_PACKAGE_NUDGE_SHOWN = "auth.firstPackageNudgeShown";
    // Stashed from /vendor/?ref=CODE at login time, carried through Google
    // sign-in, and attached to the onboarding form if this visitor becomes a
    // vendor - see AuthWebController#vendorLogin and vendor/onboarding.html.
    public static final String REFERRAL_CODE = "auth.referralCode";

    private WebSession() {
    }

    public static boolean isLoggedIn(HttpSession session) {
        return session != null && session.getAttribute(TOKEN) != null;
    }

    public static String token(HttpSession session) {
        return (String) session.getAttribute(TOKEN);
    }

    public static Long userId(HttpSession session) {
        return session == null ? null : (Long) session.getAttribute(USER_ID);
    }

    public static String role(HttpSession session) {
        return session == null ? null : (String) session.getAttribute(ROLE);
    }

    public static String firstName(HttpSession session) {
        return session == null ? null : (String) session.getAttribute(FIRST_NAME);
    }

    public static String email(HttpSession session) {
        return session == null ? null : (String) session.getAttribute(EMAIL);
    }

    public static String referralCode(HttpSession session) {
        return session == null ? null : (String) session.getAttribute(REFERRAL_CODE);
    }

    /** Only sets it if not already stashed - a reload of /vendor/ without ?ref= shouldn't wipe an earlier one. */
    public static void stashReferralCodeIfAbsent(HttpSession session, String code) {
        if (session.getAttribute(REFERRAL_CODE) == null && code != null && !code.isBlank()) {
            session.setAttribute(REFERRAL_CODE, code);
        }
    }

    public static void store(HttpSession session, BackendAuthResponse auth) {
        session.setAttribute(TOKEN, auth.token());
        session.setAttribute(USER_ID, auth.id());
        session.setAttribute(EMAIL, auth.email());
        session.setAttribute(FIRST_NAME, auth.firstName());
        session.setAttribute(ROLE, auth.role());
        storeSubscription(session, auth.plan(), auth.planExpiresAt(), false, auth.plan() == null);
    }

    public static void storeSubscription(
            HttpSession session, String plan, java.time.Instant expiresAt, boolean expiringSoon, boolean expired) {
        session.setAttribute(SUBSCRIPTION_PLAN, plan);
        session.setAttribute(SUBSCRIPTION_EXPIRES_AT, expiresAt);
        session.setAttribute(SUBSCRIPTION_EXPIRING_SOON, expiringSoon);
        session.setAttribute(SUBSCRIPTION_EXPIRED, expired);
    }

    public static boolean isSubscriptionExpired(HttpSession session) {
        if (session == null) {
            // No real login on this page at all (most stub pages) - nothing to gate.
            return false;
        }
        Boolean expired = (Boolean) session.getAttribute(SUBSCRIPTION_EXPIRED);
        return Boolean.TRUE.equals(expired);
    }
}
