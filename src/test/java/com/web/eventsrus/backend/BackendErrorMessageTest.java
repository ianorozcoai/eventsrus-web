package com.web.eventsrus.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** The text a person sees when the backend rejects a form (see BackendClient#messageFrom). */
class BackendErrorMessageTest {

    @Test
    void aValidationErrorShowsWhichRuleFailedNotJustValidationFailed() {
        Map<String, Object> body = Map.of(
                "message", "Validation failed",
                "fieldErrors", Map.of("businessTypes", "Select at least one business type"));

        assertThat(BackendClient.messageFrom(body))
                .isEqualTo("Validation failed: Select at least one business type");
    }

    @Test
    void aDefaultMessageWithoutASubjectGetsTheFieldNameInFront() {
        Map<String, Object> body = Map.of(
                "message", "Validation failed",
                "fieldErrors", Map.of("businessName", "must not be blank"));

        assertThat(BackendClient.messageFrom(body)).isEqualTo("Validation failed: Business name must not be blank");
    }

    @Test
    void severalFieldErrorsAreListedInAStableOrder() {
        Map<String, Object> body = Map.of(
                "message", "Validation failed",
                "fieldErrors", Map.of(
                        "phoneNumber", "Mobile number is required",
                        "ownerName", "Owner name is required"));

        assertThat(BackendClient.messageFrom(body))
                .isEqualTo("Validation failed: Owner name is required; Mobile number is required");
    }

    @Test
    void anErrorWithoutFieldErrorsKeepsItsOwnMessage() {
        assertThat(BackendClient.messageFrom(Map.of("message", "That promo code isn't valid.")))
                .isEqualTo("That promo code isn't valid.");
        assertThat(BackendClient.messageFrom(Map.of())).isEqualTo("Request to eventsrus-backend failed");
    }
}
