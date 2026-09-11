package com.web.eventsrus.backend;

import java.time.Instant;

/** Mirrors eventsrus-backend's {@code AdminIncompleteVendorSignupResponse}. */
public record BackendIncompleteVendorSignup(
        Long id,
        String firstName,
        String lastName,
        String email,
        String mobileNumber,
        Instant signedUpAt) {}
