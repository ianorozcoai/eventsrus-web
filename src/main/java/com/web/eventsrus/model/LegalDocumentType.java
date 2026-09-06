package com.web.eventsrus.model;

/** Mirrors eventsrus-backend's {@code enums.LegalDocumentType}, same names. */
public enum LegalDocumentType {
    DTI_PERMIT("DTI Permit"),
    SEC_REGISTRATION("SEC Registration"),
    MAYORS_PERMIT("Mayor's Permit"),
    BARANGAY_CLEARANCE("Barangay Clearance"),
    BIR_REGISTRATION("BIR Registration"),
    OTHER("Other");

    private final String label;

    LegalDocumentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
