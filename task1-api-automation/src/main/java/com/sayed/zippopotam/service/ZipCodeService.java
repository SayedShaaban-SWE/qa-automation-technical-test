package com.sayed.zippopotam.service;

import com.sayed.zippopotam.client.SpecFactory;
import com.sayed.zippopotam.endpoints.Endpoints;
import com.sayed.zippopotam.enums.Country;
import com.sayed.zippopotam.model.ZipCodeResponse;
import io.qameta.allure.Step;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

/**
 * Service object for the postal-code endpoint - the API-testing equivalent of a Page Object.
 *
 * <p>Tests express <em>what</em> they want ("look up 90210 in the US"); this class owns <em>how</em>
 * that maps to an HTTP call. If the path, the parameter style or the auth scheme ever changes, no
 * test changes.
 *
 * <p>It deliberately returns the raw {@link Response} rather than asserting inside: assertions belong
 * to the test, so one service method can serve positive, negative and contract tests alike.
 */
public class ZipCodeService {

    private final RequestSpecification requestSpec;

    public ZipCodeService() {
        this(SpecFactory.defaultRequest());
    }

    /** Constructor injection keeps the class testable and lets a test supply its own spec. */
    public ZipCodeService(RequestSpecification requestSpec) {
        this.requestSpec = requestSpec;
    }

    @Step("Look up postal code {postalCode} in {country}")
    public Response lookup(Country country, String postalCode) {
        return lookup(country.code(), postalCode);
    }

    /** String overload so negative tests can pass values a {@link Country} cannot express. */
    @Step("Look up postal code {postalCode} in country '{country}'")
    public Response lookup(String country, String postalCode) {
        return given()
                .spec(requestSpec)
                .pathParam("country", country)
                .pathParam("postalCode", postalCode)
                .when()
                .get(Endpoints.POSTAL_CODE_LOOKUP);
    }

    /** Same lookup, but using the trailing-slash URL form given in the requirements document. */
    @Step("Look up postal code {postalCode} in '{country}' using the trailing-slash URL form")
    public Response lookupWithTrailingSlash(String country, String postalCode) {
        return given()
                .spec(requestSpec)
                .pathParam("country", country)
                .pathParam("postalCode", postalCode)
                .when()
                .get(Endpoints.POSTAL_CODE_LOOKUP_TRAILING_SLASH);
    }

    /** Issues an arbitrary HTTP verb against the endpoint, for method-allow-list checks. */
    @Step("Send {method} to /{country}/{postalCode}")
    public Response request(Method method, String country, String postalCode) {
        return given()
                .spec(requestSpec)
                .pathParam("country", country)
                .pathParam("postalCode", postalCode)
                .when()
                .request(method, Endpoints.POSTAL_CODE_LOOKUP);
    }

    /** Raw path escape hatch for malformed-URL tests (extra segments, missing segments, ...). */
    @Step("GET raw path '{path}'")
    public Response getRawPath(String path) {
        return given()
                .spec(requestSpec)
                .when()
                .get(path);
    }

    /**
     * Convenience for the happy path: performs the lookup, enforces the shared success contract and
     * deserialises. Anything unexpected fails here with the full request/response logged.
     */
    @Step("Look up postal code {postalCode} in {country} and deserialise the response")
    public ZipCodeResponse lookupAsModel(Country country, String postalCode) {
        return lookup(country, postalCode)
                .then()
                .spec(SpecFactory.successfulLookup())
                .extract()
                .as(ZipCodeResponse.class);
    }
}
