package com.web.eventsrus.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.web.eventsrus.admin.AdminSession;
import com.web.eventsrus.backend.BackendClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * The admin module's one and only entry point, now that login IS the
 * real DB-backed auth check (see AdminAccountController/AdminAccountService
 * on the backend) rather than a shared-secret bridge. Standalone MockMvc -
 * no Spring context, no real backend - just BackendClient mocked out, same
 * as this controller sees it at runtime.
 */
@ExtendWith(MockitoExtension.class)
class AdminAuthControllerTest {

    @Mock
    private BackendClient backendClient;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminAuthController controller = new AdminAuthController(backendClient);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void correctCredentialsLogInAndStoreTheRealJwt() throws Exception {
        when(backendClient.adminLogin("ianadmin", "correct-horse-battery-staple")).thenReturn("a.real.jwt");

        mockMvc.perform(post("/admin/login")
                        .param("username", "ianadmin")
                        .param("password", "correct-horse-battery-staple"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(request().sessionAttribute(AdminSession.USERNAME, "ianadmin"))
                .andExpect(request().sessionAttribute(AdminSession.TOKEN, "a.real.jwt"));
    }

    @Test
    void wrongCredentialsShowAnErrorAndDoNotLogIn() throws Exception {
        when(backendClient.adminLogin(eq("ianadmin"), eq("wrong"))).thenReturn(null);

        mockMvc.perform(post("/admin/login")
                        .param("username", "ianadmin")
                        .param("password", "wrong"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/login"))
                .andExpect(model().attributeExists("loginError"))
                .andExpect(request().sessionAttributeDoesNotExist(AdminSession.USERNAME));
    }

    @Test
    void loginPageRedirectsAwayWhenAlreadyLoggedIn() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AdminSession.USERNAME, "ianadmin");

        mockMvc.perform(get("/admin/login").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    void loginPageShowsTheFormWhenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/admin/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/login"));
    }
}
