package com.web.eventsrus.model;

import java.math.BigDecimal;
import java.util.List;

/** Mirrors eventsrus-backend's {@code VendorPackageResponse} field-for-field, including real per-package photos. */
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
        List<VendorPackageImageItem> images) {}
