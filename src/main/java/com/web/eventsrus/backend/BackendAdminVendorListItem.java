package com.web.eventsrus.backend;

import com.web.eventsrus.model.BusinessType;
import java.time.Instant;
import java.util.List;

/**
 * Mirrors eventsrus-backend's {@code AdminVendorListItemResponse} - one row
 * of the verification review queue AND the Vendors directory page (both
 * admin views are every vendor, just displayed differently).
 */
public record BackendAdminVendorListItem(
        Long vendorUserId,
        String businessName,
        String ownerName,
        String contactEmail,
        String phoneNumber,
        List<BusinessType> businessTypes,
        String slug,
        String city,
        String state,
        List<String> operatingAreas,
        boolean hasIdCard,
        boolean hasSelfie,
        int legalDocumentCount,
        boolean verified,
        Instant verifiedAt,
        String verifiedByAdmin,
        boolean topVendor,
        Instant createdAt,
        long referralCount,
        boolean fakeAccount,
        long bookingCount,
        Instant lastLoginAt,
        // Null means never subscribed at all - distinct from a real
        // PAYPAL/GCASH subscription that lapsed, which is what the
        // "Payment Overdue" tab on admin/vendors.html flags (see
        // AdminController#vendors).
        String billingSource,
        boolean planExpired,
        boolean planInGracePeriod,
        Instant planOverdueSince) {

    /** True for a vendor who has a real (non-free) subscription that's currently lapsed or in its grace period. */
    public boolean isPaymentOverdue() {
        return (planExpired || planInGracePeriod) && ("PAYPAL".equals(billingSource) || "GCASH".equals(billingSource));
    }
}
