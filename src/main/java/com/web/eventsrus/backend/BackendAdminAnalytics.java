package com.web.eventsrus.backend;

import java.util.List;

/** Mirrors eventsrus-backend's {@code AdminAnalyticsController.AnalyticsResponse}. */
public record BackendAdminAnalytics(
        int days,
        Totals totals,
        Funnel funnel,
        List<DayCount> signupsByDay,
        List<Labelled> signupsBySource,
        List<Labelled> suppliersByCity,
        List<Labelled> suppliersByBusinessType) {

    public record DayCount(String date, long planners, long suppliers) {}

    public record Labelled(String label, long count) {}

    public record Totals(
            long planners, long suppliers, long suppliersWithListing, long suppliersVerified,
            long proSuppliers, long active7d, long active30d,
            long onFreeAccessOffer, long reachedFirstBookings) {}

    public record Funnel(
            long signups, long suppliers, long suppliersWithListing,
            long planners, long plannersWithEvent, long plannersWhoRequestedQuote,
            long events, long leads, long quotations, long bookings) {}
}
