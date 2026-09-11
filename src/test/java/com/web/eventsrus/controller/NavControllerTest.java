package com.web.eventsrus.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.web.eventsrus.backend.WebSession;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * "/" for an already-logged-in visitor - must route on signupIntent, not
 * role, or someone mid-vendor-onboarding (still role=PLANNER until
 * becomeVendor succeeds) lands on the planner dashboard as if they'd never
 * started, instead of back where they left off. Regression coverage for
 * that exact bug.
 */
class NavControllerTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new NavController()).build();

    @Test
    void showsTheLandingPageWhenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("landing"));
    }

    @Test
    void aRealPlannerGoesToThePlannerDashboard() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(WebSession.TOKEN, "a.jwt");
        session.setAttribute(WebSession.ROLE, "PLANNER");
        session.setAttribute(WebSession.SIGNUP_INTENT, "PLANNER");

        mockMvc.perform(get("/").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/planner/dashboard"));
    }

    @Test
    void someoneMidVendorOnboardingGoesBackToOnboardingNotThePlannerDashboard() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(WebSession.TOKEN, "a.jwt");
        session.setAttribute(WebSession.ROLE, "PLANNER");
        session.setAttribute(WebSession.SIGNUP_INTENT, "VENDOR");

        mockMvc.perform(get("/").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vendor/onboarding"));
    }

    @Test
    void aFullyOnboardedVendorGoesToTheVendorDashboard() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(WebSession.TOKEN, "a.jwt");
        session.setAttribute(WebSession.ROLE, "VENDOR");
        session.setAttribute(WebSession.SIGNUP_INTENT, "VENDOR");

        mockMvc.perform(get("/").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vendor/dashboard"));
    }
}
