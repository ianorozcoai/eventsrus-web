package com.web.eventsrus.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.web.eventsrus.model.AdminPlannerSummary;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.StringTemplateResolver;

/**
 * The admin pages show a "Check this account" tag using exactly this snippet
 * (vendors.html and planners.html). Rendering it here with the real records
 * proves the expressions work, including when an older backend sends no
 * suspiciousReasons at all.
 */
class SuspiciousTagTemplateTest {

    private static final String SNIPPET = "<span th:if=\"${p.isSuspicious()}\" "
            + "th:title=\"${#strings.listJoin(p.suspiciousReasons(), '; ')}\">Check this account</span>"
            + "<i th:text=\"'From: ' + ${p.signupSourceLabel()}\"></i>";

    private static String render(Object planner) {
        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(new StringTemplateResolver());
        Context ctx = new Context();
        ctx.setVariable("p", planner);
        return engine.process(SNIPPET, ctx);
    }

    private static AdminPlannerSummary planner(List<String> reasons) {
        return new AdminPlannerSummary(1L, "Jamie", "Cruz", "a@b.com", "5551234567", "Manila", "NCR",
                Instant.parse("2026-11-08T00:00:00Z"), 0, "ANDROID", reasons);
    }

    @Test
    void showsTheTagWithTheReasonsAndTheSource() {
        String html = render(planner(List.of("Phone number starts with 555", "Email looks machine-made")));

        assertThat(html).contains("Check this account");
        assertThat(html).contains("Phone number starts with 555; Email looks machine-made");
        assertThat(html).contains("From: Android app");
    }

    @Test
    void showsNoTagWhenNothingLooksOff() {
        assertThat(render(planner(List.of()))).doesNotContain("Check this account");
    }

    @Test
    void showsNoTagAndDoesNotFailWhenAnOlderBackendSendsNoReasons() {
        assertThat(render(planner(null))).doesNotContain("Check this account");
    }
}
