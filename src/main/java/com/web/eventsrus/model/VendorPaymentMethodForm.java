package com.web.eventsrus.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mirrors eventsrus-backend's dto.VendorPaymentMethodRequest field-for-field
 * (label @NotBlank). The QR image itself isn't a field here, same reasoning
 * as VendorPackageForm/VendorSettingsForm - Thymeleaf's th:field can't bind
 * a file input, so the controller takes it as a separate MultipartFile
 * request param.
 * A mutable POJO rather than a record - Thymeleaf's th:field="*{...}"
 * binding needs a no-arg constructor and setters.
 */
@Getter
@Setter
@NoArgsConstructor
public class VendorPaymentMethodForm {

    private String label;
}
