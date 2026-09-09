package com.web.eventsrus.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Mirrors eventsrus-backend's real {@code EventResponse} field-for-field
 * (see EventService#toResponse) - the selected event's full detail (Overview
 * tab's AI idea text + matched-supplier suggestions). checklist is
 * deliberately not modeled here - the real DTO carries one, but nothing in
 * this app's UI surfaces it yet; Jackson just ignores that field.
 */
public record PlannerEvent(
        long id,
        String name,
        EventType eventType,
        LocalDate eventDate,
        String location,
        String description,
        String aiIdeaText,
        boolean saved,
        List<PlannerVendorSuggestion> suggestions) {}
