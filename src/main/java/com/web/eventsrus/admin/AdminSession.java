package com.web.eventsrus.admin;

import jakarta.servlet.http.HttpSession;

/**
 * Session attribute for the admin module's login - entirely separate from
 * WebSession (the vendor Google-login flow). Username/password is checked
 * against eventsrus-backend's real admin_accounts table (BCrypt-hashed,
 * see AdminAccountService there), and a successful login returns a real
 * ADMIN-role JWT in the same call (see AdminAuthController#login /
 * BackendClient#adminLogin) - there's no local account store here at all
 * anymore. TOKEN can still be null if eventsrus-backend becomes unreachable
 * *after* login (mid-session) - pages that need it show a "backend
 * unavailable" notice rather than a stack trace.
 */
public final class AdminSession {

    public static final String USERNAME = "admin.username";
    public static final String TOKEN = "admin.token";

    private AdminSession() {
    }

    public static boolean isLoggedIn(HttpSession session) {
        return session != null && session.getAttribute(USERNAME) != null;
    }

    public static String username(HttpSession session) {
        return session == null ? null : (String) session.getAttribute(USERNAME);
    }

    public static String token(HttpSession session) {
        return session == null ? null : (String) session.getAttribute(TOKEN);
    }

    public static void login(HttpSession session, String username) {
        session.setAttribute(USERNAME, username);
    }

    /** Best-effort - null when the backend bridge call failed or the backend was unreachable. */
    public static void storeToken(HttpSession session, String token) {
        session.setAttribute(TOKEN, token);
    }
}
