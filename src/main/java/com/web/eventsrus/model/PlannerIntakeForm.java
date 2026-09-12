package com.web.eventsrus.model;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Mirrors eventsrus-backend's dto.CreateEventRequest field-for-field
 * (eventType @NotNull, everything else optional), plus one stub-only
 * addition: name. The real event-name field only exists on the separate
 * SaveEventRequest DTO (@NotBlank there, sent in a later "save this event"
 * step) - captured here upfront instead, since this intake form combines
 * what the real flow eventually splits into create-then-save.
 * A mutable POJO rather than a record - Thymeleaf's th:field="*{...}"
 * binding needs a no-arg constructor and setters.
 */
@Getter
@Setter
@NoArgsConstructor
public class PlannerIntakeForm {

    private String name;
    private EventType eventType;
    // Preventive, not a fix for a live bug - this form is always blank
    // today, but see QuotationRequestForm#targetDate for the actual bug
    // this heads off the moment anything ever pre-fills this field
    // (a future "duplicate event" feature, say).
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate eventDate;
    private String location;
    private String description;
}
