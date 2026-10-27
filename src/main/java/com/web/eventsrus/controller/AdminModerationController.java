package com.web.eventsrus.controller;

import com.web.eventsrus.admin.AdminSession;
import com.web.eventsrus.backend.BackendAdminAccountDeletion;
import com.web.eventsrus.backend.BackendAdminBlock;
import com.web.eventsrus.backend.BackendAdminReport;
import com.web.eventsrus.backend.BackendApiException;
import com.web.eventsrus.backend.BackendClient;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Admin visibility of the safety features the mobile app offers (Google Play requires them, and an admin has to be
 * able to see and act on them): reports from users (can be marked resolved), who has blocked whom, and account
 * deletion requests. The website itself has no block/report/delete controls for suppliers or planners.
 */
@Controller
@RequestMapping("/admin/moderation")
@RequiredArgsConstructor
public class AdminModerationController {

    private final BackendClient backendClient;

    @GetMapping
    public String page(HttpSession session, Model model) {
        model.addAttribute("activePage", "moderation");
        String jwt = AdminSession.token(session);
        List<BackendAdminReport> reports = List.of();
        List<BackendAdminBlock> blocks = List.of();
        List<BackendAdminAccountDeletion> deletions = List.of();
        if (jwt == null) {
            model.addAttribute("backendUnavailable", true);
        } else {
            try {
                reports = backendClient.listAdminReports(jwt);
                blocks = backendClient.listAdminBlocks(jwt);
                deletions = backendClient.listAdminAccountDeletions(jwt);
            } catch (BackendApiException e) {
                model.addAttribute("backendUnavailable", true);
            }
        }
        model.addAttribute("openReports", reports.stream().filter(r -> "OPEN".equals(r.status())).toList());
        model.addAttribute("resolvedReports", reports.stream().filter(r -> !"OPEN".equals(r.status())).toList());
        model.addAttribute("blocks", blocks);
        model.addAttribute("pendingDeletions", deletions.stream().filter(d -> "PENDING".equals(d.status())).toList());
        model.addAttribute("completedDeletions", deletions.stream().filter(d -> "COMPLETED".equals(d.status())).toList());
        return "admin/moderation";
    }

    @PostMapping("/reports/{reportId}/resolve")
    public String resolve(
            @PathVariable Long reportId, @RequestParam(required = false) String note,
            HttpSession session, RedirectAttributes redirectAttributes) {
        String jwt = AdminSession.token(session);
        if (jwt == null) {
            redirectAttributes.addFlashAttribute("moderationError",
                    "Not connected to eventsrus-backend right now - log out and back in to retry.");
            return "redirect:/admin/moderation";
        }
        try {
            backendClient.resolveAdminReport(jwt, reportId, note);
            redirectAttributes.addFlashAttribute("moderationSuccess", "Report marked as resolved.");
        } catch (BackendApiException e) {
            redirectAttributes.addFlashAttribute("moderationError", e.getMessage());
        }
        return "redirect:/admin/moderation";
    }
}
