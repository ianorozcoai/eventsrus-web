package com.web.eventsrus.admin;

import jakarta.servlet.http.HttpSession;

/**
 * Session attribute for the admin module's login - entirely separate from
 * WebSession (the vendor Google-login flow). An admin session carries a
 * username (checked against AdminAccountService's in-memory account list -
 * no JWT involved for that part at all) plus, when the backend bridge
 * succeeds (see AdminAuthController#login / BackendClient#adminLogin), a
 * real ADMIN-role JWT for calling eventsrus-backend's actual
 * /api/v1/admin/** endpoints. TOKEN can be absent even while logged in - the
 * bridge call is best-effort, so the local-only admin pages (planners,
 * vendors directory, admin accounts) keep working even if
 * eventsrus-backend is unreachable at login time.
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
