package com.web.eventsrus.controller;

import com.web.eventsrus.admin.AdminAccountService;
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
 * checked against AdminAccountService's in-memory account list.
 */
@Controller
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAccountService adminAccountService;
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
        if (!adminAccountService.authenticate(username, password)) {
            model.addAttribute("loginError", "Invalid username or password.");
            return "admin/login";
        }
        AdminSession.login(session, username);
        // Best-effort - null (backend unreachable, or the shared key isn't
        // configured) just means the real-backend admin pages (vendor
        // verification) show a "backend unavailable" notice instead of
        // blocking this login. See BackendClient#adminLogin.
        AdminSession.storeToken(session, backendClient.adminLogin());
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/admin/logout")
    public String logout(HttpSession session) {
        session.removeAttribute(AdminSession.USERNAME);
        return "redirect:/admin/login";
    }
}
