package com.web.eventsrus.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.web.eventsrus.backend.BackendApiException;
import com.web.eventsrus.backend.BackendClient;
import com.web.eventsrus.backend.WebSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

/**
 * The vendor side of a quotation exchange, previously a dead end: the page
 * showed "Not sent yet" for a REQUESTED quotation with no way for the
 * vendor to ever change that. See BackendClient#respondToQuotation and the
 * real backend's already-existing POST .../respond endpoint this now
 * actually calls.
 */
@ExtendWith(MockitoExtension.class)
class VendorControllerQuotationRespondTest {

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

    private MockHttpSession activeSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(WebSession.TOKEN, "a.jwt");
        return session;
    }

    @Test
    void uploadingAPdfRespondsAndRedirectsWithSuccess() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile("pdf", "quote.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/vendor/quotations/42/respond").file(pdf)
                        .param("message", "Here's the revised quote")
                        .session(activeSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vendor/quotations"))
                .andExpect(flash().attribute("quotationResponded", true));

        verify(backendClient).respondToQuotation(eq("a.jwt"), eq(42L), any(), eq("Here's the revised quote"));
    }

    @Test
    void aMessageIsOptional() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile("pdf", "quote.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/vendor/quotations/42/respond").file(pdf).session(activeSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("quotationResponded", true));

        verify(backendClient).respondToQuotation(eq("a.jwt"), eq(42L), any(), eq((String) null));
    }

    @Test
    void rejectsAnEmptyFileWithoutCallingTheBackend() throws Exception {
        MockMultipartFile emptyPdf = new MockMultipartFile("pdf", "quote.pdf", "application/pdf", new byte[0]);

        mockMvc.perform(multipart("/vendor/quotations/42/respond").file(emptyPdf).session(activeSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vendor/quotations"))
                .andExpect(flash().attributeExists("quotationsError"));

        verify(backendClient, never()).respondToQuotation(any(), any(), any(), any());
    }

    @Test
    void blocksAnExpiredSubscriptionWithoutCallingTheBackend() throws Exception {
        MockHttpSession session = activeSession();
        session.setAttribute(WebSession.SUBSCRIPTION_EXPIRED, true);
        MockMultipartFile pdf = new MockMultipartFile("pdf", "quote.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/vendor/quotations/42/respond").file(pdf).session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vendor/quotations"))
                .andExpect(flash().attributeExists("quotationsError"));

        verify(backendClient, never()).respondToQuotation(any(), any(), any(), any());
    }

    @Test
    void surfacesABackendFailureAsAFlashError() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile("pdf", "quote.pdf", "application/pdf", "content".getBytes());
        when(backendClient.respondToQuotation(eq("a.jwt"), eq(42L), any(), any()))
                .thenThrow(new BackendApiException("Vendor subscription is not active", 402));

        mockMvc.perform(multipart("/vendor/quotations/42/respond").file(pdf).session(activeSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vendor/quotations"))
                .andExpect(flash().attribute("quotationsError", "Vendor subscription is not active"));
    }
}
