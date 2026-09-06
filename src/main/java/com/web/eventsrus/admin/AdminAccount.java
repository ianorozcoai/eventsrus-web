package com.web.eventsrus.admin;

import java.time.Instant;

/**
 * An admin login - username + BCrypt password hash. Held in-memory only (see
 * AdminAccountService); nothing here is persisted to a database, so accounts
 * created through the "Admins" page survive for the life of the running app
 * but are gone on restart, seeded fresh each time from the one master login.
 */
public record AdminAccount(long id, String username, String passwordHash, Instant createdAt) {}
