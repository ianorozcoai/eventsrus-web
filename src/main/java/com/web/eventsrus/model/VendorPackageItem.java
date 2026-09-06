package com.web.eventsrus.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mirrors eventsrus-backend's {@code VendorPackageResponse} field-for-field,
 * plus one stub-only addition: imageCaptions. There's no per-package image
 * gallery concept in the backend yet (VendorPackage has no image column at
 * all) - these stand in as placeholder photo captions, same approach as the
 * storefront's own galleryCaptions, until real package image uploads exist.
 */
public record VendorPackageItem(
        long id,
        String name,
        String description,
        PackageType packageType,
        PackagePricingType pricingType,
        BigDecimal price,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        boolean active,
        List<String> imageCaptions) {}
