package com.web.eventsrus.stub;

import com.web.eventsrus.model.PlannerEvent;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * The planner's session-held "previous events" list - shared between
 * PlannerController (the events shell) and PlannerProfileController, so
 * the sidebar's event list stays consistent no matter which planner page
 * is open. Seeded from stubs/planner-events.json the first time it's
 * touched in a session, then held there for the rest of the visit - a
 * deliberate middle ground, not real persistence (see PlannerController's
 * own class Javadoc for the full reasoning).
 */
@Service
public class PlannerEventSessionService {

    private static final String SESSION_EVENTS_KEY = "plannerEvents";

    private final StubDataService stubDataService;

    public PlannerEventSessionService(StubDataService stubDataService) {
        this.stubDataService = stubDataService;
    }

    @SuppressWarnings("unchecked")
    public List<PlannerEvent> events(HttpSession session) {
        List<PlannerEvent> events = (List<PlannerEvent>) session.getAttribute(SESSION_EVENTS_KEY);
        if (events == null) {
            events = new ArrayList<>(stubDataService.loadList("planner-events.json", PlannerEvent.class));
            session.setAttribute(SESSION_EVENTS_KEY, events);
        }
        return events;
    }
}
