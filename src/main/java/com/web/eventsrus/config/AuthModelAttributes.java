package com.web.eventsrus.config;

import com.web.eventsrus.backend.WebSession;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Exposes the logged-in user's real full name/role to every view, so
 * fragments/common.html and fragments/planner-shell.html's topnavbars can
 * show who's actually logged in instead of the old hardcoded "Ian Orozco" /
 * "Ivs Phil" placeholders. Null on pages with no session at all - same
 * always-safe pattern as PaywallModelAttributes/AdminModelAttributes.
 */
@ControllerAdvice
public class AuthModelAttributes {

    @ModelAttribute("currentUserFullName")
    public String currentUserFullName(HttpServletRequest request) {
        return WebSession.fullName(request.getSession(false));
    }

    @ModelAttribute("currentUserRole")
    public String currentUserRole(HttpServletRequest request) {
        return WebSession.role(request.getSession(false));
    }
}
