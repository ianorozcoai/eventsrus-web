package com.web.eventsrus.config;

import com.web.eventsrus.admin.AdminSession;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Exposes the logged-in admin's username to every view as a plain model
 * attribute, so fragments/admin-shell.html's topnavbar can show "who's
 * logged in" without every admin controller method needing to read the
 * session itself. Null (and simply unused) on every non-admin page.
 */
@ControllerAdvice
public class AdminModelAttributes {

    @ModelAttribute("adminUsername")
    public String adminUsername(HttpServletRequest request) {
        return AdminSession.username(request.getSession(false));
    }
}
