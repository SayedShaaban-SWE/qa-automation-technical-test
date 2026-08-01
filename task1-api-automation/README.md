# Task 1 — Zippopotam.us API Automation

API test framework for the single endpoint `api.zippopotam.us/{country}/{postal-code}`.

**Java 25 · Maven · REST Assured 6 · JUnit 6 · AssertJ · JSON Schema**

---

## Running it

**Prerequisites:** JDK 25+ and Maven 3.9+. Nothing else — no local service, no API key.

```bash
cd task1-api-automation
mvn test
```

Expected result:

```
Tests run: 83, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Reports: `target/surefire-reports/`.

Last verified: **1 August 2026**, 83/83 passing on JDK 25.

### Toolchain

The stack is current rather than pinned to whatever was newest when the project started: REST Assured
6.0.1, JUnit 6.1.2, AssertJ 3.27.7, Jackson 2.22.1, Allure 2.35.4, compiled at release 25. Two notes
for anyone bumping it further:

- **Allure renamed its JUnit 5 integration.** `allure-junit5` was relocated to `allure-jupiter` in
  Allure 2.35. The old coordinate still resolves but is a dead end; `pom.xml` uses the new one.
- **JUnit 6 requires Java 17+ at minimum** and drops the legacy Vintage engine. Nothing here used it,
  so the upgrade was a version bump and no code change.

### Useful variations

```bash
# Point the suite at another environment
mvn test -Dapi.base.uri=https://staging.example.com

# Run one class or one method
mvn test -Dtest=DataIntegrityTest
mvn test -Dtest=PostalCodeLookupTest#us_90210_returns_the_complete_expected_payload

# Include the known-defect tests (5 will fail — that is the point, see "Defects found")
mvn test -Dexcluded.groups=none

# Allure report (optional; downloads the report bundle on first use)
mvn test -Preporting && mvn allure:serve -Preporting
```

---

## How it is put together

```
src/main/java/com/sayed/zippopotam/     ← the framework
├── config/     ConfigManager            single source of settings, layered resolution
├── client/     SpecFactory              request/response specifications, timeouts, logging
├── endpoints/  Endpoints                every path in one place
├── enums/      Country                  countries + the values the API should echo back
├── model/      ZipCodeResponse, Place   typed contract
├── service/    ZipCodeService           service object — the API equivalent of a Page Object
└── assertions/ ZipCodeResponseAssert    custom fluent AssertJ assertion

src/test/java/com/sayed/zippopotam/tests/   ← the tests
└── BaseApiTest + 7 test classes

src/test/resources/
├── schemas/zip-code-response-schema.json    strict contract (additionalProperties: false)
├── testdata/valid-lookups.csv               data-driven fixtures
└── junit-platform.properties                parallel execution
```

### Design decisions worth explaining

**A service object, not raw calls in tests.** `ZipCodeService` owns *how* a lookup maps to HTTP.
It returns the raw `Response` rather than asserting internally, so one method serves the positive,
negative and contract tests alike. If the path style or auth scheme changed, no test would change.

**Specifications instead of RestAssured's static globals.** `RestAssured.baseURI = ...` is a mutable
static that becomes cross-test interference the moment a suite runs in parallel. `SpecFactory`
builds a fresh spec per call instead — which is what makes the parallel execution in
`junit-platform.properties` safe.

**A typed model, because of the JSON keys.** The API returns `"post code"` and
`"country abbreviation"` — keys with spaces. GPath treats the space as an expression separator, so
`jsonPath().getString("post code")` fails at runtime with *"The parameter 'code' was used but not
defined"*. This is not theoretical: it is a bug I hit and fixed while building this, and it is why
`ZipCodeResponse` uses explicit `@JsonProperty` mapping.

**A custom AssertJ assertion.** `ZipCodeResponseAssert` means an assertion reads like the rule it
enforces and every failure message is written once, next to that rule:

```java
assertThat(response)
    .hasPostCode("90210")
    .describes(Country.UNITED_STATES)
    .hasPlaceCount(1)
    .hasNoBlankFields()
    .hasGeographicallyValidCoordinates();
