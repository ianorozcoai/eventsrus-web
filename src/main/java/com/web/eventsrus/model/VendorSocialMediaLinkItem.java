package com.web.eventsrus.model;

/** Mirrors eventsrus-backend's {@code VendorSocialMediaLinkResponse} field-for-field. */
public record VendorSocialMediaLinkItem(long id, SocialMediaPlatform platform, String url) {}
