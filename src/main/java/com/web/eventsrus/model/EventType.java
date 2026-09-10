package com.web.eventsrus.model;

/**
 * Mirrors eventsrus-backend's enums.EventType exactly (same names), so the
 * value submitted here needs no translation once this form posts to the
 * real backend instead of a stub. label is a UI convenience only - the
 * real enum has no associated display text, same as BusinessType.
 */
public enum EventType {
    WEDDING("Wedding"),
    ANNIVERSARY("Anniversary"),
    BIRTHDAY("Birthday"),
    PARTY("Party"),
    BABY_SHOWER("Baby Shower"),
    BRIDAL_SHOWER("Bridal Shower"),
    BACHELOR_PARTY("Bachelor Party"),
    BACHELORETTE_PARTY("Bachelorette Party"),
    ENGAGEMENT_PARTY("Engagement Party"),
    GRADUATION("Graduation"),
    RETIREMENT("Retirement"),
    REUNION("Reunion"),
    HOUSEWARMING("Housewarming"),
    SEMINAR("Seminar"),
    NETWORKING_EVENT("Networking Event"),
    PRODUCT_LAUNCH("Product Launch"),
    TEAM_BUILDING("Team Building"),
    CORPORATE_RETREAT("Corporate Retreat"),
    TRADE_SHOW("Trade Show"),
    GALA("Gala"),
    FUNDRAISER("Fundraiser"),
    CONCERT("Concert"),
    FESTIVAL("Festival"),
    EXHIBITION("Exhibition"),
    SPORTS_EVENT("Sports Event"),
    OTHER("Other");

    private final String label;

    EventType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
