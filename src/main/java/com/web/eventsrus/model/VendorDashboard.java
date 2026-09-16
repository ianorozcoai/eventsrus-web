package com.web.eventsrus.model;

import java.math.BigDecimal;

/** Mirrors eventsrus-backend's VendorDashboardResponse field-for-field. */
public record VendorDashboard(
        long newLeadsCount,
        long newInquiriesCount,
        long newQuotationsCount,
        long newBookingsCount,
        long bookingsNeedingActionCount,
        long upcomingEventsCount,
        BigDecimal totalIncome,
        long cancellationsCount,
        boolean hasPackages,
        long quotationsUnseenCount,
        long bookingsUnseenCount) {
}
