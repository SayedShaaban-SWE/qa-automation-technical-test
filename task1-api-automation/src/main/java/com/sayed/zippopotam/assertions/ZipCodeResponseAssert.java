package com.sayed.zippopotam.assertions;

import com.sayed.zippopotam.enums.Country;
import com.sayed.zippopotam.model.Place;
import com.sayed.zippopotam.model.ZipCodeResponse;
import org.assertj.core.api.AbstractAssert;

import java.util.List;

/**
 * Domain-specific fluent assertion for {@link ZipCodeResponse}.
 *
 * <p>Extending AssertJ instead of writing loose {@code assertEquals} calls buys two things that
 * matter in a real suite: assertions read like the requirement
 * ({@code assertThat(response).describes(Country.UNITED_STATES).hasPlaceCount(1)}), and every
 * failure message is written once, correctly, next to the rule it enforces.
 */
public class ZipCodeResponseAssert extends AbstractAssert<ZipCodeResponseAssert, ZipCodeResponse> {

    private ZipCodeResponseAssert(ZipCodeResponse actual) {
        super(actual, ZipCodeResponseAssert.class);
    }

    public static ZipCodeResponseAssert assertThat(ZipCodeResponse actual) {
        return new ZipCodeResponseAssert(actual);
    }

    public ZipCodeResponseAssert hasPostCode(String expected) {
        isNotNull();
        if (!expected.equals(actual.postCode())) {
            failWithMessage("Expected 'post code' to be <%s> but was <%s>", expected, actual.postCode());
        }
        return this;
    }

    /** Asserts the country name and abbreviation both match the given country. */
    public ZipCodeResponseAssert describes(Country expected) {
        isNotNull();
        if (!expected.officialName().equals(actual.country())) {
            failWithMessage("Expected 'country' to be <%s> but was <%s>",
                    expected.officialName(), actual.country());
        }
        if (!expected.abbreviation().equals(actual.countryAbbreviation())) {
            failWithMessage("Expected 'country abbreviation' to be <%s> but was <%s>",
                    expected.abbreviation(), actual.countryAbbreviation());
        }
        return this;
    }

    public ZipCodeResponseAssert hasPlaces() {
        isNotNull();
        if (actual.places().isEmpty()) {
            failWithMessage("Expected 'places' to contain at least one entry but it was empty");
        }
        return this;
    }

    public ZipCodeResponseAssert hasPlaceCount(int expected) {
        isNotNull();
        if (actual.placeCount() != expected) {
            failWithMessage("Expected <%s> places but found <%s>: %s",
                    expected, actual.placeCount(), actual.placeNames());
        }
        return this;
    }

    public ZipCodeResponseAssert hasAtLeastPlaces(int minimum) {
        isNotNull();
        if (actual.placeCount() < minimum) {
            failWithMessage("Expected at least <%s> places but found <%s>: %s",
                    minimum, actual.placeCount(), actual.placeNames());
        }
        return this;
    }

    public ZipCodeResponseAssert containsPlaceNamed(String expectedPlaceName) {
        isNotNull();
        List<String> names = actual.placeNames();
        if (names.stream().noneMatch(name -> name != null && name.equalsIgnoreCase(expectedPlaceName))) {
            failWithMessage("Expected a place named <%s> but 'places' contained %s",
                    expectedPlaceName, names);
        }
        return this;
    }

    /** No mandatory string field may be null or blank - an empty value is a data defect, not data. */
    public ZipCodeResponseAssert hasNoBlankFields() {
        isNotNull();
        failIfBlank(actual.postCode(), "post code");
        failIfBlank(actual.country(), "country");
        failIfBlank(actual.countryAbbreviation(), "country abbreviation");
        for (int i = 0; i < actual.placeCount(); i++) {
            Place place = actual.places().get(i);
            failIfBlank(place.placeName(), "places[" + i + "].place name");
            failIfBlank(place.state(), "places[" + i + "].state");
            failIfBlank(place.latitude(), "places[" + i + "].latitude");
            failIfBlank(place.longitude(), "places[" + i + "].longitude");
        }
        return this;
    }

    /** Latitude must be within [-90, 90] and longitude within [-180, 180] for every place. */
    public ZipCodeResponseAssert hasGeographicallyValidCoordinates() {
        isNotNull();
        for (int i = 0; i < actual.placeCount(); i++) {
            Place place = actual.places().get(i);
            if (!place.hasValidCoordinates()) {
                failWithMessage(
                        "places[%s] ('%s') has coordinates outside the valid range: latitude=<%s> (expected -90..90), longitude=<%s> (expected -180..180)",
                        i, place.placeName(), place.latitude(), place.longitude());
            }
        }
        return this;
    }

    private void failIfBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            failWithMessage("Expected '%s' to be populated but it was <%s>", fieldName, value);
        }
    }
}
