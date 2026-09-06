package com.web.eventsrus.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Static reference data for the Province dropdown and Operating Area tag
 * search on Account Settings - not a mirror of any backend DTO, just a
 * fixed list of PH provinces (plus Metro Manila/NCR, which isn't
 * technically a province but is listed here since that's where most
 * vendors will actually be based).
 */
public final class PhilippineProvinces {

    public static final List<String> ALL = List.of(
            "Metro Manila (NCR)",
            "Abra",
            "Agusan del Norte",
            "Agusan del Sur",
            "Aklan",
            "Albay",
            "Antique",
            "Apayao",
            "Aurora",
            "Basilan",
            "Bataan",
            "Batanes",
            "Batangas",
            "Benguet",
            "Biliran",
            "Bohol",
            "Bukidnon",
            "Bulacan",
            "Cagayan",
            "Camarines Norte",
            "Camarines Sur",
            "Camiguin",
            "Capiz",
            "Catanduanes",
            "Cavite",
            "Cebu",
            "Cotabato",
            "Davao de Oro",
            "Davao del Norte",
            "Davao del Sur",
            "Davao Occidental",
            "Davao Oriental",
            "Dinagat Islands",
            "Eastern Samar",
            "Guimaras",
            "Ifugao",
            "Ilocos Norte",
            "Ilocos Sur",
            "Iloilo",
            "Isabela",
            "Kalinga",
            "La Union",
            "Laguna",
            "Lanao del Norte",
            "Lanao del Sur",
            "Leyte",
            "Maguindanao del Norte",
            "Maguindanao del Sur",
            "Marinduque",
            "Masbate",
            "Misamis Occidental",
            "Misamis Oriental",
            "Mountain Province",
            "Negros Occidental",
            "Negros Oriental",
            "Northern Samar",
            "Nueva Ecija",
            "Nueva Vizcaya",
            "Occidental Mindoro",
            "Oriental Mindoro",
            "Palawan",
            "Pampanga",
            "Pangasinan",
            "Quezon",
            "Quirino",
            "Rizal",
            "Romblon",
            "Samar",
            "Sarangani",
            "Siquijor",
            "Sorsogon",
            "South Cotabato",
            "Southern Leyte",
            "Sultan Kudarat",
            "Sulu",
            "Surigao del Norte",
            "Surigao del Sur",
            "Tarlac",
            "Tawi-Tawi",
            "Zambales",
            "Zamboanga del Norte",
            "Zamboanga del Sur",
            "Zamboanga Sibugay");

    /**
     * Options for the Operating Area tag search on Account Settings -
     * "Entire Philippines" first (for a vendor that serves the whole
     * country), then every province.
     */
    public static final List<String> OPERATING_AREA_OPTIONS = buildOperatingAreaOptions();

    private static List<String> buildOperatingAreaOptions() {
        List<String> options = new ArrayList<>();
        options.add("Entire Philippines");
        options.addAll(ALL);
        return List.copyOf(options);
    }

    private PhilippineProvinces() {}
}
