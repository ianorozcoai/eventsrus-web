package com.web.eventsrus.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

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
    DEBUT("Debut"),
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
    CORPORATE_EVENT("Corporate Event"),
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

    // The four most common personal-celebration types, always on top (in
    // this order), everything else alphabetical by label after them, and
    // OTHER always trailing as the catch-all - this is the one order every
    // dropdown/list in the app should use (see displayOrder()), not enum
    // declaration order or values(). Corporate Event isn't pinned - it
    // just sorts alphabetically with the rest.
    private static final List<EventType> PINNED = List.of(ANNIVERSARY, BIRTHDAY, DEBUT, WEDDING);

    /**
     * The order every event-type dropdown/list in the app should render in:
     * {@link #PINNED} first (in that fixed order), then every other type
     * (except {@link #OTHER}) alphabetical by label, then OTHER last as the
     * catch-all. Use this instead of {@link #values()} wherever EventType
     * is shown to a user.
     */
    public static List<EventType> displayOrder() {
        List<EventType> middle = Arrays.stream(values())
                .filter(t -> t != OTHER && !PINNED.contains(t))
                .sorted(Comparator.comparing(EventType::getLabel))
                .toList();
        List<EventType> ordered = new ArrayList<>(PINNED);
        ordered.addAll(middle);
        ordered.add(OTHER);
        return ordered;
    }
}
