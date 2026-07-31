package com.sayed.zippopotam.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * One location entry inside {@link ZipCodeResponse#places()}.
 *
 * <p>The JSON keys contain spaces ({@code "place name"}, {@code "state abbreviation"}), so every
 * field needs an explicit {@link JsonProperty}. {@code @JsonIgnoreProperties(ignoreUnknown = true)}
 * makes the model tolerant of new fields being added by the provider - a test suite should not go
 * red because the API grew a column.
 *
 * <p>Latitude/longitude arrive as strings. They are kept as strings so a malformed value round-trips
 * into the assertion layer instead of blowing up during deserialisation; the typed accessors below
 * are what tests use for range checks.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Place(
        @JsonProperty("place name") String placeName,
        @JsonProperty("longitude") String longitude,
        @JsonProperty("latitude") String latitude,
        @JsonProperty("state") String state,
        @JsonProperty("state abbreviation") String stateAbbreviation) {

    private static final double MIN_LATITUDE = -90.0;
    private static final double MAX_LATITUDE = 90.0;
    private static final double MIN_LONGITUDE = -180.0;
    private static final double MAX_LONGITUDE = 180.0;

    public double latitudeAsDouble() {
        return parseCoordinate(latitude, "latitude");
    }

    public double longitudeAsDouble() {
        return parseCoordinate(longitude, "longitude");
    }

    /** {@code true} when latitude is within [-90, 90] and longitude within [-180, 180]. */
    public boolean hasValidCoordinates() {
        try {
            double lat = latitudeAsDouble();
            double lon = longitudeAsDouble();
            return lat >= MIN_LATITUDE && lat <= MAX_LATITUDE
                    && lon >= MIN_LONGITUDE && lon <= MAX_LONGITUDE;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static double parseCoordinate(String raw, String name) {
        Objects.requireNonNull(raw, name + " is null");
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " is not numeric: '" + raw + "'", e);
        }
    }
}
