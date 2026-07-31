package com.sayed.zippopotam.tests;

import com.sayed.zippopotam.assertions.ZipCodeResponseAssert;
import com.sayed.zippopotam.enums.Country;
import com.sayed.zippopotam.model.Place;
import com.sayed.zippopotam.model.ZipCodeResponse;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The happy path: a known postal code resolves to the expected location.
 *
 * <p>Test data lives in {@code testdata/valid-lookups.csv} rather than in the code, so widening
 * coverage to another country is a one-line data change with no recompilation of intent.
 */
@Feature("Postal code lookup")
@DisplayName("Postal code lookup - successful resolution")
class PostalCodeLookupTest extends BaseApiTest {

    @DisplayName("Known postal codes resolve to the expected country and place")
    @ParameterizedTest(name = "[{index}] {0}/{1} -> {4}, {2}")
    @CsvFileSource(resources = "/testdata/valid-lookups.csv", numLinesToSkip = 1, encoding = "UTF-8")
    @Description("Data-driven coverage across several countries, including alphanumeric (GB), "
            + "hyphenated (JP, BR) and non-ASCII (BR) values, to prove the endpoint is not "
            + "US-postal-code specific.")
    void known_postal_codes_resolve_to_the_expected_location(String countryCode,
                                                             String postalCode,
                                                             String expectedCountryName,
                                                             String expectedCountryAbbreviation,
                                                             String expectedPlaceName) {

        ZipCodeResponse response = zipCodeService.lookupAsModel(Country.fromCode(countryCode), postalCode);

        ZipCodeResponseAssert.assertThat(response)
                .hasPostCode(postalCode)
                .describes(Country.fromCode(countryCode))
                .hasPlaces()
                .containsPlaceNamed(expectedPlaceName);

        // The CSV also documents the expected country strings; asserting them directly keeps the
        // data file honest rather than letting it drift away from the enum.
        assertThat(response.country()).isEqualTo(expectedCountryName);
        assertThat(response.countryAbbreviation()).isEqualTo(expectedCountryAbbreviation);
    }

    @Test
    @DisplayName("Every field of a single, fully-specified lookup is correct")
    @Description("One deep assertion on a stable, well-known postal code. The parameterised test "
            + "above proves breadth; this one proves the exact payload, field by field.")
    void us_90210_returns_the_complete_expected_payload() {
        ZipCodeResponse response = zipCodeService.lookupAsModel(Country.UNITED_STATES, "90210");

        ZipCodeResponseAssert.assertThat(response)
                .hasPostCode("90210")
                .describes(Country.UNITED_STATES)
                .hasPlaceCount(1)
                .hasNoBlankFields()
                .hasGeographicallyValidCoordinates();

        Place beverlyHills = response.firstPlace().orElseThrow();
        assertThat(beverlyHills.placeName()).isEqualTo("Beverly Hills");
        assertThat(beverlyHills.state()).isEqualTo("California");
        assertThat(beverlyHills.stateAbbreviation()).isEqualTo("CA");
        // Coordinates are asserted with a tolerance: the exact value is a data-provider detail,
        // being in Beverly Hills is the requirement.
        assertThat(beverlyHills.latitudeAsDouble()).isCloseTo(34.09, org.assertj.core.data.Offset.offset(0.1));
        assertThat(beverlyHills.longitudeAsDouble()).isCloseTo(-118.41, org.assertj.core.data.Offset.offset(0.1));
    }

    @Test
    @DisplayName("A postal code covering several localities returns every one of them")
    @Description("The 'places' array is a collection, not a single object. A suite that only ever "
            + "checks places[0] would not notice the API silently truncating results.")
    void postal_code_spanning_multiple_localities_returns_all_places() {
        ZipCodeResponse response = zipCodeService.lookupAsModel(Country.INDIA, "110001");

        ZipCodeResponseAssert.assertThat(response)
                .describes(Country.INDIA)
                .hasAtLeastPlaces(2)
                .hasNoBlankFields()
                .hasGeographicallyValidCoordinates();

        assertThat(response.placeNames())
                .as("place names within one postal code must be distinct")
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("Every place in the response belongs to the requested postal code")
    @Description("Guards against a join/paging defect where the response mixes in places from a "
            + "neighbouring postal code.")
    void all_returned_places_share_the_requested_postal_code() {
        ZipCodeResponse response = zipCodeService.lookupAsModel(Country.AUSTRALIA, "2000");

        assertThat(response.postCode()).isEqualTo("2000");
        assertThat(response.places())
                .allSatisfy(place -> assertThat(place.state())
                        .as("all places in AU/2000 are in New South Wales")
                        .isEqualTo("New South Wales"));
    }
}
