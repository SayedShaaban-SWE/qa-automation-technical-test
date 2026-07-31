package com.sayed.zippopotam.tests;

import com.sayed.zippopotam.assertions.ZipCodeResponseAssert;
import com.sayed.zippopotam.enums.Country;
import com.sayed.zippopotam.model.ZipCodeResponse;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Quality of the data itself, not of the transport.
 *
 * <p>A 200 with a well-formed body can still be wrong. These tests encode the invariants a consumer
 * relies on: coordinates that are actually points on Earth, mandatory fields that are populated, and
 * a response that answers the question that was asked.
 *
 * <p>Three of these invariants are violated by the live API today; those tests are kept, tagged
 * {@code known-defect}, and documented in the README rather than deleted or weakened to match the
 * bug. Weakening a test to make it pass is how a defect becomes a feature.
 */
@Feature("Data integrity")
@DisplayName("Data integrity")
class DataIntegrityTest extends BaseApiTest {

    @DisplayName("Coordinates are valid geographic points")
    @ParameterizedTest(name = "[{index}] {0}/{1}")
    @CsvSource({
            "us, 90210", "gb, B1", "ca, M5V", "fr, 75001", "es, 28001",
            "it, 00100", "nl, 1011", "br, 01000-000", "jp, 100-0001",
            "in, 110001", "au, 2000", "mx, 01000", "de, 10115"
    })
    @Description("Latitude must be within [-90, 90] and longitude within [-180, 180]. Anything else "
            + "cannot be plotted and will break every downstream map integration.")
    void coordinates_are_within_valid_geographic_ranges(String countryCode, String postalCode) {
        ZipCodeResponse response = zipCodeService.lookupAsModel(Country.fromCode(countryCode), postalCode);

        ZipCodeResponseAssert.assertThat(response).hasGeographicallyValidCoordinates();
    }

    @DisplayName("Mandatory fields are populated")
    @ParameterizedTest(name = "[{index}] {0}/{1}")
    @CsvSource({
            "us, 90210", "gb, B1", "ca, M5V", "fr, 75001", "es, 28001",
            "it, 00100", "nl, 1011", "br, 01000-000", "in, 110001",
            "au, 2000", "mx, 01000", "de, 10115"
    })
    @Description("An empty string is not data. A consumer rendering 'place name' has no way to tell "
            + "an empty value from a missing one.")
    void mandatory_fields_are_populated(String countryCode, String postalCode) {
        ZipCodeResponse response = zipCodeService.lookupAsModel(Country.fromCode(countryCode), postalCode);

        ZipCodeResponseAssert.assertThat(response).hasNoBlankFields();
    }

    @Test
    @DisplayName("The response answers the question that was asked")
    @Description("The echoed 'post code' and country must match the request. A mismatch means the "
            + "caller is being handed someone else's data - the worst possible failure for a "
            + "cached, CDN-fronted API.")
    void response_echoes_the_requested_parameters() {
        ZipCodeResponse response = zipCodeService.lookupAsModel(Country.CANADA, "M5V");

        ZipCodeResponseAssert.assertThat(response)
                .hasPostCode("M5V")
                .describes(Country.CANADA);
    }

    @Test
    @DisplayName("Places within one postal code are geographically close together")
    @Description("A sanity check on multi-place responses: entries for a single postal code should "
            + "cluster, not be scattered across the country. Catches join defects that a "
            + "field-by-field assertion would miss.")
    void places_within_one_postal_code_are_clustered() {
        ZipCodeResponse response = zipCodeService.lookupAsModel(Country.AUSTRALIA, "2000");

        double maxLatitude = response.places().stream().mapToDouble(p -> p.latitudeAsDouble()).max().orElseThrow();
        double minLatitude = response.places().stream().mapToDouble(p -> p.latitudeAsDouble()).min().orElseThrow();
        double maxLongitude = response.places().stream().mapToDouble(p -> p.longitudeAsDouble()).max().orElseThrow();
        double minLongitude = response.places().stream().mapToDouble(p -> p.longitudeAsDouble()).min().orElseThrow();

        // 1 degree is roughly 111 km - generous, but it still catches an entry from another state.
        assertThat(maxLatitude - minLatitude).as("latitude spread").isLessThan(1.0);
        assertThat(maxLongitude - minLongitude).as("longitude spread").isLessThan(1.0);
    }

    // ------------------------------------------------------------------ known defects

    @Test
    @Tag("known-defect")
    @Issue("BUG-003")
    @DisplayName("BUG-003: DE/01067 returns a latitude of 14612 with latitude and longitude swapped")
    @Description("""
            GET /de/01067 returns, for all three places:
                "longitude": "51.05", "latitude": "14612"
            Latitude 14612 is outside the valid [-90, 90] range. The values are also transposed:
            51.05 is Dresden's latitude and 13.74 would be its longitude - 14612 appears to be a
            corrupted/unconverted source value.

            Impact: any consumer plotting this point renders it off-map or throws.
            """)
    void de_01067_should_return_valid_coordinates() {
        ZipCodeResponse response = zipCodeService.lookupAsModel(Country.GERMANY, "01067");

        ZipCodeResponseAssert.assertThat(response).hasGeographicallyValidCoordinates();
    }

    @Test
    @Tag("known-defect")
    @Issue("BUG-004")
    @DisplayName("BUG-004: TR/34000 returns an empty place name")
    @Description("""
            GET /tr/34000 returns 200 with "place name": "". The record exists and carries a valid
            state (Istanbul) and coordinates, so the caller gets a location it cannot label.
            Either the name should be populated or the record should not be returned.
            """)
    void tr_34000_should_return_a_place_name() {
        ZipCodeResponse response = zipCodeService.lookupAsModel(Country.TURKEY, "34000");

        ZipCodeResponseAssert.assertThat(response).hasNoBlankFields();
    }

    @Test
    @Tag("known-defect")
    @Issue("BUG-005")
    @DisplayName("BUG-005: AT/1010 returns an empty state")
    @Description("""
            GET /at/1010 returns 200 with both "state" and "state abbreviation" empty, although
            Vienna is a well-defined Austrian state. The schema still validates because the fields
            are present - which is exactly why a schema check alone is not sufficient coverage.
            """)
    void at_1010_should_return_a_state() {
        ZipCodeResponse response = zipCodeService.lookupAsModel(Country.AUSTRIA, "1010");

        ZipCodeResponseAssert.assertThat(response).hasNoBlankFields();
    }
}
