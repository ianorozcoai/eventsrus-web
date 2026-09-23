package com.web.eventsrus.backend;

import java.time.Instant;

/** One entry in a vendor's GCash review timeline - see BackendSubscriptionPayment#history. */
public record BackendGcashHistoryEntry(String eventType, String note, Instant occurredAt) {}
