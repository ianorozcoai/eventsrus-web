package com.web.eventsrus.model;

import java.time.Instant;

/** Mirrors eventsrus-backend's {@code LeadResponse}. */
public record VendorLead(
        long id,
        long plannerUserId,
        String plannerName,
        long eventId,
        String eventName,
        Instant firstVisitedAt,
        Instant lastVisitedAt) {}
