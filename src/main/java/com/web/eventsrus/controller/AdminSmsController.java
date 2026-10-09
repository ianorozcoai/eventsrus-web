package com.web.eventsrus.controller;

import com.web.eventsrus.admin.AdminSession;
import com.web.eventsrus.backend.BackendApiException;
import com.web.eventsrus.backend.BackendClient;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Admin ad-hoc SMS tool (any number, any message) plus a send history -
 * deliberately NOT tied to any quotation/booking entity, unlike the
 * vendor-triggered SMS that fires on its own from
 * QuotationService/BookingService/BookingAmendmentService. Sends are
 * disabled backend-side (app.sms-enabled=false) until Semaphore approves the
 * "EventsRUsPH" sender name - sendAdhocSms still returns a real result in
 * that case (success=false with a clear reason), not an error, so this page
 * always shows something meaningful rather than a generic failure banner.
 */
@Controller
@RequestMapping("/admin/sms")
@RequiredArgsConstructor
public class AdminSmsController {

    private final BackendClient backendClient;

    @GetMapping
    public String page(HttpSession session, Model model) {
        model.addAttribute("activePage", "sms");
        String jwt = AdminSession.token(session);
        if (jwt == null) {
            model.addAttribute("backendUnavailable", true);
            model.addAttribute("history", List.of());
            return "admin/sms";
        }
        try {
            model.addAttribute("history", backendClient.listSmsHistory(jwt));
        } catch (BackendApiException e) {
            model.addAttribute("backendUnavailable", true);
            model.addAttribute("history", List.of());
        }
        return "admin/sms";
    }

    @PostMapping("/send")
    public String send(
            @RequestParam String number,
            @RequestParam String message,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        String jwt = AdminSession.token(session);
        if (jwt == null) {
            redirectAttributes.addFlashAttribute("smsError", "Not connected to eventsrus-backend - log out and back in to retry.");
            return "redirect:/admin/sms";
        }
        try {
            var result = backendClient.sendAdhocSms(jwt, number, message);
            if (result.success()) {
                redirectAttributes.addFlashAttribute("smsSent", true);
            } else {
                redirectAttributes.addFlashAttribute("smsError", result.errorMessage());
            }
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("smsError", e.getMessage());
        }
        return "redirect:/admin/sms";
    }
}
