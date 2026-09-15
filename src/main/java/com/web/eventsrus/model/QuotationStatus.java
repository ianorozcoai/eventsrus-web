package com.web.eventsrus.model;

/** Mirrors eventsrus-backend's {@code com.backend.eventsrus.enums.QuotationStatus}. */
public enum QuotationStatus {
    REQUEST_FOR_QUOTE,
    QUOTE_SENT,
    REVISION_REQUESTED,
    REVISION_SENT,
    QUOTE_ACCEPTED,
    PENDING_DEPOSIT,
    PAYMENT_REVIEW,
    PAYMENT_REJECTED,
    BOOKED,
    DECLINED
}
