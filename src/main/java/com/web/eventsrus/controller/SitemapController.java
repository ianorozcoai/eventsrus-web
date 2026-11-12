package com.web.eventsrus.controller;

import com.web.eventsrus.backend.BackendClient;
import com.web.eventsrus.model.SitemapEntry;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * sitemap.xml: the fixed public pages plus every supplier storefront the backend says is search-indexable
 * (supplier opted in, complete profile, active plan). The supplier list is cached for an hour; if the backend
 * cannot be reached the fixed pages are still served.
 */
@Controller
public class SitemapController {

    private static final Logger log = LoggerFactory.getLogger(SitemapController.class);
    static final String SITE = "https://eventsrus.app";
    private static final Duration TTL = Duration.ofHours(1);

    private final BackendClient backendClient;
    private volatile List<SitemapEntry> cached = List.of();
    private volatile Instant cachedAt = Instant.EPOCH;

    public SitemapController(BackendClient backendClient) {
        this.backendClient = backendClient;
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(30)).cachePublic())
                .body(render(suppliers()));
    }

    private List<SitemapEntry> suppliers() {
        if (Instant.now().isBefore(cachedAt.plus(TTL))) {
            return cached;
        }
        try {
            cached = backendClient.listSitemapEntries();
            cachedAt = Instant.now();
        } catch (RuntimeException e) {
            log.warn("sitemap: could not load supplier pages, serving what we have", e);
            cachedAt = Instant.now().minus(TTL).plusSeconds(60);
        }
        return cached;
    }

    static String render(List<SitemapEntry> suppliers) {
        String today = LocalDate.now(ZoneOffset.UTC).toString();
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        url(xml, SITE + "/", today, "1.0");
        url(xml, SITE + "/terms", "2026-10-27", "0.3");
        url(xml, SITE + "/privacy", "2026-10-27", "0.3");
        url(xml, SITE + "/delete-account", "2026-10-27", "0.2");
        for (SitemapEntry s : suppliers) {
            if (s.slug() == null || s.slug().isBlank()) {
                continue;
            }
            String lastmod = s.lastModified() == null ? today : s.lastModified().atZone(ZoneOffset.UTC).toLocalDate().toString();
            url(xml, SITE + "/vendor/storefront/" + escape(s.slug()), lastmod, "0.7");
        }
        return xml.append("</urlset>\n").toString();
    }

    private static void url(StringBuilder xml, String loc, String lastmod, String priority) {
        xml.append("  <url><loc>").append(loc).append("</loc><lastmod>").append(lastmod)
                .append("</lastmod><priority>").append(priority).append("</priority></url>\n");
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
