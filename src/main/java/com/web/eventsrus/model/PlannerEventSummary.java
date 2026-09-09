package com.web.eventsrus.model;

import java.time.LocalDate;

/**
 * Mirrors eventsrus-backend's real {@code EventSummaryResponse} - one row
 * in the planner's sidebar "My Events" list (see EventService#listEvents).
 * Deliberately a separate, smaller shape from PlannerEvent (the full
 * per-event detail) - the real GET /api/v1/events endpoint this comes from
 * doesn't carry location/description/aiIdeaText/suggestions, only enough to
 * render the sidebar.
 */
public record PlannerEventSummary(long id, String name, EventType eventType, LocalDate eventDate, boolean saved) {}
