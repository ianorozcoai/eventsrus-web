package com.web.eventsrus.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.web.eventsrus.backend.BackendClient;
import com.web.eventsrus.backend.WebSession;
import com.web.eventsrus.model.VendorOnboardingForm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

/**
 * Supplier outreach emails link to /vendor/?promo=CODE. The code must survive the Google sign-in
 * (it is kept in the session) and arrive pre-filled in the Promo Code box on the onboarding form.
 */
@ExtendWith(MockitoExtension.class)
class VendorPromoLinkTest {

    @Mock
    private BackendClient backendClient;
    @Mock
    private ObjectMapper objectMapper;

    private static MockMvc standalone(Object controller) {
        // A no-op view: the real templates are not needed, and a plain view name equal to the request
        // path would otherwise trip Spring's circular-view check in standalone mode.
        return MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers((viewName, locale) -> (model, request, response) -> { })
                .build();
    }

    @Test
    void theVendorLinkRemembersThePromoCodeInTheSession() throws Exception {
        MockHttpSession session = new MockHttpSession();

        standalone(new AuthWebController(backendClient))
                .perform(get("/vendor/").param("promo", "FREE3BOOKINGS").session(session))
                .andExpect(status().isOk());

        assertThat(WebSession.promoCode(session)).isEqualTo("FREE3BOOKINGS");
    }

    @Test
    void aReloadWithoutPromoKeepsTheCodeAndANewCodeReplacesTheOldOne() throws Exception {
        MockHttpSession session = new MockHttpSession();
        MockMvc mvc = standalone(new AuthWebController(backendClient));

        mvc.perform(get("/vendor/").param("promo", "FIRST").session(session));
        mvc.perform(get("/vendor/").session(session));
        assertThat(WebSession.promoCode(session)).isEqualTo("FIRST");

        mvc.perform(get("/vendor/").param("promo", "SECOND").session(session));
        assertThat(WebSession.promoCode(session)).isEqualTo("SECOND");
    }

    @Test
    void junkOrOversizedPromoValuesAreIgnored() throws Exception {
        MockHttpSession session = new MockHttpSession();
        MockMvc mvc = standalone(new AuthWebController(backendClient));

        mvc.perform(get("/vendor/").param("promo", "<script>alert(1)</script>").session(session));
        mvc.perform(get("/vendor/").param("promo", "A".repeat(41)).session(session));
        mvc.perform(get("/vendor/").param("promo", "   ").session(session));

        assertThat(WebSession.promoCode(session)).isNull();
    }

    @Test
    void theOnboardingFormStartsWithThePromoCodeFilledIn() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(WebSession.TOKEN, "a.jwt");
        session.setAttribute(WebSession.EMAIL, "owner@example.com");
        WebSession.stashPromoCode(session, "FREE3BOOKINGS");

        var result = standalone(new VendorController(objectMapper, backendClient))
                .perform(get("/vendor/onboarding").session(session))
                .andExpect(status().isOk())
                .andReturn();

        VendorOnboardingForm form = (VendorOnboardingForm) result.getModelAndView().getModel().get("vendorOnboardingForm");
        assertThat(form.getPromoCode()).isEqualTo("FREE3BOOKINGS");
    }

    @Test
    void withoutAPromoLinkTheBoxStaysEmpty() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(WebSession.TOKEN, "a.jwt");

        var result = standalone(new VendorController(objectMapper, backendClient))
                .perform(get("/vendor/onboarding").session(session))
                .andReturn();

        VendorOnboardingForm form = (VendorOnboardingForm) result.getModelAndView().getModel().get("vendorOnboardingForm");
        assertThat(form.getPromoCode()).isNull();
    }
}
