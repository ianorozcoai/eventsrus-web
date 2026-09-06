package com.web.eventsrus.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mirrors eventsrus-backend's dto.QuotationRequest field-for-field
 * (plannerName, targetDate, message, packageIds) - the real backend now
 * supports flagging more than one package per quotation request (same
 * multi-select concept as operatingAreas/cateredEventTypes on the vendor
 * settings/onboarding forms), so this stub's packageIds submits as-is once
 * this page calls the real endpoint instead of simulating it.
 * A mutable POJO rather than a record - Thymeleaf's th:field="*{...}"
 * binding needs a no-arg constructor and setters.
 */
@Getter
@Setter
@NoArgsConstructor
public class QuotationRequestForm {

    private String plannerName;
    private LocalDate targetDate;
    private List<Long> packageIds = new ArrayList<>();
    private String message;
}
