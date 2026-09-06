package com.web.eventsrus.model;

import java.time.LocalDate;
import java.util.List;

/**
 * One entry in the planner's "My Events" sidebar, plus everything the
 * Overview tab needs. Mirrors what eventsrus-backend's real EventResponse
 * already carries (id/name/eventType/eventDate/location/description/
 * aiIdeaText/suggestions - see EventService#toResponse), except sourced
 * from the session-held list PlannerController seeds from
 * stubs/planner-events.json rather than a live call - see
 * PlannerCoordinatorService for how a freshly-submitted event gets these
 * same fields filled in.
 */
public record PlannerEvent(
        long id,
        String name,
        EventType eventType,
        LocalDate eventDate,
        String location,
        String description,
        String coordinatorReply,
        List<PlannerVendorSuggestion> suggestedSuppliers) {}
