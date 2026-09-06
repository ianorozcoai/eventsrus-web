package com.web.eventsrus.model;

/** Mirrors eventsrus-backend's {@code enums.TicketCategory}, same names. */
public enum TicketCategory {
    TRANSACTION_DISPUTE("Transaction Dispute"),
    BILLING("Billing"),
    TECHNICAL_ISSUE("Technical Issue"),
    ACCOUNT("Account"),
    OTHER("Other");

    private final String label;

    TicketCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
