package com.web.eventsrus.model;

import java.time.Instant;
import java.util.List;

/**
 * Mirrors eventsrus-backend's {@code CalendarEntryResponse} (vendor side)
 * field-for-field, plus one stub-only addition: endDatetime. The real DTO
 * only has a single eventDatetime (bookings are single points in time) -
 * this stands in for multi-day spans (e.g. setup/teardown) until the
 * backend supports them. Null means single-day, same as before.
 */
public record VendorCalendarEntry(
        long eventId,
        String eventName,
        Instant eventDatetime,
        Instant endDatetime,
        String status,
        List<String> vendorNames,
        String plannerName) {}
