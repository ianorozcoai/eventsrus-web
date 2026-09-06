package com.web.eventsrus.model;

/** Mirrors eventsrus-backend's {@code VendorPaymentMethodResponse} field-for-field. */
public record VendorPaymentMethodItem(
        long id,
        String label,
        String qrImageUrl,
        PaymentMethodStatus status) {}
