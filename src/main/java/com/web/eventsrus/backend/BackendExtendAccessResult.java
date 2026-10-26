package com.web.eventsrus.backend;

import java.time.Instant;

/** Mirrors eventsrus-backend's AdminExtendAccessResponse: where the supplier's access now ends. */
public record BackendExtendAccessResult(Long vendorUserId, int daysAdded, Instant accessEndsAt, String billingSource) {
}
