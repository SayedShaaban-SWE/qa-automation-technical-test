package com.sayed.zippopotam.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Optional;

/**
 * Typed view of a successful {@code GET /{country}/{postalCode}} response.
 *
 * <p>Deserialising into a model rather than asserting on raw JSON paths gives three things:
 * compile-time safety when the contract changes, readable assertions, and one single place that
 * documents the contract.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ZipCodeResponse(
        @JsonProperty("post code") String postCode,
        @JsonProperty("country") String country,
        @JsonProperty("country abbreviation") String countryAbbreviation,
        @JsonProperty("places") List<Place> places) {

    /** Defensive copy so a deserialised response cannot be mutated by a test. */
    public ZipCodeResponse {
        places = places == null ? List.of() : List.copyOf(places);
    }

    public int placeCount() {
        return places.size();
    }

    public Optional<Place> firstPlace() {
        return places.isEmpty() ? Optional.empty() : Optional.of(places.get(0));
    }

    public List<String> placeNames() {
        return places.stream().map(Place::placeName).toList();
    }
}
