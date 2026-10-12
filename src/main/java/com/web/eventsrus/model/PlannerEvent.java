package com.web.eventsrus.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Mirrors eventsrus-backend's real {@code EventResponse} field-for-field
 * (see EventService#toResponse) - the selected event's full detail (Overview
 * tab's AI idea text, matched-supplier suggestions, and checklist).
 */
public record PlannerEvent(
        long id,
        String name,
        EventType eventType,
        LocalDate eventDate,
        String location,
        String description,
        BigDecimal budget,
        Integer guestCount,
        String aiIdeaText,
        boolean saved,
        List<PlannerVendorSuggestion> suggestions,
        List<PlannerChecklistItem> checklist,
        List<Long> bookmarkedVendorProfileIds) {}
