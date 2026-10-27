package com.web.eventsrus.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.web.eventsrus.model.BusinessType;
import com.web.eventsrus.model.SitemapEntry;
import com.web.eventsrus.model.VendorPublicProfile;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class StorefrontSeoTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    private VendorPublicProfile profile(boolean indexable, String description) {
        return new VendorPublicProfile(
                1L, 2L, "Sweet Moments", "Owner", description, "https://cdn.example.com/logo.png",
                List.of(BusinessType.CATERING), "Quezon City", "Metro Manila", "Philippines", "secret@example.com",
                "0917 000 0000", null, null, null, null, null, List.of(), false, List.of(), List.of(), 4.66, 3,
                null, List.of(), List.of(), List.of(), List.of(), List.of(), indexable);
    }

    @Test
    void indexablePageGetsCanonicalDescriptionAndStructuredDataWithoutContactDetails() {
        Map<String, Object> m = StorefrontSeo.attributes(profile(true, "Filipino buffet for weddings."), "sweet-moments", mapper);

        assertThat(m.get("seoIndexable")).isEqualTo(true);
        assertThat(m.get("seoTitle")).isEqualTo("Sweet Moments - Catering in Quezon City | EventsRUs");
        assertThat(m.get("seoUrl")).isEqualTo("https://eventsrus.app/vendor/storefront/sweet-moments");
        assertThat((String) m.get("seoDescription")).startsWith("Sweet Moments, catering in Quezon City. Filipino buffet");
        String json = (String) m.get("seoJsonLd");
        assertThat(json).contains("\"@type\":\"LocalBusiness\"", "\"addressLocality\":\"Quezon City\"", "\"ratingValue\":4.7", "\"reviewCount\":3");
        assertThat(json).doesNotContain("secret@example.com").doesNotContain("0917");
    }

    @Test
    void pageThatMayNotBeIndexedOnlyGetsATitle() {
        Map<String, Object> m = StorefrontSeo.attributes(profile(false, "Filipino buffet."), "sweet-moments", mapper);

        assertThat(m).containsOnlyKeys("seoTitle");
    }

    @Test
    void longDescriptionsAreTrimmedAndScriptEndTagsAreDefused() {
        Map<String, Object> m = StorefrontSeo.attributes(profile(true, "x".repeat(400) + " </script><b>"), "s", mapper);
        assertThat(((String) m.get("seoDescription")).length()).isLessThanOrEqualTo(155);

        Map<String, Object> n = StorefrontSeo.attributes(profile(true, "a </script> b"), "s", mapper);
        assertThat((String) n.get("seoJsonLd")).doesNotContain("</");
    }

    @Test
    void sitemapListsFixedPagesAndOnlyTheSuppliersGiven() {
        String xml = SitemapController.render(List.of(
                new SitemapEntry("sweet-moments", Instant.parse("2026-10-20T10:00:00Z")),
                new SitemapEntry(" ", null)));

        assertThat(xml).contains("<loc>https://eventsrus.app/</loc>", "<loc>https://eventsrus.app/terms</loc>",
                "<loc>https://eventsrus.app/vendor/storefront/sweet-moments</loc>", "<lastmod>2026-10-20</lastmod>");
        assertThat(xml.split("<url>").length - 1).isEqualTo(5);
    }
}
