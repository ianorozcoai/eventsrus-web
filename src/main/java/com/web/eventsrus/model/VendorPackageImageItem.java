package com.web.eventsrus.model;

import java.time.Instant;
import java.util.List;

/** Mirrors eventsrus-backend's {@code VendorPackageImageResponse} - one real uploaded photo for a package. */
public record VendorPackageImageItem(long id, String imageUrl, String caption, Instant createdAt, List<String> tags) {}
