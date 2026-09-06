package com.web.eventsrus.model;

/**
 * Mirrors eventsrus-backend's enums.EventType exactly (same names), so the
 * value submitted here needs no translation once this form posts to the
 * real backend instead of a stub. label is a stub-only UI convenience -
 * the real enum has no associated display text either, same as BusinessType.
 */
public enum EventType {
    WEDDING("Wedding"),
    ANNIVERSARY("Anniversary"),
    BIRTHDAY("Birthday"),
    PARTY("Party"),
    OTHER("Other");

    private final String label;

    EventType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
