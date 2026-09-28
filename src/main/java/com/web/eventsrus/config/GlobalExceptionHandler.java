package com.web.eventsrus.config;

import com.web.eventsrus.backend.BackendApiException;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
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
@Slf4j
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

    /**
     * A file over spring.servlet.multipart.max-file-size/max-request-size
     * throws this mid-parse, before any controller method runs - there's no
     * page-specific flash attribute to set here the way individual
     * controllers do for their own upload validation (InvalidFileTypeException,
     * TooManyAttachmentsException, ...). Answering it ourselves and forwarding
     * to CustomErrorController - rather than just calling response.sendError()
     * and relying on the servlet container's own error-page dispatch - is
     * deliberate: DefaultHandlerExceptionResolver already calls sendError()
     * for this exact exception, and it still reaches the browser as a bare,
     * bodyless 413 with the connection closed, never reaching "/error". This
     * bypasses whatever's swallowing that dispatch entirely.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex, HttpServletResponse response, HttpServletRequest request) {
        log.warn(
                "MaxUploadSizeExceededException on {} {} - Content-Length header: {}, Content-Type: {}, User-Agent: {}",
                request.getMethod(), request.getRequestURI(), request.getHeader("Content-Length"),
                request.getHeader("Content-Type"), request.getHeader("User-Agent"), ex);
        response.setStatus(HttpStatus.CONTENT_TOO_LARGE.value());
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.CONTENT_TOO_LARGE.value());
        return "forward:/error";
    }
}
