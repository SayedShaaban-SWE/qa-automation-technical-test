package com.sayed.zippopotam.client;

import com.sayed.zippopotam.config.ConfigManager;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

/**
 * Builds the reusable request/response specifications.
 *
 * <p>Centralising them means base URI, timeouts, logging policy and reporting filters are declared
 * exactly once. Tests never call {@code RestAssured.baseURI = ...} - that static global is the usual
 * source of cross-test interference once a suite runs in parallel.
 *
 * <p>Logging is attached via {@code enableLoggingOfRequestAndResponseIfValidationFails}: a green run
 * stays quiet, a red run prints everything needed to triage it.
 */
public final class SpecFactory {

    private static final ConfigManager CONFIG = ConfigManager.get();

    private SpecFactory() {
        throw new AssertionError("Factory - not instantiable");
    }

    /** Base request specification: every call in the suite starts from this. */
    public static RequestSpecification defaultRequest() {
        return new RequestSpecBuilder()
                .setBaseUri(CONFIG.baseUri())
                .setAccept(ContentType.JSON)
                .setConfig(restAssuredConfig())
                .addFilter(new AllureRestAssured())
                .build();
    }

    /**
     * Contract every successful lookup must satisfy, regardless of which country is queried.
     *
     * <p>Deliberately functional-only. Response time is a separate, non-functional concern asserted
     * by {@code ResponseTimeTest}; baking it in here would turn every functional test into a
     * network-latency flake.
     */
    public static ResponseSpecification successfulLookup() {
        return new ResponseSpecBuilder()
                .expectStatusCode(200)
                .expectContentType(ContentType.JSON)
                .build();
    }

    /** Contract for a lookup that cannot be resolved. */
    public static ResponseSpecification notFound() {
        return new ResponseSpecBuilder()
                .expectStatusCode(404)
                .build();
    }

    private static RestAssuredConfig restAssuredConfig() {
        return RestAssuredConfig.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", CONFIG.connectionTimeoutMs())
                        .setParam("http.socket.timeout", CONFIG.socketTimeoutMs()))
                .logConfig(LogConfig.logConfig()
                        .enableLoggingOfRequestAndResponseIfValidationFails(
                                LogDetail.valueOf(CONFIG.loggingOnFailureDetail())));
    }
}
