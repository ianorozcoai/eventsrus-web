package com.web.eventsrus.admin;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Website visitors for the admin Analytics page, read from Cloudflare Web Analytics
 * (same source the NailSwipe dashboard uses). Needs a read-only API token; when it is
 * missing or Cloudflare errors, the page shows a plain notice instead of failing.
 * Results are cached for 5 minutes so reloading the page does not hammer the API.
 */
@Service
public class CloudflareTrafficService {

    private static final Logger log = LoggerFactory.getLogger(CloudflareTrafficService.class);
    private static final ZoneId MANILA = ZoneId.of("Asia/Manila");
    private static final long CACHE_MS = 5 * 60 * 1000;

    public record Day(String date, long visits, long phVisits) {}
    public record Item(String label, long visits, long views) {}
    public record Traffic(
            boolean configured, String error, long visits, long phVisits, long phViews,
            List<Day> days, List<Item> countries, List<Item> phPages,
            List<Item> phReferrers, List<Item> phDevices) {
        static Traffic notConfigured(String why) {
            return new Traffic(false, why, 0, 0, 0, List.of(), List.of(), List.of(), List.of(), List.of());
        }
    }

    private record Cached(long at, Traffic value) {}

    private final RestClient http = RestClient.create();
    private final ObjectMapper mapper;
    private final Map<Integer, Cached> cache = new ConcurrentHashMap<>();

    @Value("${cloudflare.api-token:}")
    private String apiToken;

    @Value("${cloudflare.account-id:}")
    private String accountId;

    @Value("${cloudflare.site-host:eventsrus.app}")
    private String siteHost;

    @Value("${cloudflare.beacon-token:}")
    private String beaconToken;

    /** Only changed in local tests, to point the service at a fake Cloudflare reply. */
    @Value("${cloudflare.api-url:https://api.cloudflare.com/client/v4/graphql}")
    private String apiUrl;

    public CloudflareTrafficService(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public Traffic traffic(int days) {
        if (apiToken == null || apiToken.isBlank()) {
            return Traffic.notConfigured("Cloudflare is not connected yet (no API token set).");
        }
        Cached c = cache.get(days);
        if (c != null && System.currentTimeMillis() - c.at < CACHE_MS) {
            return c.value;
        }
        try {
            Traffic t = fetch(days);
            cache.put(days, new Cached(System.currentTimeMillis(), t));
            return t;
        } catch (Exception e) {
            log.warn("Cloudflare analytics fetch failed: {}", e.toString());
            return Traffic.notConfigured("Could not read Cloudflare: " + e.getMessage());
        }
    }

    public boolean beaconInstalled() {
        return beaconToken != null && !beaconToken.isBlank();
    }

    private Traffic fetch(int days) throws Exception {
        String since = LocalDate.now(MANILA).minusDays(days - 1L).toString();
        String base = "date_geq:\"" + since + "\",requestHost:\"" + siteHost + "\",bot:0";
        String ph = base + ",countryName:\"PH\"";
        String q = "query{viewer{accounts(filter:{accountTag:\"" + accountId + "\"}){"
                + group("days", 400, base, "date_ASC", "date")
                + group("phDays", 400, ph, "date_ASC", "date")
                + group("countries", 12, base, "sum_visits_DESC", "countryName")
                + group("phPages", 15, ph, "sum_visits_DESC", "requestPath")
                + group("phRefs", 10, ph, "sum_visits_DESC", "refererHost")
                + group("phDevices", 5, ph, "sum_visits_DESC", "deviceType")
                + "}}}";

        String body = mapper.writeValueAsString(Map.of("query", q));
        String raw = http.post().uri(apiUrl)
                .header("Authorization", "Bearer " + apiToken)
                .header("Content-Type", "application/json")
                .body(body).retrieve().body(String.class);
        JsonNode root = mapper.readTree(raw);
        if (root.has("errors") && root.get("errors").size() > 0) {
            throw new IllegalStateException(root.get("errors").get(0).path("message").asText("unknown error"));
        }
        JsonNode a = root.path("data").path("viewer").path("accounts").path(0);

        Map<String, Long> phByDay = new ConcurrentHashMap<>();
        for (JsonNode n : a.path("phDays")) {
            phByDay.put(n.path("dimensions").path("date").asText(), n.path("sum").path("visits").asLong());
        }
        List<Day> dayList = new ArrayList<>();
        long visits = 0;
        for (JsonNode n : a.path("days")) {
            String d = n.path("dimensions").path("date").asText();
            long v = n.path("sum").path("visits").asLong();
            visits += v;
            dayList.add(new Day(d, v, phByDay.getOrDefault(d, 0L)));
        }
        long phVisits = 0;
        long phViews = 0;
        for (JsonNode n : a.path("phDays")) {
            phVisits += n.path("sum").path("visits").asLong();
            phViews += n.path("count").asLong();
        }
        return new Traffic(true, null, visits, phVisits, phViews, dayList,
                items(a.path("countries"), "countryName", "Unknown"),
                items(a.path("phPages"), "requestPath", "/"),
                items(a.path("phRefs"), "refererHost", "Direct / unknown"),
                items(a.path("phDevices"), "deviceType", "Unknown"));
    }

    private static String group(String alias, int limit, String filter, String order, String dim) {
        return alias + ":rumPageloadEventsAdaptiveGroups(limit:" + limit + ",filter:{" + filter + "},orderBy:[" + order
                + "]){count sum{visits} dimensions{" + dim + "}}";
    }

    private static List<Item> items(JsonNode arr, String dim, String fallback) {
        List<Item> out = new ArrayList<>();
        for (JsonNode n : arr) {
            String label = n.path("dimensions").path(dim).asText("");
            out.add(new Item(label.isEmpty() ? fallback : label, n.path("sum").path("visits").asLong(), n.path("count").asLong()));
        }
        return out;
    }
}
