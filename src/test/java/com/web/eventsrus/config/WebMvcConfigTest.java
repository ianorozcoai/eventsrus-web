package com.web.eventsrus.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.web.eventsrus.backend.WebSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

/**
 * The /planner/** gate - must block an account locked to the vendor
 * identity (signupIntent=VENDOR) even though its role is still PLANNER
 * until onboarding finishes, the same distinction NavController#index
 * makes for "/". See WebMvcConfigTest's sibling, NavControllerTest, for
 * the "/" side of this same fix.
 */
class WebMvcConfigTest {

    private final WebMvcConfig.RequireNotVendorIntentInterceptor interceptor =
            new WebMvcConfig.RequireNotVendorIntentInterceptor();

    @Test
    void redirectsToLoginWhenNotLoggedIn() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getSession(false)).thenReturn(null);

        boolean proceed = interceptor.preHandle(request, response, new Object());

        assertThat(proceed).isFalse();
        verify(response).sendRedirect("/planner/");
    }

    @Test
    void allowsARealPlannerThrough() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(WebSession.TOKEN, "a.jwt");
        session.setAttribute(WebSession.SIGNUP_INTENT, "PLANNER");
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getSession(false)).thenReturn(session);

        boolean proceed = interceptor.preHandle(request, response, new Object());

        assertThat(proceed).isTrue();
        verify(response, never()).sendRedirect(anyString());
    }

    @Test
    void redirectsSomeoneMidVendorOnboardingToFinishItInsteadOfThePlannerSide() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(WebSession.TOKEN, "a.jwt");
        session.setAttribute(WebSession.ROLE, "PLANNER");
        session.setAttribute(WebSession.SIGNUP_INTENT, "VENDOR");
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getSession(false)).thenReturn(session);

        boolean proceed = interceptor.preHandle(request, response, new Object());

        assertThat(proceed).isFalse();
        verify(response).sendRedirect("/vendor/onboarding");
    }

    @Test
    void redirectsAFullyOnboardedVendorToTheirOwnDashboard() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(WebSession.TOKEN, "a.jwt");
        session.setAttribute(WebSession.ROLE, "VENDOR");
        session.setAttribute(WebSession.SIGNUP_INTENT, "VENDOR");
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getSession(false)).thenReturn(session);

        boolean proceed = interceptor.preHandle(request, response, new Object());

        assertThat(proceed).isFalse();
        verify(response).sendRedirect("/vendor/dashboard");
    }
}
