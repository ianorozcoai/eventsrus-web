package com.web.eventsrus.model;

import java.util.List;

/** Mirrors eventsrus-backend's {@code CoordinatorHistoryResponse}. */
public record CoordinatorHistory(List<CoordinatorQuestion> questions, int questionsRemainingToday, int dailyLimit) {}
