package com.web.eventsrus.model;

import java.math.BigDecimal;

/**
 * Mirrors eventsrus-backend's VendorDashboardResponse field-for-field, so
 * this record can deserialize either the local JSON stub (for now) or the
 * real backend response (later) without any changes.
 */
public record VendorDashboard(
        long newLeadsCount,
        long newMessagesCount,
        long newQuotationsCount,
        long newBookingsCount,
        long upcomingEventsCount,
        BigDecimal totalIncome,
        long cancellationsCount,
        boolean hasPackages) {
}
