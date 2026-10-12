package com.web.eventsrus.model;

import java.util.Set;

/** Mirrors eventsrus-backend's real {@code CategoryRecommendationResponse} - essential vs. optional supplier categories for one EventType, with no event required to exist yet. */
public record CategoryRecommendation(Set<BusinessType> essential, Set<BusinessType> optional) {}
