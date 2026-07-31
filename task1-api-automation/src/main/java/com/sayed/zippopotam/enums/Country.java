package com.sayed.zippopotam.enums;

import java.util.Arrays;
import java.util.Locale;

/**
 * Countries the suite exercises, with the values the API is expected to echo back.
 *
 * <p>Modelling this as an enum instead of passing bare strings around means a test can assert
 * "the response describes Germany" without repeating the literal {@code "Germany"} in ten places,
 * and an unsupported country becomes impossible to express by accident.
 */
public enum Country {

    UNITED_STATES("us", "United States", "US"),
    GREAT_BRITAIN("gb", "Great Britain", "GB"),
    CANADA("ca", "Canada", "CA"),
    GERMANY("de", "Germany", "DE"),
    FRANCE("fr", "France", "FR"),
    SPAIN("es", "Spain", "ES"),
    ITALY("it", "Italy", "IT"),
    NETHERLANDS("nl", "Netherlands", "NL"),
    BRAZIL("br", "Brazil", "BR"),
    JAPAN("jp", "Japan", "JP"),
    INDIA("in", "India", "IN"),
    AUSTRALIA("au", "Australia", "AU"),
    MEXICO("mx", "Mexico", "MX"),
    AUSTRIA("at", "Austria", "AT"),
    TURKEY("tr", "Turkey", "TR");

    private final String code;
    private final String officialName;
    private final String abbreviation;

    Country(String code, String officialName, String abbreviation) {
        this.code = code;
        this.officialName = officialName;
        this.abbreviation = abbreviation;
    }

    /** ISO-3166 alpha-2 code as used in the URL path, lower case. */
    public String code() {
        return code;
    }

    /** Value expected in the {@code country} field of the response. */
    public String officialName() {
        return officialName;
    }

    /** Value expected in the {@code country abbreviation} field of the response. */
    public String abbreviation() {
        return abbreviation;
    }

    public String codeUpperCase() {
        return code.toUpperCase(Locale.ROOT);
    }

    public static Country fromCode(String code) {
        return Arrays.stream(values())
                .filter(c -> c.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown country code: " + code));
    }

    @Override
    public String toString() {
        return code;
    }
}
