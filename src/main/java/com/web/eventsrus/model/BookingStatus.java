package com.web.eventsrus.model;

/** Mirrors eventsrus-backend's {@code com.backend.eventsrus.enums.BookingStatus}. */
public enum BookingStatus {
    PROPOSED,
    APPROVED,
    DECLINED,
    CANCELLED,
    AWAITING_PAYMENT,
    PAYMENT_SUBMITTED,
    BOOKED,
    PAYMENT_REJECTED
}
