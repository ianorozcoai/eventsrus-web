package com.web.eventsrus.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.web.eventsrus.model.PlannerEvent;
import com.web.eventsrus.model.VendorPublicProfile;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Regression test for the incident that broke the Railway build: a test
 * file's hand-written record-constructor calls went stale when
 * VendorPublicProfile/PlannerEvent grew new fields, and nothing caught the
 * drift until a compile error at deploy time. This test instead feeds
 * realistic backend-shaped JSON through the real deserializer (the same
 * ObjectMapper/record mechanism BackendClient uses) and asserts against the
 * record's own accessors - so a future field rename/addition fails here
 * with a clear assertion/mapping error, not as a surprise compile failure
 * in an unrelated test file.
 */
class BackendDeserializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesARealisticVendorPublicProfileResponse() {
        String json = """
                {
                  "vendorUserId": 9001,
                  "vendorProfileId": 42,
                  "businessName": "Golden Frame Photo & Film",
                  "ownerName": "Jane Dela Cruz",
                  "description": "Wedding and event photography.",
                  "logoImageUrl": "https://cdn.example.com/logo.jpg",
                  "businessTypes": ["PHOTO_AND_VIDEO"],
                  "city": "Quezon City",
                  "state": "Metro Manila",
                  "country": "Philippines",
                  "contactEmail": "hello@goldenframe.example",
                  "phoneNumber": "0917-000-0000",
                  "facebookPageUrl": null,
                  "tierLabel": null,
                  "primaryRegion": null,
                  "bookingsCount": null,
                  "responseTime": null,
                  "legalDocuments": [],
                  "identityVerified": true,
                  "packages": [],
                  "reviews": [],
                  "averageRating": 4.8,
                  "reviewCount": 12,
                  "paymentInstructions": null,
                  "paymentMethods": [],
                  "galleryImages": [],
                  "availableImageTags": [],
                  "availableGroups": [],
                  "socialMediaLinks": []
                }
                """;

        VendorPublicProfile profile = objectMapper.readValue(json, VendorPublicProfile.class);

        assertThat(profile.vendorUserId()).isEqualTo(9001L);
        assertThat(profile.vendorProfileId()).isEqualTo(42L);
        assertThat(profile.businessName()).isEqualTo("Golden Frame Photo & Film");
        assertThat(profile.businessTypes()).hasSize(1);
        assertThat(profile.identityVerified()).isTrue();
        assertThat(profile.averageRating()).isEqualTo(4.8);
        assertThat(profile.reviewCount()).isEqualTo(12);
    }

    @Test
    void deserializesARealisticPlannerEventResponse() {
        String json = """
                {
                  "id": 42,
                  "name": "Birthday",
                  "eventType": "BIRTHDAY",
                  "eventDate": "2026-12-25",
                  "location": "Quezon City",
                  "description": null,
                  "budget": 50000,
                  "guestCount": 80,
                  "aiIdeaText": null,
                  "saved": true,
                  "suggestions": [],
                  "checklist": [],
                  "bookmarkedVendorProfileIds": [42]
                }
                """;

        PlannerEvent event = objectMapper.readValue(json, PlannerEvent.class);

        assertThat(event.id()).isEqualTo(42L);
        assertThat(event.name()).isEqualTo("Birthday");
        assertThat(event.eventDate()).isEqualTo(LocalDate.of(2026, 12, 25));
        assertThat(event.saved()).isTrue();
        assertThat(event.bookmarkedVendorProfileIds()).containsExactly(42L);
    }
}