```

**Logging only on failure.** `enableLoggingOfRequestAndResponseIfValidationFails` — a green run
stays quiet, a red run prints the full request and response.

**Response time is not in the shared success spec.** Baking a latency budget into every functional
assertion would turn every test into a network flake. It is one separate, non-functional test with
a configurable budget.

---

## What is tested, and why

83 tests across 7 classes. The endpoint is small, so the breadth comes from asking what can go
wrong at each layer rather than from repeating the same lookup with more postal codes.

| Class | Covers | Why it earns its place |
|---|---|---|
| `PostalCodeLookupTest` | Happy path across 9 countries (CSV-driven); one full field-by-field payload check; multi-place responses; all places belonging to the requested code | Breadth *and* depth. Alphanumeric (GB), hyphenated (JP/BR) and non-ASCII (BR) values prove the endpoint is not US-specific. `places` is a collection — a suite that only checks `places[0]` would not notice truncation |
| `ResponseContractTest` | Strict JSON schema across 5 countries; content type; CORS header; `HEAD` consistency; cacheability | Schema is `additionalProperties: false` + `required` everywhere, so it fails on a removed, renamed, retyped **or silently added** field — the four ways a JSON contract breaks |
| `NegativeLookupTest` | 8 unresolvable combinations; 5 malformed URLs; 4 injection payloads; oversized input; swapped parameters | The rules: a miss is a 404 (not a 200 with an empty body, not a 500), errors stay machine-readable, and no input reaches a server error |
| `HttpMethodTest` | POST/PUT/PATCH/DELETE/OPTIONS/TRACE → 405; GET is safe and idempotent; a rejected POST creates nothing | A read-only endpoint that quietly accepts POST is an unintended write surface. 405 rather than 404 is what tells a client "wrong verb", not "wrong URL" |
| `UrlFormatTest` | Country-code casing (4 variants); alphanumeric postal-code casing; separators echoed verbatim | The mistakes real integrators make. Casing must return the same *payload*, not merely the same status |
| `DataIntegrityTest` | Coordinate ranges over 13 countries; mandatory fields populated; request echoed back; multi-place geographic clustering | A 200 with a well-formed body can still be wrong. Clustering catches a join defect that no field-by-field assertion would |
| `ResponseTimeTest` | A hit and a miss both inside the configured budget | A 404 dramatically slower than a 200 usually means the miss path is doing a full scan — a cheap DoS vector on a public endpoint |

---

## Defects found

Testing the live API surfaced five genuine defects. Each is a real test asserting the **correct**
behaviour, tagged `known-defect` so the default build stays green, and reproducible with:

```bash
mvn test -Dexcluded.groups=none      # → Tests run: 88, Failures: 5
```

| ID | Defect | Evidence | Impact |
|---|---|---|---|
| **BUG-001** | The documented trailing-slash URL returns 404 | The brief and the zippopotam.us site both publish `api.zippopotam.us/country/postal-code/`. That exact URL 404s; only the slash-less form works | Anyone following the documentation literally gets zero results |
| **BUG-002** | Error responses are inconsistent | `/us/99999` → 404 JSON `{}`, but `/us` → 404 **HTML** page that echoes the requested URL back | A client cannot parse errors uniformly; the HTML page leaks the web framework |
| **BUG-003** | `GET /de/01067` returns an impossible latitude | All three places return `"longitude": "51.05", "latitude": "14612"`. Latitude 14612 is outside `[-90, 90]`, and the values are transposed — 51.05 is Dresden's *latitude* | Any consumer plotting the point renders it off-map or throws |
| **BUG-004** | `GET /tr/34000` returns an empty place name | 200 with `"place name": ""`, alongside a valid state and coordinates | The caller gets a location it cannot label |
| **BUG-005** | `GET /at/1010` returns an empty state | 200 with both `"state"` and `"state abbreviation"` empty, though Vienna is a well-defined Austrian state | Schema validation still passes because the fields are *present* — which is exactly why a schema check alone is not sufficient coverage |

The `known-defect` mechanism is a Maven property, so it composes normally:

```xml
<excluded.groups>known-defect</excluded.groups>   <!-- default -->
```

---

## Configuration

All in `src/main/resources/config.properties`; every key is overridable by system property
(`-Dapi.base.uri=...`) or environment variable (`API_BASE_URI=...`), in that order of precedence.

| Key | Default | Purpose |
|---|---|---|
| `api.base.uri` | `https://api.zippopotam.us` | Target environment |
| `http.connection.timeout.ms` | `10000` | Connection timeout |
| `http.socket.timeout.ms` | `10000` | Socket timeout |
| `performance.response.time.sla.ms` | `3000` | Budget for `ResponseTimeTest` |
| `logging.on.failure.detail` | `ALL` | Detail logged when an assertion fails |

---

## What I would add next

Given more time, in priority order: contract tests pinned against a recorded response so provider
drift is caught without hitting the network; a WireMock stub so the negative paths (5xx, timeouts,
malformed JSON) that a live third-party API will never produce on demand can be tested properly;
and a scheduled CI run, since the value of testing someone else's API is mostly in detecting *their*
regressions.
