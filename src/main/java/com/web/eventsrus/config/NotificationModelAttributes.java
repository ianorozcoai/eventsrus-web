package com.web.eventsrus.config;

import com.web.eventsrus.backend.BackendApiException;
import com.web.eventsrus.backend.BackendClient;
import com.web.eventsrus.backend.WebSession;
import com.web.eventsrus.model.Notification;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Backs the bell icon in both fragments/common.html (vendor shell) and
 * fragments/planner-shell.html's topnavbar - the unread count for the
 * badge, and the most recent notifications for the dropdown - as plain
 * model attributes on every request, same always-safe pattern as
 * PaywallModelAttributes/VendorNavBadgeModelAttributes: a no-op (no backend
 * call, badge just doesn't show) unless the session is an actual logged-in
 * planner or vendor. The admin module has its own separate session/JWT
 * (AdminSession, not WebSession) whose subject isn't a real backend user by
 * email, so it isn't wired into this - it never had a bell to begin with.
 */
@ControllerAdvice
public class NotificationModelAttributes {

    private static final int DROPDOWN_LIMIT = 8;

    private final BackendClient backendClient;

    public NotificationModelAttributes(BackendClient backendClient) {
        this.backendClient = backendClient;
    }

    @ModelAttribute
    public void addNotifications(HttpServletRequest request, Model model) {
        long unread = 0;
        List<Notification> recent = List.of();

        HttpSession session = request.getSession(false);
        if (session != null && WebSession.isLoggedIn(session)) {
            try {
                List<Notification> all = backendClient.getNotifications(WebSession.token(session));
                unread = all.stream().filter(n -> !n.read()).count();
                recent = all.stream().limit(DROPDOWN_LIMIT).toList();
            } catch (BackendApiException e) {
                // A notification fetch failing shouldn't break the page it's
                // decorating - just show no badge/dropdown content.
            }
        }

        model.addAttribute("unreadNotificationCount", unread);
        model.addAttribute("recentNotifications", recent);
    }
}
