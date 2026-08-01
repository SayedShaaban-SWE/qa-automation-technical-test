# Technical Test — API & Mobile Automation

Two independent Java automation projects, one per task.

| | Task | Stack | Status |
|---|---|---|---|
| **Task 1** | [`task1-api-automation/`](task1-api-automation) — REST API tests for `api.zippopotam.us` | Java 25 · Maven · REST Assured 6 · JUnit 6 · AssertJ | **83 tests, all passing** (verified locally, 1 Aug 2026) |
| **Task 2** | [`task2-mobile-automation/`](task2-mobile-automation) — E2E "save an article to a reading list" on the Wikipedia app | Java 25 · Maven · Appium 10 · TestNG · AssertJ | **Runs against a real emulator; not green yet** — blocked by a system keyboard dialog, not by the app or the framework (see below) |

Each project has its own README with full setup and run instructions:

- **[Task 1 README →](task1-api-automation/README.md)**
- **[Task 2 README →](task2-mobile-automation/README.md)**

---

## Quick start

**Prerequisites shared by both:** JDK 25+ and Maven 3.9+.

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

The mobile suite **has been executed against a real Android emulator** (Pixel API 36, Appium 10,
`org.wikipedia` r/50598), and that is where most of its current shape came from. Reading the app's
source got the first draft; running it rewrote a good deal of it. Onboarding and the search results
list are Jetpack Compose now and publish no resource ids at all; the Explore feed's search bar has
become a bottom-navigation tab; the article's WebView node disappears from the native tree at exactly
the moment the article finishes loading; and a clean profile raises four separate first-run promos,
each of which is its own window that hides the screen underneath. None of that is visible from the
source alone.

**The scenario is not green yet, and the reason is the emulator rather than the app.** The run
reaches step 2 and stops there because Gboard raises a *"Try out your stylus"* first-run dialog over
the Wikipedia window the moment the search field takes focus, so the search input is not in the
accessibility tree to be found. The failure screenshot shows it directly. Disabling that system promo
on the image — or scripting the AVD so "clean device" means the same thing every time — is the fix,
and it is environment setup rather than framework work.

That is recorded here rather than smoothed over, for the same reason Task 1 keeps its five failing
defect tests: a suite that reports precisely where it stops is worth more than one tuned until it
looks green. The Task 2 README has the full detail, including what each locator changed from and why.
