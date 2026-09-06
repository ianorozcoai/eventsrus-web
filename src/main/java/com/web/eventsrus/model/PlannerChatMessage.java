package com.web.eventsrus.model;

import java.time.Instant;

/**
 * One turn in a planner's ongoing chat with the AI coordinator on an
 * event's Overview tab. Session-held (PlannerController), seeded from that
 * PlannerEvent's own description/coordinatorReply as the first two turns,
 * then appended to as the planner sends follow-ups
 * (PlannerCoordinatorService#followUpReply).
 */
public record PlannerChatMessage(boolean plannerMessage, String body, Instant createdAt) {}
