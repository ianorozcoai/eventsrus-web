package com.web.eventsrus.admin;

import jakarta.servlet.http.HttpSession;

/**
 * Session attribute for the admin module's login - entirely separate from
 * WebSession (the vendor Google-login flow). An admin session only ever
 * carries a username; there's no JWT or backend call involved at all, since
 * the whole admin module is self-contained in eventsrus-web (see
 * AdminAccountService).
 */
public final class AdminSession {

    public static final String USERNAME = "admin.username";

    private AdminSession() {
    }

    public static boolean isLoggedIn(HttpSession session) {
        return session != null && session.getAttribute(USERNAME) != null;
    }

    public static String username(HttpSession session) {
        return session == null ? null : (String) session.getAttribute(USERNAME);
    }

    public static void login(HttpSession session, String username) {
        session.setAttribute(USERNAME, username);
    }
}
