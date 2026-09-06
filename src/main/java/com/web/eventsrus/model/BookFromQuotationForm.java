package com.web.eventsrus.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mirrors eventsrus-backend's dto.BookingFromQuotationRequest field-for-field
 * (price, eventDatetime, agreementDetails) - eventDatetime is captured here
 * as a plain LocalDate since there's no time-of-day concept on the
 * intake/quotation forms elsewhere in this stub app either.
 * A mutable POJO rather than a record - Thymeleaf's th:field="*{...}"
 * binding needs a no-arg constructor and setters.
 */
@Getter
@Setter
@NoArgsConstructor
public class BookFromQuotationForm {

    private BigDecimal price;
    private LocalDate eventDatetime;
    private String agreementDetails;
}
