package com.web.eventsrus.controller;

import com.web.eventsrus.admin.AdminSession;
import com.web.eventsrus.backend.BackendClient;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Login/logout for the admin module - entirely separate from the vendor
 * Google sign-in (see AuthWebController/WebSession). Username/password,
 * checked against eventsrus-backend's real admin_accounts table (see
 * BackendClient#adminLogin / AdminAccountController there) - a successful
 * login IS the JWT-issuing step now, not a separate best-effort bridge call.
 */
@Controller
@RequiredArgsConstructor
public class AdminAuthController {

    private final BackendClient backendClient;

    // "/admin" and "/admin/" ARE the login page, not just a redirect to one -
    // that's the one URL this module was asked to expose.
    @GetMapping({"/admin", "/admin/", "/admin/login"})
    public String loginPage(HttpSession session) {
        if (AdminSession.isLoggedIn(session)) {
            return "redirect:/admin/dashboard";
        }
        return "admin/login";
    }

    @PostMapping("/admin/login")
    public String login(
            @RequestParam String username, @RequestParam String password, HttpSession session, Model model) {
        String token = backendClient.adminLogin(username, password);
        if (token == null) {
            model.addAttribute("loginError", "Invalid username or password, or eventsrus-backend is unreachable.");
            return "admin/login";
        }
        AdminSession.login(session, username);
        AdminSession.storeToken(session, token);
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/admin/logout")
    public String logout(HttpSession session) {
        session.removeAttribute(AdminSession.USERNAME);
        return "redirect:/admin/login";
    }
}
