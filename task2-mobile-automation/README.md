# Task 2 — Wikipedia Mobile Automation

End-to-end automation of *"save an article to a reading list"* in the Wikipedia app.

**Java 17 · Maven · Appium 9 · TestNG · AssertJ · Page Object Model**

---

## Status, stated plainly

The project **compiles cleanly and initialises end to end** — configuration loads, the driver
factory builds valid Android capabilities, the TestNG listeners fire, and the run terminates at
exactly the expected point: a `ConnectException` reaching the Appium server, because the machine it
was built on has no Android SDK, emulator or Appium installation.

**It has not been executed against a real device.** So that this is not a wall of guessed locators,
every Android locator was read out of the Wikipedia app's own source rather than invented — see
[Locator strategy](#locator-strategy) for the exact files. The remaining risk is the app's
version-to-version drift, and [Troubleshooting](#troubleshooting) says precisely which file to edit
if a locator has moved.

---

## Setup

### 1. Prerequisites

| Requirement | Check with | Notes |
|---|---|---|
| JDK 17+ | `java -version` | |
| Maven 3.8+ | `mvn -v` | |
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

**TestNG here, JUnit 5 in Task 1.** Not inconsistency for its own sake: mobile needs
`IRetryAnalyzer`, `ITestListener` for screenshot-on-failure, and XML suites for per-device runs.
TestNG provides all three natively.

**Selenium is pinned.** `io.appium:java-client` declares Selenium as an open range
(`[4.26.0, 5.0)`), so an unpinned build resolves whatever Selenium released most recently — and
Selenium 4.46 removed `org.openqa.selenium.ContextAware`, which java-client still implements. That
produces a `NoClassDefFoundError` at session creation on a build that compiled fine yesterday. I hit
this while building the project; the `selenium-bom` pin in `pom.xml` is the fix and the comment
there explains why not to remove it.

---

## Locator strategy

Android resource ids are the most stable locator the platform offers — they survive translation,
theming and layout changes in a way that XPath over visible text never does. Every id used here was
taken from the Wikipedia app's own source, not from guesswork:

| Locator | Source file |
|---|---|
| `page_save`, `nav_tab_reading_lists`, `nav_tab_home` | `res/values/ids.xml`, cross-checked in `NavTab.kt` and `PageActionItem.kt` |
| `fragment_onboarding_skip_button` | `res/layout/fragment_onboarding_pager.xml` |
| `search_container` | `res/layout/view_search_bar.xml` |
| `fragment_search_results` | `res/layout/fragment_search.xml` |
| `page_list_item_title` | `res/layout/item_page_list_entry.xml` |
| `page_web_view`, `page_actions_tab_layout` | `res/layout/fragment_page.xml` |
| `create_button`, `dialog_title`, `list_of_lists` | `res/layout/dialog_add_to_reading_list.xml` |
| `text_input` | `res/layout/dialog_text_input.xml` |
| `recycler_view`, `item_title` | `res/layout/fragment_reading_lists.xml`, `item_reading_list.xml` |
| `reading_list_recycler_view` | `res/layout/fragment_reading_list.xml` |
| `menu_search_lists` | `res/menu/menu_main.xml` |

The save-then-add-to-list flow was traced through the app's code as well:
`ReadingListBehaviorsUtil.addToDefaultList()` raises a snackbar whose action is
`R.string.reading_list_add_to_list_button` ("Add to list") and opens `AddToReadingListDialog`. That
is the primary route the framework takes. Because a snackbar auto-dismisses, `ArticlePage` falls
back to long-pressing Save, which the app maps to the same dialog — both are real app behaviour,
not locator roulette.

**iOS.** The framework is platform-aware end to end (`MobilePlatform`, `DriverFactory`, the
`@iOSXCUITFindBy` annotations), but only the Android locators are source-verified. The iOS
accessibility identifiers are a starting point and should be confirmed in Appium Inspector before an
iOS run is trusted.

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
| `SearchPage did not appear within 20s` | Usually a slow emulator. Raise `wait.timeout.seconds` |
| `NoClassDefFoundError: org/openqa/selenium/ContextAware` | The Selenium pin in `pom.xml` was removed or bumped past java-client's compatibility. See "Selenium is pinned" above |

---

## What I would add next

Run it against a real device matrix and fix whatever locator drift shows up — that is the honest
first item. Then: negative coverage around the same flow (duplicate list names, cancelling the
create dialog, saving while offline), an Allure report with the screenshot attached to the failing
step, and a CI pipeline on a hosted device farm, since a mobile suite that only runs on one
developer's emulator does not really run at all.
