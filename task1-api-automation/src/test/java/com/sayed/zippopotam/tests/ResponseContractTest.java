package com.sayed.zippopotam.tests;

import com.sayed.zippopotam.enums.Country;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.restassured.http.Method;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract-level checks: shape, types and transport headers rather than individual values.
 *
 * <p>A functional test proves 90210 is Beverly Hills. These prove the API keeps the promise it made
 * to every client - which is what actually breaks integrations when a provider refactors.
 */
@Feature("Response contract")
@DisplayName("Response contract")
class ResponseContractTest extends BaseApiTest {

    private static final String SCHEMA = "schemas/zip-code-response-schema.json";

    @DisplayName("Successful responses conform to the published JSON schema")
    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(value = Country.class, names = {"UNITED_STATES", "GREAT_BRITAIN", "GERMANY", "JAPAN", "AUSTRALIA"})
    @Description("The schema is strict (additionalProperties=false, required on every field). "
            + "It therefore fails on a removed field, a renamed field, a changed type AND a "
            + "silently added field - the four ways a JSON contract breaks.")
    void successful_responses_match_the_json_schema(Country country) {
        zipCodeService.lookup(country, postalCodeFor(country))
                .then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath(SCHEMA));
    }

    @Test
    @DisplayName("Successful responses are served as application/json")
    void response_is_json() {
        zipCodeService.lookup(Country.UNITED_STATES, "90210")
                .then()
                .statusCode(200)
                .contentType("application/json");
    }

    @Test
    @DisplayName("The response advertises CORS so browser clients can consume it")
    @Description("zippopotam.us is documented for browser use; losing this header silently breaks "
            + "every front-end consumer while every server-side test stays green.")
    void response_exposes_permissive_cors_header() {
        Response response = zipCodeService.lookup(Country.UNITED_STATES, "90210");

        assertThat(response.getHeader("Access-Control-Allow-Origin"))
                .as("Access-Control-Allow-Origin header")
                .isEqualTo("*");
    }

    @Test
    @DisplayName("HEAD returns the response metadata without a body")
    @Description("HEAD must be consistent with GET: same status, same content type, empty body. "
            + "It is the cheapest existence check a client can make.")
    void head_returns_metadata_without_a_body() {
        Response headResponse = zipCodeService.request(Method.HEAD, Country.UNITED_STATES.code(), "90210");

        assertThat(headResponse.statusCode()).isEqualTo(200);
        assertThat(headResponse.getContentType()).contains("application/json");
        assertThat(headResponse.getBody().asString()).isEmpty();
    }

    @Test
    @DisplayName("The response is cacheable, as expected for immutable reference data")
    @Description("Postal-code data barely changes. A missing or 'no-store' Cache-Control would be a "
            + "performance regression for every consumer and for the provider's own infrastructure.")
    void response_is_cacheable() {
        Response response = zipCodeService.lookup(Country.UNITED_STATES, "90210");

        assertThat(response.getHeader("Cache-Control"))
                .as("Cache-Control header")
                .isNotNull()
                .doesNotContain("no-store");
    }

    /** Small lookup table so the enum-driven schema test stays readable. */
    private static String postalCodeFor(Country country) {
        return switch (country) {
            case UNITED_STATES -> "90210";
            case GREAT_BRITAIN -> "B1";
            case GERMANY -> "10115";
            case JAPAN -> "100-0001";
            case AUSTRALIA -> "2000";
            default -> throw new IllegalArgumentException("No sample postal code configured for " + country);
        };
    }
}
