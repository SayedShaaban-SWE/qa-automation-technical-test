# Task 2 — Wikipedia Mobile Automation

End-to-end automation of *"save an article to a reading list"* in the Wikipedia app.

**Java 25 · Maven · Appium 10 · TestNG · AssertJ · Page Object Model**

---

## Status, stated plainly

The suite **has now been executed against a real Android emulator** (Pixel API 36, Appium 10,
`org.wikipedia` r/50598), and that run is what produced most of the locator and flow work described
below. Driving the real app invalidated a good deal of what reading the app's source had suggested —
onboarding and the search results list are Jetpack Compose now and expose no resource ids at all, the
Explore feed's search bar has become a bottom-navigation tab, and a clean profile raises four
separate first-run promos that each hide the screen beneath them. Every one of those was found by
running it, not by reading it.

**The scenario is not green yet.** As of 1 August 2026 the run reaches step 2 and fails there:

```
SearchPage did not appear within 20s
  → NoSuchElementException: org.wikipedia:id/search_src_text
```

The cause is not the app and not the framework — it is the emulator's own keyboard. Gboard raises a
first-run **"Try out your stylus"** dialog the moment the search field takes focus, and it sits over
the Wikipedia window, so the search input is not in the accessibility tree to be found. The failure
screenshot in `target/screenshots/` shows it plainly. An earlier run on the same build got past it
and failed later, at step 4's save confirmation, which is the same class of problem one screen on.

So the honest summary is: **the framework drives the app correctly and the flow is right; the
emulator image needs its own first-run state dealt with before the suite is reliably green.** The fix
belongs in one of two places, and both are small:

- Suppress the IME promo on the image once — `adb shell pm disable-user com.google.android.inputmethod.latin`,
  or use an AVD with a plain keyboard — which is the better answer, because a system dialog is
  environment setup rather than something the test should be paying for on every run.
- Or add it to `Interstitials` alongside the app's own promos, if the suite has to survive an
  arbitrary emulator it does not control.

