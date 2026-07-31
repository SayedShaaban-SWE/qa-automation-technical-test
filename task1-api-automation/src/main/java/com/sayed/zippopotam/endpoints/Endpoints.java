package com.sayed.zippopotam.endpoints;

/**
 * Every path the framework knows about, in one place. When the API adds or renames a route this is
 * the only file that changes.
 */
public final class Endpoints {

    /** The single endpoint under test: {@code /{country}/{postalCode}}. */
    public static final String POSTAL_CODE_LOOKUP = "/{country}/{postalCode}";

    /**
     * The form documented in the requirements ({@code api.zippopotam.us/country/postal-code/}).
     * Kept separate because the implementation and the documentation disagree - see
     * {@code TrailingSlashTest}.
     */
    public static final String POSTAL_CODE_LOOKUP_TRAILING_SLASH = POSTAL_CODE_LOOKUP + "/";

    private Endpoints() {
        throw new AssertionError("Constants holder - not instantiable");
    }
}
