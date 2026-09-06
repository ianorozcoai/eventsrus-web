package com.web.eventsrus.model;

/**
 * Mirrors eventsrus-backend's enums.BusinessType exactly (same names), so
 * the value submitted here needs no translation once this form posts to
 * the real backend instead of a stub.
 */
public enum BusinessType {
    VENUE("Venue"),
    CATERING("Catering"),
    PHOTO_AND_VIDEO("Photo and Video"),
    PHOTO_BOOTHS("Photo Booths"),
    EVENT_COORDINATOR("Event Coordinator"),
    EVENT_HOST("Event Host"),
    ENTERTAINMENT("Entertainment"),
    PERFORMERS("Performers"),
    DECORATION_PRODUCTION("Decoration/Production"),
    LIGHTS_AND_SOUNDS("Lights and Sounds"),
    FOOD_CARTS_GRAZING("Food carts/Grazing"),
    SOUVENIR_GIVEAWAYS("Souvenir/Give Aways"),
    CAKE_AND_PASTRIES("Cake and Pastries"),
    INFLATABLES("Inflatables"),
    MOBILE_PLAYGROUND("Mobile Playground"),
    ARCADE("Arcade"),
    INVITATIONS("Invitations"),
    HAIR_AND_MAKEUP("Hair and Makeup"),
    BRIDAL_GOWN_DESIGNER("Bridal Gown Designer"),
    SUIT_RENTALS("Suit Rentals"),
    WARDROBE_STYLISTS_DRESSERS("Wardrobe Stylists & Dressers"),
    POWER_GENERATOR_SERVICES("Power & Generator Services"),
    LED_WALL_VISUAL_PROJECTION_RENTALS("LED Wall & Visual Projection Rentals"),
    STAGING_TRUSSING_FLOORING_RENTALS("Staging, Trussing, & Flooring Rentals"),
    TRANSPORT_SHUTTLE_FLEET_SERVICES("Transport, Shuttle, & Fleet Services"),
    SECURITY_CROWD_CONTROL("Security & Crowd Control"),
    INTERACTIVE_BAR_MIXOLOGY_SERVICES("Interactive Bar & Mixology Services"),
    LIVE_EVENT_PAINTERS_SKETCH_ARTISTS("Live Event Painters & Sketch Artists"),
    SPECIAL_EFFECTS("Special Effects"),
    FLORAL_SERVICES("Floral Services");

    private final String label;

    BusinessType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
