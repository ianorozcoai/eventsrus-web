package com.web.eventsrus.model;

import java.time.Instant;

/** Mirrors eventsrus-backend's {@code CoordinatorQuestionResponse} - one Q&A exchange with the Events Coordinator. */
public record CoordinatorQuestion(long id, String question, String answer, Instant createdAt) {}
