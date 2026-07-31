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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Method allow-list.
 *
 * <p>This is a read-only lookup endpoint. Every verb other than GET/HEAD must be refused - an
 * endpoint that quietly accepts POST is either dead code or an unintended write surface.
 */
@Feature("HTTP semantics")
@DisplayName("HTTP method handling")
class HttpMethodTest extends BaseApiTest {

    @DisplayName("Write and diagnostic verbs are rejected with 405 Method Not Allowed")
    @ParameterizedTest(name = "[{index}] {0} -> 405")
    @EnumSource(value = Method.class, names = {"POST", "PUT", "PATCH", "DELETE", "OPTIONS", "TRACE"})
    @Description("405 rather than 404 matters: it tells the client the resource exists but the verb "
            + "does not, which is the difference between 'wrong URL' and 'wrong call'.")
    void unsupported_verbs_are_rejected(Method method) {
        Response response = zipCodeService.request(method, Country.UNITED_STATES.code(), "90210");

        assertThat(response.statusCode())
                .as("%s on a read-only endpoint", method)
                .isEqualTo(405);
    }

    @Test
    @DisplayName("GET is safe: repeated calls do not alter the resource")
    @Description("Verifies the 'safe method' guarantee of GET. If a read ever changed state, the "
            + "second read would differ.")
    void get_is_safe_and_idempotent() {
        String first = zipCodeService.lookup(Country.UNITED_STATES, "90210").getBody().asString();
        String second = zipCodeService.lookup(Country.UNITED_STATES, "90210").getBody().asString();

        assertThat(second)
                .as("two identical GETs must return byte-identical payloads")
                .isEqualTo(first);
    }

    @Test
    @DisplayName("A rejected POST does not create a resource")
    @Description("Follows the 405 up with the assertion that actually matters to a user: nothing "
            + "was written.")
    void rejected_post_does_not_create_a_resource() {
        String beforeState = zipCodeService.lookup(Country.UNITED_STATES, "90210").getBody().asString();

        zipCodeService.request(Method.POST, Country.UNITED_STATES.code(), "90210");

        assertThat(zipCodeService.lookup(Country.UNITED_STATES, "90210").getBody().asString())
                .as("resource must be unchanged after a rejected write")
                .isEqualTo(beforeState);
    }
}
