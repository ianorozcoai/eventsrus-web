package com.web.eventsrus.backend;

/** Mirrors eventsrus-backend's {@code AdminDashboardResponse}. */
public record BackendAdminDashboardStats(
        long plannerCount,
        long vendorCount,
        long vendorTicketCount,
        long newVendorTicketCount,
        long newPlannerTicketCount,
        long incompleteVendorSignupCount) {}
