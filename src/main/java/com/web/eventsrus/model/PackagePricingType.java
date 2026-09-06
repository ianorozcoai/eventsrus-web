package com.web.eventsrus.model;

/** Mirrors eventsrus-backend's {@code com.backend.eventsrus.enums.PackagePricingType}. */
public enum PackagePricingType {
    FIXED("Fixed Price"),
    RANGE("Price Range"),
    QUOTE("Request for Quotation");

    private final String label;

    PackagePricingType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
