package com.web.eventsrus.model;

import java.time.Instant;

/** Mirrors eventsrus-backend's {@code GoogleCalendarStatusResponse} field-for-field. */
public record GoogleCalendarStatusItem(boolean connected, Instant connectedAt) {}
