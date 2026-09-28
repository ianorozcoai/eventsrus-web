package com.web.eventsrus.model;

import java.time.Instant;
import java.util.List;

/** Mirrors eventsrus-backend's {@code VendorTaggedImageResponse} field-for-field. */
public record VendorTaggedImageItem(
        long id,
        String imageUrl,
        String caption,
        Instant createdAt,
        ImageSource source,
        Long packageId,
        String packageName,
        List<VendorImageTagItem> tags) {}
