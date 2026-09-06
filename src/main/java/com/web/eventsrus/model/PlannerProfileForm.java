package com.web.eventsrus.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mirrors eventsrus-backend's dto.UpdateProfileRequest field-for-field
 * (firstName/lastName/email/mobileNumber @NotBlank there, address fields
 * optional). Stub-only round trip for now - PlannerController#updateProfile
 * doesn't call the real PUT /api/v1/users/me/profile endpoint yet, same
 * reasoning as every other planner-facing form in this app: there's no real
 * planner login in eventsrus-web to call it with.
 * A mutable POJO rather than a record - Thymeleaf's th:field="*{...}"
 * binding needs a no-arg constructor and setters.
 */
@Getter
@Setter
@NoArgsConstructor
public class PlannerProfileForm {

    private String firstName;
    private String lastName;
    private String email;
    private String mobileNumber;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
}
