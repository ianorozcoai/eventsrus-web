package com.web.eventsrus.controller;

import com.web.eventsrus.admin.AdminSession;
import com.web.eventsrus.backend.WebSession;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Replaces Spring Boot's default Whitelabel Error Page for every unhandled
 * error - a misspelled/unknown URL (404) or any genuine uncaught exception
 * (500) - with a friendly, on-brand page instead. Spring Boot forwards any
 * unhandled error/exception to "/error" automatically; mapping a controller
 * there is the whole mechanism, no other wiring changes needed anywhere
 * else. Deliberately separate from GlobalExceptionHandler, which only
 * handles one specific case (an expired session, redirected straight back
 * to login) - everything else still lands here.
 */
@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, HttpSession session, Model model) {
        Object statusAttr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = statusAttr != null ? Integer.parseInt(statusAttr.toString()) : 500;
        model.addAttribute("isNotFound", status == 404);

        if (AdminSession.isLoggedIn(session)) {
            model.addAttribute("dashboardUrl", "/admin/dashboard");
            model.addAttribute("showLogout", true);
            model.addAttribute("logoutAction", "/admin/logout");
        } else if (WebSession.isLoggedIn(session)) {
            model.addAttribute("dashboardUrl", "VENDOR".equals(WebSession.role(session)) ? "/vendor/dashboard" : "/planner/dashboard");
            model.addAttribute("showLogout", true);
            model.addAttribute("logoutAction", "/logout");
        } else {
            model.addAttribute("dashboardUrl", "/");
            model.addAttribute("showLogout", false);
        }

        return "error";
    }
}
