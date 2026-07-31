package com.sayed.zippopotam.tests;

import com.sayed.zippopotam.enums.Country;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A single, non-functional guard rail.
 *
 * <p>Deliberately kept separate from the functional suite and deliberately generous: this is a
 * shared public API behind a CDN, so a tight threshold here would produce noise, not signal. The
 * budget lives in {@code config.properties} so it can be tightened per environment without a code
 * change. Real load/performance testing is a different tool and a different pipeline stage.
 */
@Feature("Non-functional")
@DisplayName("Response time")
class ResponseTimeTest extends BaseApiTest {

    @Test
    @DisplayName("A lookup completes within the configured SLA")
    @Description("Catches gross regressions - a lookup that went from milliseconds to seconds - "
            + "without pretending to be a performance test.")
    void lookup_completes_within_the_configured_sla() {
        long slaMs = CONFIG.responseTimeSlaMs();

        Response response = zipCodeService.lookup(Country.UNITED_STATES, "90210");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.timeIn(java.util.concurrent.TimeUnit.MILLISECONDS))
                .as("response time budget is %d ms (configurable via performance.response.time.sla.ms)", slaMs)
                .isLessThan(slaMs);
    }

    @Test
    @DisplayName("A miss (404) is answered as quickly as a hit")
    @Description("An unresolvable lookup should short-circuit. If a 404 is dramatically slower than "
            + "a 200 it usually means the miss path is doing a full scan - a cheap denial-of-service "
            + "vector on a public endpoint.")
    void unresolvable_lookup_is_answered_within_the_same_budget() {
        Response response = zipCodeService.lookup(Country.UNITED_STATES.code(), "99999");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.timeIn(java.util.concurrent.TimeUnit.MILLISECONDS))
                .as("a 404 must respect the same budget as a 200")
                .isLessThan(CONFIG.responseTimeSlaMs());
    }
}
