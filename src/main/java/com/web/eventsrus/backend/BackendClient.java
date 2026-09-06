package com.web.eventsrus.backend;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

/**
 * The one place eventsrus-web talks to the real eventsrus-backend - grown
 * from just the subscription/billing/paywall feature to also cover real
 * login and vendor onboarding (see AuthWebController/VendorController).
 * Every method takes the caller's JWT explicitly rather than reading it
 * from a session itself, so this class has no web-layer concerns
 * (HttpSession, cookies) at all.
 */
@Service
@RequiredArgsConstructor
public class BackendClient {

    private final RestClient backendRestClient;

    public BackendAuthResponse loginWithGoogle(String googleIdToken) {
        return backendRestClient.post()
                .uri("/api/v1/auth/google")
                .body(Map.of("idToken", googleIdToken))
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::raise)
                .body(BackendAuthResponse.class);
    }

    public BackendSubscriptionStatus getSubscriptionStatus(String jwt) {
        return backendRestClient.get()
                .uri("/api/v1/vendors/me/subscription")
                .header("Authorization", "Bearer " + jwt)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::raise)
                .body(BackendSubscriptionStatus.class);
    }

    public List<BackendBillingHistoryEntry> getBillingHistory(String jwt) {
        return backendRestClient.get()
                .uri("/api/v1/vendors/me/subscription/history")
                .header("Authorization", "Bearer " + jwt)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::raise)
                .body(new org.springframework.core.ParameterizedTypeReference<List<BackendBillingHistoryEntry>>() {});
    }

    public BackendCreateSubscriptionResponse createSubscription(String jwt, String plan, String billingCycle) {
        return backendRestClient.post()
                .uri("/api/v1/vendors/me/subscription")
                .header("Authorization", "Bearer " + jwt)
                .body(Map.of("plan", plan, "billingCycle", billingCycle))
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::raise)
                .body(BackendCreateSubscriptionResponse.class);
    }

    public BackendSubscriptionStatus confirmSubscription(String jwt, Long vendorSubscriptionId) {
        return backendRestClient.get()
                .uri("/api/v1/vendors/me/subscription/confirm?vendorSubscriptionId=" + vendorSubscriptionId)
                .header("Authorization", "Bearer " + jwt)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::raise)
                .body(BackendSubscriptionStatus.class);
    }

    /**
     * PATCH /api/v1/users/me/vendor - the real vendor onboarding submission.
     * Field-for-field port of eventsrus-ui's vendor_api.dart#becomeVendor
     * (the one other real caller of this endpoint): same scalar fields, same
     * parallel-indexed legalDocumentFiles/Types/Labels arrays. Idempotent on
     * the backend - safe to call again for an already-VENDOR user (it just
     * upserts their existing profile), so no "already a vendor" guard here.
     * Returns a freshly-issued token/role, exactly like login - the caller
     * must WebSession.store(...) the result since role may have just become
     * VENDOR.
     */
    public BackendAuthResponse becomeVendor(
            String jwt,
            com.web.eventsrus.model.VendorOnboardingForm form,
            MultipartFile logo,
            MultipartFile idCard,
            MultipartFile selfie,
            List<MultipartFile> legalDocumentFiles,
            List<String> legalDocumentTypes,
            List<String> legalDocumentLabels) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        addIfPresent(body, "businessName", form.getBusinessName());
        addIfPresent(body, "businessType", form.getBusinessType() != null ? form.getBusinessType().name() : null);
        addIfPresent(body, "ownerName", form.getOwnerName());
        addIfPresent(body, "description", form.getDescription());
        addIfPresent(body, "contactEmail", form.getContactEmail());
        addIfPresent(body, "phoneNumber", form.getPhoneNumber());
        addIfPresent(body, "addressLine1", form.getAddressLine1());
        addIfPresent(body, "addressLine2", form.getAddressLine2());
        addIfPresent(body, "city", form.getCity());
        addIfPresent(body, "state", form.getState());
        addIfPresent(body, "postalCode", form.getPostalCode());
        addIfPresent(body, "country", form.getCountry());
        if (form.getOperatingAreas() != null) {
            form.getOperatingAreas().forEach(area -> body.add("operatingAreas", area));
        }
        body.add("acceptedTerms", String.valueOf(form.isAcceptedTerms()));
        addFileIfPresent(body, "logo", logo);
        addFileIfPresent(body, "idCard", idCard);
        addFileIfPresent(body, "selfie", selfie);
        if (legalDocumentFiles != null) {
            for (int i = 0; i < legalDocumentFiles.size(); i++) {
                MultipartFile file = legalDocumentFiles.get(i);
                if (file == null || file.isEmpty()) {
                    continue;
                }
                body.add("legalDocumentFiles", file.getResource());
                body.add("legalDocumentTypes",
                        legalDocumentTypes != null && i < legalDocumentTypes.size() ? legalDocumentTypes.get(i) : "OTHER");
                body.add("legalDocumentLabels",
                        legalDocumentLabels != null && i < legalDocumentLabels.size() ? legalDocumentLabels.get(i) : "");
            }
        }

        return backendRestClient.patch()
                .uri("/api/v1/users/me/vendor")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::raise)
                .body(BackendAuthResponse.class);
    }

    private void addIfPresent(MultiValueMap<String, Object> body, String key, String value) {
        if (value != null && !value.isBlank()) {
            body.add(key, value);
        }
    }

    private void addFileIfPresent(MultiValueMap<String, Object> body, String key, MultipartFile file) {
        if (file != null && !file.isEmpty()) {
            body.add(key, file.getResource());
        }
    }

    @SuppressWarnings("unchecked")
    private void raise(org.springframework.http.HttpRequest request,
            org.springframework.http.client.ClientHttpResponse response) throws java.io.IOException {
        String message;
        try {
            Map<String, Object> body =
                    (Map<String, Object>) new tools.jackson.databind.ObjectMapper().readValue(response.getBody(), Map.class);
            message = String.valueOf(body.getOrDefault("message", "Request to eventsrus-backend failed"));
        } catch (Exception e) {
            message = "Request to eventsrus-backend failed (" + response.getStatusCode() + ")";
        }
        throw new BackendApiException(message);
    }
}
