package com.web.eventsrus.backend;

import java.time.Instant;

/** Mirrors eventsrus-backend's dto.AuthResponse field-for-field. plan is null when there's no live subscription. */
public record BackendAuthResponse(
        Long id,
        String token,
        String tokenType,
        long expiresIn,
        String role,
        String firstName,
        String email,
        String plan,
        Instant planExpiresAt) {}
