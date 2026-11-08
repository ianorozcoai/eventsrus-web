package com.web.eventsrus.backend;

import java.time.Instant;

/** Mirrors eventsrus-backend's {@code AdminIncompleteVendorSignupResponse}. */
public record BackendIncompleteVendorSignup(
        Long id,
        String firstName,
        String lastName,
        String email,
        String mobileNumber,
        Instant signedUpAt,
        Integer reminderCount,
        Instant lastReminderAt,
        String signupSource,
        java.util.List<String> suspiciousReasons) {

    public boolean isSuspicious() {
        return suspiciousReasons != null && !suspiciousReasons.isEmpty();
    }

    public String signupSourceLabel() {
        return com.web.eventsrus.model.SignupSourceLabels.label(signupSource);
    }
}
