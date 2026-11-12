package com.web.eventsrus.model;

import java.util.regex.Pattern;

/**
 * Checks a Philippine mobile number as typed on the website sign-up form:
 * 09XX XXX XXXX, +63 9XX XXX XXXX or 63 9XX XXX XXXX, with optional spaces,
 * dashes or brackets. Only the website uses this - the mobile app and the
 * backend accept any number for now (parked until Google approves the app for
 * production, because Google's reviewers type dummy numbers).
 */
public final class PhoneNumbers {

    private static final Pattern PH_MOBILE = Pattern.compile("^(\\+?63|0)9\\d{9}$");

    private PhoneNumbers() {
    }

    public static boolean isPhilippineMobile(String phone) {
        if (phone == null) {
            return false;
        }
        String compact = phone.replaceAll("[\\s\\-()]", "");
        return PH_MOBILE.matcher(compact).matches();
    }
}
