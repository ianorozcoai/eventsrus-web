package com.web.eventsrus.backend;

import com.web.eventsrus.model.BusinessTypeSupplierCount;
import java.util.List;

/** Mirrors eventsrus-backend's {@code AdminDashboardResponse}. */
public record BackendAdminDashboardStats(
        long plannerCount,
        long vendorCount,
        long vendorTicketCount,
        long newVendorTicketCount,
        long newPlannerTicketCount,
        long incompleteVendorSignupCount,
        List<BusinessTypeSupplierCount> supplierCountsByBusinessType) {}
