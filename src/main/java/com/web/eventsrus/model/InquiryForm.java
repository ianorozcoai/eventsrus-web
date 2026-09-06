package com.web.eventsrus.model;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mirrors eventsrus-backend's dto.SendMessageRequest (message only), plus
 * two stub-only additions - plannerName and targetDate. The real
 * SendMessageRequest doesn't carry these (the sender's identity comes from
 * auth on an existing conversation); a first-contact inquiry from the public
 * storefront needs them, so they're captured here and dropped from the
 * submission until the backend supports starting a conversation this way.
 * A mutable POJO rather than a record - Thymeleaf's th:field="*{...}"
 * binding needs a no-arg constructor and setters.
 */
@Getter
@Setter
@NoArgsConstructor
public class InquiryForm {

    private String plannerName;
    private LocalDate targetDate;
    private String message;
}
