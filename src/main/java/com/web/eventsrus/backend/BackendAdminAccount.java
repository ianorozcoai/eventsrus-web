package com.web.eventsrus.backend;

import java.time.Instant;

/** Mirrors eventsrus-backend's dto.AdminAccountResponse - one row for the admin module's "Admins" page. */
public record BackendAdminAccount(String username, Instant createdAt) {}
