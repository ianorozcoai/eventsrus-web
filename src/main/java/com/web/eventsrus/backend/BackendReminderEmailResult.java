package com.web.eventsrus.backend;

/** Mirrors eventsrus-backend's {@code IncompleteSignupReminderService.Result}; outcome is its enum name. */
public record BackendReminderEmailResult(Long userId, String email, String outcome) {}
