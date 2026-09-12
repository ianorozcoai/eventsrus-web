package com.web.eventsrus.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.web.eventsrus.backend.BackendApiException;
import com.web.eventsrus.backend.BackendClient;
import com.web.eventsrus.backend.WebSession;
import com.web.eventsrus.model.PlannerEvent;
import com.web.eventsrus.model.VendorPublicProfile;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

/**
 * A planner arriving at a vendor's storefront from one of their own events
 * (eventId on the URL) already has their name and that event's date on
 * hand - the quotation/inquiry forms should start pre-filled with both
 * instead of making them retype what the app already knows.
 */
@ExtendWith(MockitoExtension.class)
class VendorControllerStorefrontTest {

    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private BackendClient backendClient;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        VendorController controller = new VendorController(objectMapper, backendClient);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private VendorPublicProfile profile(long vendorUserId) {
        return new VendorPublicProfile(
                vendorUserId, "Business", "Owner", "Description", null, null, "City", "State", "Country",
                "contact@example.com", "0900", null, null, null, null, null, false, java.util.List.of(),
                java.util.List.of(), null, 0, null, java.util.List.of(), java.util.List.of());
    }

    @Test
    void prefillsPlannerNameAndEventDateWhenArrivingFromAnEvent() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(WebSession.TOKEN, "a.jwt");
        session.setAttribute(WebSession.FIRST_NAME, "Ian");
        when(backendClient.getVendorProfile(eq("a.jwt"), eq("some-vendor"), eq(42L))).thenReturn(profile(9001L));
        when(backendClient.getEvent("a.jwt", 42L))
                .thenReturn(new PlannerEvent(42L, "Birthday", null, LocalDate.of(2026, 12, 25), null, null, null, false, null));

        mockMvc.perform(get("/vendor/storefront/some-vendor").param("eventId", "42").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attribute("quotationRequestForm", org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.hasProperty("plannerName", org.hamcrest.Matchers.is("Ian")),
                        org.hamcrest.Matchers.hasProperty("targetDate", org.hamcrest.Matchers.is(LocalDate.of(2026, 12, 25))))))
                .andExpect(model().attribute("inquiryForm", org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.hasProperty("plannerName", org.hamcrest.Matchers.is("Ian")),
                        org.hamcrest.Matchers.hasProperty("targetDate", org.hamcrest.Matchers.is(LocalDate.of(2026, 12, 25))))));
    }

    @Test
    void leavesTheDateBlankWhenThereIsNoEventId() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(WebSession.TOKEN, "a.jwt");
        session.setAttribute(WebSession.FIRST_NAME, "Ian");
        when(backendClient.getVendorProfile(eq("a.jwt"), eq("some-vendor"), eq(null))).thenReturn(profile(9001L));

        mockMvc.perform(get("/vendor/storefront/some-vendor").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attribute("quotationRequestForm", org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.hasProperty("plannerName", org.hamcrest.Matchers.is("Ian")),
                        org.hamcrest.Matchers.hasProperty("targetDate", org.hamcrest.Matchers.nullValue()))));
    }

    @Test
    void leavesTheDateBlankWhenTheEventLookupFails() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(WebSession.TOKEN, "a.jwt");
        session.setAttribute(WebSession.FIRST_NAME, "Ian");
        when(backendClient.getVendorProfile(eq("a.jwt"), eq("some-vendor"), eq(42L))).thenReturn(profile(9001L));
        when(backendClient.getEvent("a.jwt", 42L)).thenThrow(new BackendApiException("not found", 404));

        mockMvc.perform(get("/vendor/storefront/some-vendor").param("eventId", "42").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attribute("quotationRequestForm", org.hamcrest.Matchers.hasProperty(
                        "targetDate", org.hamcrest.Matchers.nullValue())));
    }

    @Test
    void leavesTheNameBlankForAnAnonymousVisitor() throws Exception {
        when(backendClient.getVendorProfile(eq(null), eq("some-vendor"), any())).thenReturn(profile(9001L));

        mockMvc.perform(get("/vendor/storefront/some-vendor"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("quotationRequestForm", org.hamcrest.Matchers.hasProperty(
                        "plannerName", org.hamcrest.Matchers.nullValue())));
    }
}
