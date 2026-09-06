package com.web.eventsrus.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mirrors eventsrus-backend's {@code CreateTicketRequest} field-for-field.
 * A mutable POJO rather than a record - Thymeleaf's th:field="*{...}"
 * binding needs a no-arg constructor and setters.
 */
@Getter
@Setter
@NoArgsConstructor
public class CreateTicketForm {

    private String subject;
    private TicketCategory category;
    private String message;
    private Long relatedEventId;
}
