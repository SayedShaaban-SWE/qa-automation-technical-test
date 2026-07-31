package com.sayed.zippopotam.tests;

import com.sayed.zippopotam.enums.Country;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What the endpoint does when the input is wrong.
 *
 * <p>The rules being enforced are: an unresolvable lookup is a 404 (not a 200 with an empty body,
 * and not a 500), the error body stays machine-readable JSON, and no input can push the service
 * into a server error.
 */
@Feature("Postal code lookup")
@DisplayName("Postal code lookup - invalid input")
class NegativeLookupTest extends BaseApiTest {

    @DisplayName("Unresolvable country/postal-code combinations return 404 with an empty JSON body")
    @ParameterizedTest(name = "[{index}] /{0}/{1} - {2}")
    @CsvSource(delimiter = '|', textBlock = """
            us | 99999                  | postal code that does not exist in a supported country
            xx | 90210                  | country code that is not supported
            zz | 0                      | neither country nor postal code is valid
            us | 9021                   | postal code one digit too short
            us | 902100                 | postal code one digit too long
            us | abcde                  | alphabetic postal code for a numeric-only country
            us | 90 210                 | postal code containing a space
            gb | 90210                  | postal code that is valid, but in a different country
            """)
    void unresolvable_lookups_return_404(String country, String postalCode, String scenario) {
        Response response = zipCodeService.lookup(country, postalCode);

        assertThat(response.statusCode())
                .as("status code for scenario: %s", scenario)
                .isEqualTo(404);
        assertThat(response.getContentType())
                .as("an error must stay machine-readable, not fall back to an HTML page")
                .contains("application/json");
        assertThat(response.getBody().asString().replaceAll("\\s", ""))
                .as("body for scenario: %s", scenario)
                .isEqualTo("{}");
    }

    @DisplayName("Malformed URLs are rejected as client errors, never as server errors")
    @ParameterizedTest(name = "[{index}] GET {0}")
    @ValueSource(strings = {
            "/us",                      // missing the postal code segment
            "/us/",                     // postal code segment present but empty
            "//90210",                  // country segment present but empty
            "/us/90210/extra",          // an extra path segment
            "/us/90210/90211"           // two postal codes
    })
    @Description("A 5xx here would mean unhandled input reaching the application layer. The "
            + "assertion is deliberately on the 4xx class rather than an exact code, because "
            + "404 vs 400 vs 414 is a routing-layer decision the API owner is free to change.")
    void malformed_urls_return_a_client_error(String rawPath) {
        Response response = zipCodeService.getRawPath(rawPath);

        assertThat(response.statusCode())
                .as("status for malformed path %s", rawPath)
                .isBetween(400, 499);
    }

    @DisplayName("Injection-style payloads are neither executed nor reflected back")
    @ParameterizedTest(name = "[{index}] payload: {0}")
    @ValueSource(strings = {
            "' OR 1=1--",
            "<script>alert(1)</script>",
            "../../etc/passwd",
            "%00"
    })
    @Description("Basic input-handling safety net: the payload must produce an ordinary 404, must "
            + "not appear in the response body (reflected XSS), and must not surface a stack trace.")
    void injection_payloads_are_handled_safely(String payload) {
        Response response = zipCodeService.lookup(Country.UNITED_STATES.code(), payload);
        String body = response.getBody().asString();

        assertThat(response.statusCode()).isBetween(400, 499);
        assertThat(body)
                .as("payload must not be reflected into the response")
                .doesNotContain(payload);
        assertThat(body.toLowerCase())
                .as("internal details must not leak")
                .doesNotContain("traceback")
                .doesNotContain("exception")
                .doesNotContain("sql");
    }

    @Test
    @DisplayName("An excessively long postal code does not break the service")
    @Description("Boundary/robustness check: the service must reject oversized input rather than "
            + "time out or crash on it.")
    void excessively_long_postal_code_is_rejected() {
        String oversized = "9".repeat(2_000);

        Response response = zipCodeService.lookup(Country.UNITED_STATES.code(), oversized);

        assertThat(response.statusCode())
                .as("oversized input must be a client error, not a 5xx")
                .isBetween(400, 499);
    }

    @Test
    @DisplayName("Swapping the country and postal code segments returns 404")
    @Description("Parameter-order mistakes are the most common client bug; they must fail loudly "
            + "rather than accidentally resolve.")
    void swapped_path_parameters_return_404() {
        assertThat(zipCodeService.lookup("90210", "us").statusCode()).isEqualTo(404);
    }
}
