package com.web.eventsrus.config;

import com.web.eventsrus.admin.AdminSession;
import com.web.eventsrus.backend.WebSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Real-login gating for the three real-auth areas of the app: the planner
 * experience, the vendor business pages, and the admin module. Everything
 * else (public storefronts, the login/landing pages themselves) stays open.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Any logged-in user (either role) can use the planner experience -
        // a vendor can also plan/book events, same dual-sided-marketplace
        // convention used everywhere else in this app.
        registry.addInterceptor(new RequireRoleInterceptor(null, "/planner/"))
                .addPathPatterns("/planner/**")
                .excludePathPatterns("/planner", "/planner/");

        // Onboarding itself only requires SOME login (matches the real
        // backend's own permission model - PATCH /api/v1/users/me/vendor
        // isn't role-restricted, since a fresh PLANNER is exactly who's
        // meant to call it) - not gated to existing VENDORs only.
        registry.addInterceptor(new RequireRoleInterceptor(null, "/vendor/"))
                .addPathPatterns("/vendor/onboarding");

        // Every other vendor business page requires an actual VENDOR
        // account. Storefronts stay fully public (planner-facing), and the
        // login/onboarding pages are excluded so they're reachable at all.
        registry.addInterceptor(new RequireRoleInterceptor("VENDOR", "/vendor/"))
                .addPathPatterns("/vendor/**")
                .excludePathPatterns("/vendor", "/vendor/", "/vendor/onboarding", "/vendor/storefront/**");

        registry.addInterceptor(new RequireAdminLoginInterceptor())
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin", "/admin/", "/admin/login");
    }

    /**
     * requiredRole == null means "any logged-in user"; otherwise the
     * session's stored role must match exactly. Not logged in at all ->
     * redirect to loginPath. Logged in but wrong role -> redirect to
     * /vendor/onboarding (a nudge to finish becoming a vendor) rather than
     * bouncing back to a login screen for someone who's already signed in.
     */
    private static class RequireRoleInterceptor implements HandlerInterceptor {
        private final String requiredRole;
        private final String loginPath;

        RequireRoleInterceptor(String requiredRole, String loginPath) {
            this.requiredRole = requiredRole;
            this.loginPath = loginPath;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
                throws Exception {
            HttpSession session = request.getSession(false);
            if (!WebSession.isLoggedIn(session)) {
                response.sendRedirect(loginPath);
                return false;
            }
            if (requiredRole != null && !requiredRole.equals(WebSession.role(session))) {
                response.sendRedirect("/vendor/onboarding");
                return false;
            }
            return true;
        }
    }

    /** Gates every /admin/** page except the login form itself (see AdminAuthController). */
    private static class RequireAdminLoginInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
                throws Exception {
            if (AdminSession.isLoggedIn(request.getSession(false))) {
                return true;
            }
            response.sendRedirect("/admin/login");
            return false;
        }
    }
}
