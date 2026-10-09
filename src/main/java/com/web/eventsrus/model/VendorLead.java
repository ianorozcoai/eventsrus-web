package com.web.eventsrus.model;

import java.time.Instant;
import java.time.LocalDate;

/** Mirrors eventsrus-backend's {@code LeadResponse}. */
public record VendorLead(
        long id,
        long plannerUserId,
        String plannerName,
        long eventId,
        String eventName,
        /** Null when the planner never set a date for this event - not mandatory. */
        LocalDate eventDate,
        Instant firstVisitedAt,
        Instant lastVisitedAt) {}
