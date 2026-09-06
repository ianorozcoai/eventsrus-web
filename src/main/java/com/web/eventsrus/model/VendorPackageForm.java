package com.web.eventsrus.model;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mirrors eventsrus-backend's dto.VendorPackageRequest field-for-field
 * (name @NotBlank, packageType/pricingType @NotNull, description optional -
 * note there's no "active" field here either, since the real request DTO
 * doesn't expose it at creation time; a package is presumably active by
 * default and toggled through a separate endpoint that doesn't exist yet).
 * price/minPrice/maxPrice are all optional here since only one set is
 * actually used, depending on pricingType - the Add Package modal only
 * shows/requires whichever one applies.
 * A mutable POJO rather than a record - Thymeleaf's th:field="*{...}"
 * binding needs a no-arg constructor and setters.
 */
@Getter
@Setter
@NoArgsConstructor
public class VendorPackageForm {

    private String name;
    private String description;
    private PackageType packageType;
    // Defaults to FIXED so the Add Package modal's radio group and its
    // matching price field are pre-selected on first load.
    private PackagePricingType pricingType = PackagePricingType.FIXED;

    /** Used when pricingType == FIXED. */
    private BigDecimal price;

    /** Used when pricingType == RANGE. */
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
}
