package com.web.eventsrus.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.StringTemplateResolver;

/** Same expression as the Location cell in admin/planners.html. */
class PlannerLocationTemplateTest {

    private static final String CELL = "<td th:with=\"c=${city}, s=${state}\" "
            + "th:text=\"${#strings.isEmpty(c) and #strings.isEmpty(s)} ? 'No location' : "
            + "(${#strings.isEmpty(c)} ? ${s} : (${#strings.isEmpty(s)} ? ${c} : ${c} + ', ' + ${s}))\"></td>";

    private static String render(String city, String state) {
        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(new StringTemplateResolver());
        Context ctx = new Context();
        ctx.setVariable("city", city);
        ctx.setVariable("state", state);
        return engine.process(CELL, ctx);
    }

    @Test
    void bothMissingShowsNoLocation() {
        assertThat(render(null, null)).contains("No location");
        assertThat(render("", " ".trim())).contains("No location");
    }

    @Test
    void oneMissingShowsTheOther() {
        assertThat(render("Manila", null)).contains(">Manila<");
        assertThat(render(null, "NCR")).contains(">NCR<");
    }

    @Test
    void bothPresentShowsCityAndState() {
        assertThat(render("Manila", "NCR")).contains("Manila, NCR");
    }
}
