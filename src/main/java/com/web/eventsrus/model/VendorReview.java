package com.web.eventsrus.model;

import java.time.Instant;
import java.time.LocalDate;

/** Mirrors eventsrus-backend's {@code ReviewResponse} - one planner review of a completed booking. */
public record VendorReview(
        long id,
        int rating,
        String comment,
        Instant createdAt,
        Instant updatedAt,
        boolean hidden,
        String reviewerName,
        String eventName,
        LocalDate eventDate) {}
