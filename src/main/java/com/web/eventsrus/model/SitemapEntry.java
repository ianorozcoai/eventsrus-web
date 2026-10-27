package com.web.eventsrus.model;

import java.time.Instant;

/** One public storefront the backend says search engines may list (see /api/v1/vendors/sitemap). */
public record SitemapEntry(String slug, Instant lastModified) {}
