package com.web.eventsrus.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.web.eventsrus.backend.BackendReminderEmailResult;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The plain-language message the admin sees after pressing an "Email reminder" button. */
class AdminReminderSummaryTest {

    private static BackendReminderEmailResult r(String outcome) {
        return new BackendReminderEmailResult(1L, "a@example.com", outcome);
    }

    @Test
    void oneSentEmail() {
        assertThat(AdminController.reminderSummary(List.of(r("SENT")))).isEqualTo("1 reminder email sent.");
    }

    @Test
    void explainsWhySomeoneWasSkipped() {
        String text = AdminController.reminderSummary(List.of(r("SENT"), r("SENT"), r("TOO_SOON"), r("LIMIT_REACHED"), r("NOT_ELIGIBLE")));

        assertThat(text).isEqualTo("2 reminder emails sent. 1 skipped: emailed less than 7 days ago."
                + " 1 skipped: already got 3 reminders. 1 skipped: no longer an incomplete sign-up.");
    }

    @Test
    void saysSoWhenEmailIsSwitchedOffOrNobodyIsThere() {
        assertThat(AdminController.reminderSummary(List.of(r("EMAIL_OFF")))).contains("switched off");
        assertThat(AdminController.reminderSummary(List.of())).isEqualTo("There is nobody to email.");
    }
}
