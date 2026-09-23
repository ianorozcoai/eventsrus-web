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
    public static final String LAST_NAME = "auth.lastName";
    public static final String ROLE = "auth.role";
    // Which door this account is permanently locked to (planner or vendor) -
    // distinct from ROLE, which still flows PLANNER -> VENDOR once
    // onboarding is finished. See NavController#index and
    // WebMvcConfig - both need this, not ROLE, to correctly recognize
    // someone mid-vendor-onboarding instead of treating them as a planner.
    public static final String SIGNUP_INTENT = "auth.signupIntent";
    public static final String SUBSCRIPTION_PLAN = "auth.subscriptionPlan";
    public static final String SUBSCRIPTION_EXPIRES_AT = "auth.subscriptionExpiresAt";
    public static final String SUBSCRIPTION_EXPIRING_SOON = "auth.subscriptionExpiringSoon";
    public static final String SUBSCRIPTION_EXPIRED = "auth.subscriptionExpired";
    public static final String SUBSCRIPTION_IN_GRACE_PERIOD = "auth.subscriptionInGracePeriod";
    public static final String SUBSCRIPTION_GRACE_ENDS_AT = "auth.subscriptionGraceEndsAt";
    // "FREE_GRANT"/"PAYPAL"/"GCASH" (or null if never subscribed) - lets
    // PaywallModelAttributes tell a vendor with a pending GCash screenshot
    // awaiting admin review (GCASH, plan still null) apart from a real
    // paid/promo plan, since both can otherwise look similar.
    public static final String SUBSCRIPTION_BILLING_SOURCE = "auth.subscriptionBillingSource";
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
    // Set only when an admin used "View Dashboard" (see AdminController) to
    // open this vendor's dashboard without their own Google login - drives
    // the "Viewing as ... - Exit" banner on every vendor page. Never set by
    // any real login.
    public static final String IMPERSONATING_BY_ADMIN = "auth.impersonatingByAdmin";

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

    public static String signupIntent(HttpSession session) {
        return session == null ? null : (String) session.getAttribute(SIGNUP_INTENT);
    }

    public static String firstName(HttpSession session) {
        return session == null ? null : (String) session.getAttribute(FIRST_NAME);
    }

    public static String lastName(HttpSession session) {
        return session == null ? null : (String) session.getAttribute(LAST_NAME);
    }

    /** First + last name combined with a space - null/blank last name (or session) falls back to just the first name. */
    public static String fullName(HttpSession session) {
        String first = firstName(session);
        String last = lastName(session);
        return (last == null || last.isBlank()) ? first : first + " " + last;
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

    public static boolean isImpersonating(HttpSession session) {
        return session != null && Boolean.TRUE.equals(session.getAttribute(IMPERSONATING_BY_ADMIN));
    }

    public static void markImpersonating(HttpSession session) {
        session.setAttribute(IMPERSONATING_BY_ADMIN, true);
    }

    // Deliberately NOT session.invalidate() - AdminSession lives in this same
    // HttpSession under its own "admin.*" namespace and must survive this,
    // so "Exit" (see AdminController#exitImpersonation) drops the admin
    // straight back into their still-logged-in admin session instead of a
    // login page. Only removes this class's own "auth.*" attributes.
    public static void clear(HttpSession session) {
        session.removeAttribute(TOKEN);
        session.removeAttribute(USER_ID);
        session.removeAttribute(EMAIL);
        session.removeAttribute(FIRST_NAME);
        session.removeAttribute(LAST_NAME);
        session.removeAttribute(ROLE);
        session.removeAttribute(SIGNUP_INTENT);
        session.removeAttribute(SUBSCRIPTION_PLAN);
        session.removeAttribute(SUBSCRIPTION_EXPIRES_AT);
        session.removeAttribute(SUBSCRIPTION_EXPIRING_SOON);
        session.removeAttribute(SUBSCRIPTION_EXPIRED);
        session.removeAttribute(SUBSCRIPTION_IN_GRACE_PERIOD);
        session.removeAttribute(SUBSCRIPTION_GRACE_ENDS_AT);
        session.removeAttribute(SUBSCRIPTION_BILLING_SOURCE);
        session.removeAttribute(PAYWALL_SHOWN);
        session.removeAttribute(FIRST_PACKAGE_NUDGE_SHOWN);
        session.removeAttribute(REFERRAL_CODE);
        session.removeAttribute(IMPERSONATING_BY_ADMIN);
    }

    public static void store(HttpSession session, BackendAuthResponse auth) {
        session.setAttribute(TOKEN, auth.token());
        session.setAttribute(USER_ID, auth.id());
        session.setAttribute(EMAIL, auth.email());
        session.setAttribute(FIRST_NAME, auth.firstName());
        session.setAttribute(LAST_NAME, auth.lastName());
        session.setAttribute(ROLE, auth.role());
        session.setAttribute(SIGNUP_INTENT, auth.signupIntent());
        // Login only knows plan/planExpiresAt (see BackendAuthResponse), not
        // the finer expiringSoon/inGracePeriod/billingSource detail - only
        // the full subscription-status fetch computes those (see
        // storeSubscription's other callers, and VendorController#dashboard
        // which now refreshes this on every dashboard load). expired is left
        // false here even when plan() is null - a brand-new vendor with no
        // subscription at all isn't "expired," they just haven't picked a
        // plan yet (see PaywallModelAttributes#paywallRequiresPlanSelection),
        // and the dashboard's fresh fetch supplies the real state before any
        // paywall decision is made.
        storeSubscription(session, auth.plan(), auth.planExpiresAt(), false, false, false, null, null);
    }

    public static void storeSubscription(
            HttpSession session, String plan, java.time.Instant expiresAt, boolean expiringSoon, boolean expired,
            boolean inGracePeriod, java.time.Instant graceEndsAt, String billingSource) {
        session.setAttribute(SUBSCRIPTION_PLAN, plan);
        session.setAttribute(SUBSCRIPTION_EXPIRES_AT, expiresAt);
        session.setAttribute(SUBSCRIPTION_EXPIRING_SOON, expiringSoon);
        session.setAttribute(SUBSCRIPTION_EXPIRED, expired);
        session.setAttribute(SUBSCRIPTION_IN_GRACE_PERIOD, inGracePeriod);
        session.setAttribute(SUBSCRIPTION_GRACE_ENDS_AT, graceEndsAt);
        session.setAttribute(SUBSCRIPTION_BILLING_SOURCE, billingSource);
    }

    public static boolean isSubscriptionExpired(HttpSession session) {
        if (session == null) {
            // No real login on this page at all (most stub pages) - nothing to gate.
            return false;
        }
        Boolean expired = (Boolean) session.getAttribute(SUBSCRIPTION_EXPIRED);
        return Boolean.TRUE.equals(expired);
    }

    public static boolean isInSubscriptionGracePeriod(HttpSession session) {
        if (session == null) {
            return false;
        }
        Boolean inGracePeriod = (Boolean) session.getAttribute(SUBSCRIPTION_IN_GRACE_PERIOD);
        return Boolean.TRUE.equals(inGracePeriod);
    }

    public static java.time.Instant subscriptionGraceEndsAt(HttpSession session) {
        return session == null ? null : (java.time.Instant) session.getAttribute(SUBSCRIPTION_GRACE_ENDS_AT);
    }

    public static String subscriptionBillingSource(HttpSession session) {
        return session == null ? null : (String) session.getAttribute(SUBSCRIPTION_BILLING_SOURCE);
    }
}
