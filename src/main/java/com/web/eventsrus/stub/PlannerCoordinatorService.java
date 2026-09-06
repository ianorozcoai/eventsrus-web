package com.web.eventsrus.stub;

import com.web.eventsrus.model.BusinessType;
import com.web.eventsrus.model.EventType;
import com.web.eventsrus.model.PlannerVendorSuggestion;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Stub-only "AI event coordinator" for the landing page's chat mode. Ported
 * directly from eventsrus-backend's RuleBasedAiSuggestionService - same idea
 * text templates, same default-vendor-types-per-event-type map, same
 * keyword scan over the description - so this stub stays a faithful preview
 * of what the real backend's EventService#createEvent pipeline already
 * does. Not a real LLM call, same as the backend it mirrors.
 * <p>
 * Vendor matching also mirrors the real VendorSearchService#findMatchingVendors
 * (now operating-area-based, not city-based), but sourced from the local
 * stubs/vendor-directory.json rather than a live call - eventsrus-web has no
 * real planner auth yet, so this stays a frontend simulation.
 */
@Service
public class PlannerCoordinatorService {

    private static final Map<EventType, List<BusinessType>> DEFAULT_VENDOR_TYPES = new EnumMap<>(EventType.class);
    private static final Map<EventType, String> IDEA_TEMPLATES = new EnumMap<>(EventType.class);

    static {
        DEFAULT_VENDOR_TYPES.put(EventType.WEDDING,
                List.of(BusinessType.VENUE, BusinessType.CATERING, BusinessType.PHOTO_AND_VIDEO,
                        BusinessType.DECORATION_PRODUCTION, BusinessType.ENTERTAINMENT));
        DEFAULT_VENDOR_TYPES.put(EventType.ANNIVERSARY,
                List.of(BusinessType.VENUE, BusinessType.CATERING, BusinessType.PHOTO_AND_VIDEO,
                        BusinessType.DECORATION_PRODUCTION));
        DEFAULT_VENDOR_TYPES.put(EventType.BIRTHDAY,
                List.of(BusinessType.VENUE, BusinessType.CATERING, BusinessType.ENTERTAINMENT,
                        BusinessType.DECORATION_PRODUCTION));
        DEFAULT_VENDOR_TYPES.put(EventType.PARTY,
                List.of(BusinessType.VENUE, BusinessType.CATERING, BusinessType.ENTERTAINMENT));
        DEFAULT_VENDOR_TYPES.put(EventType.OTHER,
                List.of(BusinessType.VENUE, BusinessType.CATERING));

        IDEA_TEMPLATES.put(EventType.WEDDING,
                "For a memorable wedding, start by locking in your venue and date, then layer in "
                        + "catering, photography, and decor around a single cohesive theme. Book your "
                        + "highest-demand vendors (venue and photographer) first - they tend to fill up "
                        + "fastest for popular dates.");
        IDEA_TEMPLATES.put(EventType.ANNIVERSARY,
                "Anniversaries work best with an intimate, personal touch - a venue that fits your "
                        + "guest count comfortably, catering that reflects a shared favorite, and a "
                        + "photographer to capture the milestone.");
        IDEA_TEMPLATES.put(EventType.BIRTHDAY,
                "Birthdays are all about the guest of honor - pick a venue and entertainment that "
                        + "match their personality, and build catering and decor around that theme.");
        IDEA_TEMPLATES.put(EventType.PARTY,
                "Keep it simple: a flexible venue, crowd-pleasing catering, and entertainment that "
                        + "matches your guest list's energy.");
        IDEA_TEMPLATES.put(EventType.OTHER,
                "Start with a venue that fits your guest count and budget, then build out catering "
                        + "and any specialty vendors your event needs.");
    }

    private final StubDataService stubDataService;

    public PlannerCoordinatorService(StubDataService stubDataService) {
        this.stubDataService = stubDataService;
    }

    /** The AI coordinator's reply text for the given event type. */
    public String coordinatorReply(EventType eventType) {
        return IDEA_TEMPLATES.getOrDefault(eventType, IDEA_TEMPLATES.get(EventType.OTHER));
    }

    /**
     * A reply to a planner's follow-up message on an event's Overview tab
     * chat. Same rule-based spirit as coordinatorReply/matchSuppliers - not
     * a real LLM call. Reuses the same keyword scan matchSuppliers already
     * runs over the original description, so a follow-up that mentions e.g.
     * "catering" gets pointed back at the supplier categories already
     * shown, rather than a generic non-answer.
     */
    public String followUpReply(String message) {
        Set<BusinessType> mentioned = scanDescriptionForVendorTypes(message);
        if (mentioned.isEmpty()) {
            return "Thanks for the update! Let me know if you'd like more supplier recommendations or details on any of them.";
        }
        String categories = mentioned.stream().map(BusinessType::getLabel).reduce((a, b) -> a + " and " + b).orElse("");
        return "Got it - noted. You may want to take a closer look at the " + categories.toLowerCase(Locale.ROOT)
                + " suppliers below.";
    }

    /** Vendors from the stub directory matching the suggested categories for this event and serving {@code location}. */
    public List<PlannerVendorSuggestion> matchSuppliers(EventType eventType, String description, String location) {
        if (location == null || location.isBlank()) {
            return List.of();
        }

        Set<BusinessType> vendorTypes = new LinkedHashSet<>(
                DEFAULT_VENDOR_TYPES.getOrDefault(eventType, DEFAULT_VENDOR_TYPES.get(EventType.OTHER)));
        vendorTypes.addAll(scanDescriptionForVendorTypes(description));

        String trimmedLocation = location.trim();
        return stubDataService.loadList("vendor-directory.json", PlannerVendorSuggestion.class).stream()
                .filter(v -> vendorTypes.contains(v.businessType()))
                .filter(v -> v.operatingAreas().contains(trimmedLocation) || v.operatingAreas().contains("Entire Philippines"))
                .toList();
    }

    private Set<BusinessType> scanDescriptionForVendorTypes(String description) {
        Set<BusinessType> found = new LinkedHashSet<>();
        if (description == null || description.isBlank()) {
            return found;
        }

        String lower = description.toLowerCase(Locale.ROOT);
        if (lower.contains("photo") || lower.contains("video")) found.add(BusinessType.PHOTO_AND_VIDEO);
        if (lower.contains("cater") || lower.contains("food") || lower.contains("dining")) found.add(BusinessType.CATERING);
        if (lower.contains("decor")) found.add(BusinessType.DECORATION_PRODUCTION);
        if (lower.contains("flowers") || lower.contains("florist")) found.add(BusinessType.FLORAL_SERVICES);
        if (lower.contains("band") || lower.contains("dj") || lower.contains("music") || lower.contains("entertain")) {
            found.add(BusinessType.ENTERTAINMENT);
        }
        if (lower.contains("venue") || lower.contains("hall") || lower.contains("garden") || lower.contains("outdoor")) {
            found.add(BusinessType.VENUE);
        }
        return found;
    }
}
