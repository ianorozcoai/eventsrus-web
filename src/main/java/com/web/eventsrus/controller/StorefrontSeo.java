package com.web.eventsrus.controller;

import com.web.eventsrus.model.BusinessType;
import com.web.eventsrus.model.VendorPublicProfile;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.ObjectMapper;

/**
 * Search/share tags for a public supplier storefront. Only a page the backend marks searchIndexable
 * (supplier opted in, complete profile, active plan) gets indexable tags; every other storefront view keeps the
 * default noindex from fragments/common. The structured data deliberately leaves out phone and email.
 */
final class StorefrontSeo {

    static final String SITE = "https://eventsrus.app";
    private static final String DEFAULT_IMAGE = SITE + "/assets/images/new-logo-login-fb.jpg";

    private StorefrontSeo() {}

    /** Model attributes read by fragments/common (head) and fragments/seo (supplier). */
    static Map<String, Object> attributes(VendorPublicProfile p, String slug, ObjectMapper objectMapper) {
        Map<String, Object> m = new LinkedHashMap<>();
        String category = firstCategory(p.businessTypes());
        String where = isBlank(p.city()) ? "" : " in " + p.city().trim();
        String title = p.businessName() + (category.isEmpty() ? "" : " - " + category + where) + " | EventsRUs";
        m.put("seoTitle", title);
        if (!Boolean.TRUE.equals(p.searchIndexable())) {
            return m;
        }
        String url = SITE + "/vendor/storefront/" + slug;
        String image = p.logoImageUrl() != null && p.logoImageUrl().startsWith("https://") ? p.logoImageUrl() : DEFAULT_IMAGE;
        String description = description(p, category, where);
        m.put("seoIndexable", true);
        m.put("seoUrl", url);
        m.put("seoDescription", description);
        m.put("seoImage", image);
        m.put("seoJsonLd", jsonLd(p, url, image, description, category, objectMapper));
        return m;
    }

    static String description(VendorPublicProfile p, String category, String where) {
        String lead = p.businessName() + (category.isEmpty() ? "" : ", " + category.toLowerCase()) + where + ". ";
        String body = p.description() == null ? "" : p.description().replaceAll("\s+", " ").trim();
        String full = lead + body;
        return full.length() <= 155 ? full : full.substring(0, 154).trim() + "…";
    }

    private static String jsonLd(VendorPublicProfile p, String url, String image, String description, String category,
            ObjectMapper objectMapper) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("@context", "https://schema.org");
        node.put("@type", "LocalBusiness");
        node.put("name", p.businessName());
        node.put("url", url);
        node.put("description", description);
        node.put("image", image);
        if (!category.isEmpty()) {
            node.put("additionalType", category);
        }
        Map<String, Object> address = new LinkedHashMap<>();
        address.put("@type", "PostalAddress");
        address.put("addressLocality", p.city());
        if (!isBlank(p.state())) {
            address.put("addressRegion", p.state());
        }
        address.put("addressCountry", "PH");
        node.put("address", address);
        if (p.averageRating() != null && p.reviewCount() > 0) {
            Map<String, Object> rating = new LinkedHashMap<>();
            rating.put("@type", "AggregateRating");
            rating.put("ratingValue", Math.round(p.averageRating() * 10) / 10.0);
            rating.put("reviewCount", p.reviewCount());
            node.put("aggregateRating", rating);
        }
        // Inside a <script> tag: stop a stray "</script>" in supplier text from ending it early.
        return objectMapper.writeValueAsString(node).replace("</", "<\\/");
    }

    private static String firstCategory(List<BusinessType> types) {
        return types == null || types.isEmpty() ? "" : types.get(0).getLabel();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
