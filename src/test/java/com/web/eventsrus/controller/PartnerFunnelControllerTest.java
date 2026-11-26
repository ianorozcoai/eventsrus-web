package com.web.eventsrus.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.web.eventsrus.backend.WebSession;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** The 3 funnel pages must load, and ?ref= / ?promo= must survive them on the way to /vendor/. */
class PartnerFunnelControllerTest {

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new PartnerFunnelController())
            .setViewResolvers((viewName, locale) -> (model, request, response) -> { })
            .build();

    @Test
    void allThreePagesLoad() throws Exception {
        mvc.perform(get("/partner/")).andExpect(status().isOk()).andExpect(view().name("partner/hook"));
        mvc.perform(get("/partner/opportunity")).andExpect(status().isOk()).andExpect(view().name("partner/opportunity"));
        mvc.perform(get("/partner/join")).andExpect(status().isOk()).andExpect(view().name("partner/join"));
    }

    @Test
    void refAndPromoAreRememberedFromAnyPage() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mvc.perform(get("/partner/").param("ref", "ABC123").param("promo", "FREE3BOOKINGS").session(session));
        mvc.perform(get("/partner/join").session(session));

        assertThat(WebSession.referralCode(session)).isEqualTo("ABC123");
        assertThat(WebSession.promoCode(session)).isEqualTo("FREE3BOOKINGS");
    }
}
