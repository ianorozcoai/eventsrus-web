package com.web.eventsrus.model;

/** Plain-words label for the backend's signup_source value (WEB, ANDROID, IOS, MOBILE, UNKNOWN or null). */
public final class SignupSourceLabels {

    private SignupSourceLabels() {
    }

    public static String label(String source) {
        if (source == null) {
            return "Unknown (before tracking)";
        }
        switch (source) {
            case "WEB":
                return "Website";
            case "ANDROID":
                return "Android app";
            case "IOS":
                return "iPhone app";
            case "MOBILE":
                return "Mobile app";
            default:
                return "Unknown";
        }
    }
}
