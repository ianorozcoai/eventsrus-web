package com.web.eventsrus.model;

import java.time.Instant;

/**
 * A planner row in the admin module's Planner List (stubs/admin-planners.json) -
 * also doubles as the read-only profile view's data, since there's no
 * separate per-planner detail stub yet. Stub-only, same as everywhere else in
 * this app: there's no real planner directory on eventsrus-backend for the
 * admin module to call.
 */
public record AdminPlannerSummary(
        long id,
        String firstName,
        String lastName,
        String email,
        String mobileNumber,
        String city,
        String state,
        Instant joinedAt,
        int eventsCount) {}
