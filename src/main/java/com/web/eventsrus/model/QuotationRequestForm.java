package com.web.eventsrus.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

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
    // Forces ISO (yyyy-MM-dd) formatting for th:field's rendered value -
    // without this, Thymeleaf's ad-hoc bind status on a plain GET render
    // (no @ModelAttribute/BindingResult involved, unlike a form re-render
    // after a failed POST) falls back to locale-based formatting
    // (e.g. "10/10/2026"), which an <input type="date"> silently rejects
    // and shows blank - exactly the bug reported with the storefront's
    // event-date prefill.
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate targetDate;
    private List<Long> packageIds = new ArrayList<>();
    private String message;
}
