package com.web.eventsrus.model;

/**
 * Mirrors eventsrus-backend's real {@code ChecklistItemResponse}. vendorType
 * is non-null only for a "Book a X vendor" item created from checking a
 * supplier category; vendorProfileId/vendorSlug are non-null only for a
 * "Follow up with X" item created from bookmarking a specific supplier on
 * their storefront (see EventService#addSupplierCategoryChecklistItem /
 * #addBookmarkChecklistItem) - the two origins are mutually exclusive.
 */
public record PlannerChecklistItem(
        long id, String label, String status, String source, BusinessType vendorType,
        Long vendorProfileId, String vendorSlug) {

    public boolean done() {
        return "DONE".equals(status);
    }

    /** Crossed out ("no longer interested") - distinct from done(), see ChecklistStatus#DISMISSED on the backend. */
    public boolean dismissed() {
        return "DISMISSED".equals(status);
    }
}
