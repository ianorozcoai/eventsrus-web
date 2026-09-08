package com.web.eventsrus.config;

import com.web.eventsrus.backend.BackendApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Most controller methods that call eventsrus-backend already catch
 * BackendApiException themselves and turn it into a flash error on the same
 * page (settingsError, packagesError, ...) - but a plain page-LOAD call (the
 * GET handlers that render dashboard/packages/settings/etc. by fetching the
 * vendor's own data) never wraps that fetch in a try/catch, since there's
 * nothing sensible to show on a normal render failure other than the
 * fetch's own data.
 *
 * A stale/expired session is exactly this kind of failure, and it isn't
 * rare: eventsrus-backend's JwtAuthenticationFilter silently treats an
 * expired or otherwise invalid JWT as an anonymous request (it never itself
 * returns 401) - so a session sitting open past the token's lifetime, or a
 * token invalidated for any other reason, surfaces here purely as a plain
 * 403 FORBIDDEN on whatever the vendor happens to click next. Without this
 * handler, that turns into Spring Boot's default Whitelabel Error Page and
 * a raw stack trace - just log back in fixes it, but nothing on screen ever
 * says so.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BackendApiException.class)
    public String handleBackendApiException(
            BackendApiException e,
            HttpServletRequest request,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (e.getStatus() != HttpStatus.UNAUTHORIZED.value() && e.getStatus() != HttpStatus.FORBIDDEN.value()) {
            throw e; // not a session problem - keep the existing Whitelabel/500 behavior
        }
        session.invalidate();
        redirectAttributes.addFlashAttribute("loginError", "Your session has expired. Please log in again.");
        String loginPath = request.getRequestURI().startsWith("/planner") ? "/planner" : "/vendor";
        return "redirect:" + loginPath;
    }
}