This is written down rather than papered over because a suite that is quietly retried until it passes
is worth less than one that says exactly where it stops. [Troubleshooting](#troubleshooting) lists
both symptoms.

---

## Setup

### 1. Prerequisites

| Requirement | Check with | Notes |
|---|---|---|
| JDK 25+ | `java -version` | |
| Maven 3.9+ | `mvn -v` | |
| Node.js 18+ | `node -v` | Needed by Appium |
| Android SDK + platform-tools | `adb version` | Android Studio installs both |
| An emulator or a USB device | `adb devices` | Must list one device before running |

Set `ANDROID_HOME` if it is not already set:

```bash
export ANDROID_HOME=$HOME/Library/Android/sdk          # macOS default
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator
```

### 2. Install Appium and the Android driver

```bash
npm install -g appium
appium driver install uiautomator2

# Confirms the machine is ready — resolve anything it flags before running the suite
npx appium-doctor --android
```

You do **not** need to start `appium` yourself: the suite starts and stops its own server
(`appium.server.autostart=true`). Set it to `false` to use a server you manage or a remote grid.

### 3. Start a device

```bash
emulator -list-avds
emulator -avd <your_avd_name> &
adb devices                      # must show 'device', not 'offline'
```

### 4. Provide the Wikipedia app

**Either** install it on the device beforehand and let the suite launch it (the default —
`android.app.package=org.wikipedia`):

```bash
adb install /path/to/wikipedia.apk
```

**Or** point the suite at an APK and let Appium install it each run:

```bash
mvn test -Dapp.path=/path/to/wikipedia.apk
```

A build can be obtained from [F-Droid](https://f-droid.org/packages/org.wikipedia/) or from the
[app's release page](https://github.com/wikimedia/apps-android-wikipedia/releases).

### 5. Run

```bash
cd task2-mobile-automation
mvn test
```

Console output narrates the scenario against the numbered requirement:

```
STEP 1: Launch the app and get past first-run onboarding
STEP 2: Search for 'Artificial intelligence'
STEP 3: Open the article from the search results
...
STEP 9: Verify 'Artificial intelligence' is inside 'QA Automation List 20260731-210644'
```

Results in `target/surefire-reports/`; screenshots of any failure in `target/screenshots/`.

On a stock emulator image the run currently stops at STEP 2 — see [Status](#status-stated-plainly)
for why and for the one-line fix.

### Useful variations

```bash
mvn test -Ddevice.name="Pixel 7 API 34" -Dplatform.version=14
mvn test -Ddevice.udid=emulator-5554          # pick one of several attached devices
mvn test -Dreset.app.state=false              # keep app data between sessions (faster, less isolated)
mvn test -Dappium.server.autostart=false      # use your own / a remote Appium server
mvn test -Dtest.retry.count=0                 # disable retries while debugging
```

---

## Architecture

```
src/main/java/com/sayed/wikipedia/
├── config/      ConfigManager, MobilePlatform      layered settings; platform as an enum
├── driver/      AppiumServerManager               starts/stops a local Appium server
│                DriverFactory                     builds Android/iOS capabilities
│                DriverManager                     ThreadLocal session, safe teardown
├── pages/       BasePage + 8 page objects         one screen each
├── components/  BottomNavBar, SnackbarComponent   UI present across several screens
│                Interstitials                     dismisses first-run promos
├── listeners/   ScreenshotListener, RetryAnalyzer screenshot on failure, bounded retries
└── utils/       TestDataFactory, ScreenshotUtil

src/test/java/com/sayed/wikipedia/tests/
├── BaseTest                        session lifecycle
└── SaveArticleToReadingListTest    the scenario
```

### Design decisions worth explaining

**Page objects expose state; tests own assertions.** No page contains an assertion. Pages offer
`isDisplayed()`, `articleTitles()`, `hasList(name)` — the test decides what is correct. That is what
keeps a page reusable across positive and negative scenarios instead of hard-coding one expectation.

**Every page declares a `pageIdentifier()`.** The element that proves *this* screen, not some
intermediate one, is in front of the user. Each navigation verifies it on arrival, so a navigation
bug fails at the page boundary with *"SearchPage did not appear within 20s"* rather than three steps
later as an unexplained `NoSuchElementException`.

**Navigation methods return the next page object.** So the test reads as the journey it describes:

```java
new OnboardingPage().skipIfPresent().waitUntilLaunched().openSearch().searchFor(term);
```

**No `Thread.sleep`, anywhere.** Every interaction goes through an explicit `WebDriverWait`, and the
waits are on *conditions*, not durations — search waits for the results container to have children,
list filtering waits for a matching row. A fixed sleep is either too short (flaky) or too long
(slow), and usually both in the same suite.

**The proxy timeout is short on purpose.** `AppiumFieldDecorator` gets 2 seconds, not the full 20.
Pairing a long implicit wait with a long explicit wait multiplies them — a 20s wait on a 20s proxy
is a 400s worst case, not 20s.

**`ThreadLocal` driver.** A shared static driver is the classic reason a mobile suite becomes
inexplicably flaky once anything runs in parallel. `DriverManager.quit()` clears the thread-local in
a `finally` so a dead session can never leak into a pooled thread.

**Session per test, server per suite.** A fresh session per test means no test can inherit another's
app state; one server per suite because starting one per test would dominate the runtime.
`@AfterMethod(alwaysRun = true)` guarantees teardown even when setup fails.

**Unique test data.** The app persists reading lists on the device. A hard-coded list name would
pass on a clean emulator and then fail on every re-run — or worse, pass for the wrong reason by
finding the *previous* run's list. `TestDataFactory` timestamps the name.

**Bounded, loud retries.** One retry (configurable) absorbs the genuinely non-deterministic layer of
a mobile run — a stuck animation, an emulator hiccup. More would start hiding real intermittent
bugs. Every retry is logged at WARN so a test that only ever passes on the second attempt is
visible rather than silently green.

**TestNG here, JUnit 6 in Task 1.** Not inconsistency for its own sake: mobile needs
`IRetryAnalyzer`, `ITestListener` for screenshot-on-failure, and XML suites for per-device runs.
TestNG provides all three natively.

**Selenium is still pinned, for a different reason than it started.** `io.appium:java-client`
declares Selenium as an open range (`[4.42.0, 5.0)`), so an unpinned build resolves whatever Selenium
released most recently — which means yesterday's green build and today's red one can have identical
source. Originally the pin was a workaround: Selenium 4.46 removed
`org.openqa.selenium.ContextAware`, which java-client 9.x still implemented, and the result was a
`NoClassDefFoundError` at session creation. java-client 10.x is built against the post-removal API,
so that specific breakage is gone and the pin (`selenium-bom` at 4.46.0) now buys reproducibility
alone. Worth bumping deliberately; not worth removing.

**Interstitials are a component, not a page.** A clean profile — which `reset.app.state=true`
guarantees on every session — makes the app raise four separate first-run promos across the flow.
Each is its own window, so while one is up the screen underneath is not in the accessibility tree at
all, and the failure surfaces somewhere unrelated to the promo that caused it. `Interstitials` is a
fixed list of known promos with short probes and best-effort dismissal; it has no identifier and no
"loaded" state, which is exactly why it is not a page object. `verifyLoadedClearingPromos()` on
`BasePage` splits the ordinary wait budget into slices and re-clears between them, because some
promos only appear once the screen beneath has finished rendering — dismissing once up front is too
early. The total wait is unchanged.

---

## Locator strategy

Android resource ids are the most stable locator the platform offers — they survive translation,
theming and layout changes in a way that XPath over visible text never does. Every id here was taken
from the Wikipedia app's own source **and then confirmed against the running build**, which is a
distinction worth making: several ids that exist in the source are not what the current app puts on
screen.

| Locator | Source file |
|---|---|
| `page_save`, `nav_tab_reading_lists`, `nav_tab_home`, `nav_tab_search` | `res/values/ids.xml`, cross-checked in `NavTab.kt` and `PageActionItem.kt` |
| `main_nav_tab_layout` | `res/layout/activity_main.xml` |
| `fragment_search_results`, `search_src_text`, `search_card` | `res/layout/fragment_search.xml` |
| `page_actions_tab_layout` | `res/layout/fragment_page.xml` |
| `create_button`, `dialog_title`, `list_of_lists` | `res/layout/dialog_add_to_reading_list.xml` |
| `text_input` | `res/layout/dialog_text_input.xml` |
| `recycler_view`, `item_title` | `res/layout/fragment_reading_lists.xml`, `item_reading_list.xml` |
| `reading_list_recycler_view` | `res/layout/fragment_reading_list.xml` |
| `menu_search_lists` | `res/menu/menu_main.xml` |
| `snackbar_text`, `snackbar_action` | Wikipedia's own custom snackbar layout — **not** `com.google.android.material:id/…` |

### What running it changed

Four locators from the source-reading pass did not survive contact with the app, and each failure
mode is a different lesson:

| Was | Is now | Why |
|---|---|---|
| `search_container` identified the home screen | `main_nav_tab_layout` | The Explore feed no longer carries a search bar; search is a bottom-nav tab, reached in two hops via a card. The tab scaffold is what actually proves "the app launched" |
| `page_web_view` identified the article | `page_actions_tab_layout` | The WebView node is in the native tree *only until the WebView publishes its content*, then UiAutomator replaces it with the rendered page's nodes. Waiting on it is a race a fast device loses — it vanishes precisely because the article finished loading. The action bar is native and persists |
| `fragment_onboarding_skip_button` | contentDescription `Forward` / text `Skip` | Onboarding was rewritten in Compose and publishes no resource ids. Advancing is a bounded loop rather than a fixed tap count, because the number of onboarding pages is a product decision that changes per release |
| `page_list_item_title` per result row | rows located structurally under `fragment_search_results` | Same Compose rewrite. The container keeps its id, each result is the only clickable node beneath it, and a row's first `TextView` is its title — so titles are read positionally rather than by matching visible text, which keeps the page language- and ranking-independent |

The snackbar is the fifth: its ids look like Material Components' (`com.google.android.material:id/…`)
and are not — the app ships a custom snackbar layout under its own package. That one is easy to get
wrong and produces a component that silently never matches anything.

The save-then-add-to-list flow was traced through the app's code as well:
`ReadingListBehaviorsUtil.addToDefaultList()` raises a snackbar whose action is
`R.string.reading_list_add_to_list_button` ("Add to list") and opens `AddToReadingListDialog`. That
is the primary route the framework takes. Because a snackbar auto-dismisses, `ArticlePage` falls
back to long-pressing Save, which the app maps to the same dialog — both are real app behaviour,
not locator roulette.

**iOS.** The framework is platform-aware end to end (`MobilePlatform`, `DriverFactory`, the
`@iOSXCUITFindBy` annotations), but only the Android locators have been run. The iOS accessibility
identifiers are a starting point and should be confirmed in Appium Inspector before an iOS run is
trusted — and given how much the Android pass changed on contact with the real app, they should be
assumed wrong until they are.

---

## Configuration

`src/main/resources/config.properties`; every key is overridable by system property or environment
variable, in that order of precedence.

| Key | Default | Purpose |
|---|---|---|
| `platform` | `ANDROID` | `ANDROID` or `IOS` |
| `appium.server.autostart` | `true` | Suite manages its own Appium server |
| `appium.server.url` | `http://127.0.0.1:4723` | Server address |
| `device.name` | `Android Emulator` | Target device |
| `platform.version`, `device.udid` | *(blank)* | Optional; needed for multi-device runs |
| `app.path` | *(blank)* | Install this APK instead of using the installed build |
| `android.app.package` / `.activity` | `org.wikipedia` / `org.wikipedia.main.MainActivity` | Launch target |
| `wait.timeout.seconds` | `20` | The single explicit-wait budget |
| `wait.startup.timeout.seconds` | `45` | Longer budget for first launch |
| `reset.app.state` | `true` | Clean app data per session |
| `test.retry.count` | `1` | Retries for a failed test |
| `article.search.term` | `Artificial intelligence` | Test data |
| `reading.list.name.prefix` | `QA Automation List` | Timestamp is appended at runtime |

---

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| `ConnectException` / `Could not start a new session` | No Appium server. Either `npm install -g appium` (autostart handles the rest) or start one yourself and set `appium.server.autostart=false` |
| `Could not start a local Appium server` | Appium is not on `PATH`. `npm install -g appium && appium driver install uiautomator2` |
| `No devices are connected` | `adb devices` shows nothing — start an emulator or plug in a device |
| `NoSuchElementException` on a Wikipedia id | The app version differs from the one these locators were read from. Open Appium Inspector, find the new id, and update the single `@AndroidFindBy` in the relevant page — all locators live in `pages/` and `components/`, one per screen |
| `SearchPage did not appear within 20s`, id `search_src_text` not found | **The known failure — see [Status](#status-stated-plainly).** Almost always Gboard's "Try out your stylus" first-run dialog covering the app, not a slow emulator. Check `target/screenshots/`: if a system dialog is on top, disable the IME promo on the image (`adb shell pm disable-user com.google.android.inputmethod.latin`) rather than raising the timeout |
| A step fails with the *previous* screen still on show | A first-run promo is sitting over the target. Each promo is its own window, so the screen beneath is not in the accessibility tree at all. Add it to `Interstitials` — the list is deliberately fixed rather than a catch-all that clicks anything shaped like a close button |
| `NoClassDefFoundError: org/openqa/selenium/ContextAware` | Only affects java-client 9.x. On 10.x the class is gone from both sides; if this appears, the `selenium-bom` pin and `appium.version` in `pom.xml` have drifted apart. See "Selenium is still pinned" above |

---

## What I would add next

Get the emulator's own first-run state out of the way and take the scenario green — that is the
honest first item, and it is environment work rather than framework work. Then: a scripted AVD setup
so "a clean device" means the same thing on every machine instead of being whatever Gboard felt like
doing; a real device matrix, since one emulator is a sample size of one; negative coverage around the
same flow (duplicate list names, cancelling the create dialog, saving while offline); an Allure report
with the failure screenshot attached to the step that failed; and a CI pipeline on a hosted device
farm, because a mobile suite that only runs on one developer's laptop does not really run at all.
