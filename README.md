# Technical Test — API & Mobile Automation

Two independent Java automation projects, one per task.

| | Task | Stack | Status |
|---|---|---|---|
| **Task 1** | [`task1-api-automation/`](task1-api-automation) — REST API tests for `api.zippopotam.us` | Java 17 · Maven · REST Assured · JUnit 5 · AssertJ | **83 tests, all passing** (verified locally) |
| **Task 2** | [`task2-mobile-automation/`](task2-mobile-automation) — E2E "save an article to a reading list" on the Wikipedia app | Java 17 · Maven · Appium 9 · TestNG · AssertJ | **Compiles and wires up cleanly**; needs an emulator/device to execute (see note below) |

Each project has its own README with full setup and run instructions:

- **[Task 1 README →](task1-api-automation/README.md)**
- **[Task 2 README →](task2-mobile-automation/README.md)**

---

## Quick start

**Prerequisites shared by both:** JDK 17+ and Maven 3.8+.

```bash
# Task 1 — runs immediately, needs only an internet connection
cd task1-api-automation && mvn test

# Task 2 — additionally needs Node.js, Appium and an Android emulator (see its README)
cd task2-mobile-automation && mvn test
```

---

## What these two projects have in common

Both were built to the same set of principles, which is the point of the exercise:

**Layered, so tests only ever describe intent.**
Configuration, transport/driver, and domain (service objects / page objects) are separate layers. A
test says "look up 90210 in the US" or "open the article from the search results". *How* that
happens — a path template, a resource id, a wait strategy — lives one layer down. When the app or
the API changes, the tests do not.

**Configuration is external and layered.**
Both projects resolve every setting as *system property → environment variable → `config.properties`*,
so the same artefact runs on a laptop, in CI, or against another environment without a code change:

```bash
mvn test -Dapi.base.uri=https://staging.example.com    # Task 1
mvn test -Ddevice.name="Pixel 7 API 34"                # Task 2
```

**No hard-coded waits, no hard-coded test data.**
There is no `Thread.sleep` anywhere. Task 1's fixtures live in a CSV; Task 2 generates a
timestamped reading-list name so re-runs never collide with a previous run's leftover state.

**Assertions are domain-specific.**
Task 1 has a custom AssertJ assertion (`ZipCodeResponseAssert`) so failures read like the
requirement they broke, not like a diff of two strings:

```
places[0] ('Dresden') has coordinates outside the valid range:
  latitude=<14612> (expected -90..90), longitude=<51.05> (expected -180..180)
```

**Failing tests are kept, not weakened.**
Task 1 found five genuine defects in the live API. Each one is a real test asserting the *correct*
behaviour, tagged `known-defect` and excluded from the default run so the build stays green — with
a one-line command to run them and watch them fail. Adjusting a test to match a bug is how a defect
quietly becomes a feature.

---

## On Task 2's status — stated plainly

The mobile project **compiles cleanly and initialises end to end**: configuration loads, the driver
factory builds the Android capabilities, the TestNG listeners fire, and the run terminates at
exactly the expected point — `ConnectException` reaching the Appium server, because this machine has
no Android SDK, emulator or Appium install.

**It has not been executed against a real device.** Rather than guess at locators, every Android
locator was taken from the Wikipedia app's own source (`res/values/ids.xml`, `NavTab.kt`,
`PageActionItem.kt` and the layout XMLs) — the exact files are listed in
[`pages/package-info.java`](task2-mobile-automation/src/main/java/com/sayed/wikipedia/pages/package-info.java).
The app's version-to-version drift is the remaining risk, and the Task 2 README says exactly which
files to touch if a locator has moved.
