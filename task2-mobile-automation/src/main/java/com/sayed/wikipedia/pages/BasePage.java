package com.sayed.wikipedia.pages;

import com.sayed.wikipedia.components.Interstitials;
import com.sayed.wikipedia.config.ConfigManager;
import com.sayed.wikipedia.config.MobilePlatform;
import com.sayed.wikipedia.driver.DriverManager;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Behaviour shared by every page object: driver access, waiting, and the handful of interactions a
 * page is allowed to perform.
 *
 * <p>Two rules the whole page layer follows:
 * <ul>
 *   <li><b>No {@code Thread.sleep}.</b> Every interaction goes through an explicit
 *       {@link WebDriverWait}. A fixed sleep is either too short (flaky) or too long (slow), and
 *       usually both in the same suite.</li>
 *   <li><b>No assertions.</b> Pages expose state ({@code isDisplayed()}, {@code getTitles()});
 *       tests decide what is correct. That is what keeps a page reusable across positive and
 *       negative scenarios.</li>
 * </ul>
 *
 * <p>Every subclass declares a {@link #pageIdentifier()} - the element that proves this screen, and
 * not some intermediate one, is in front of the user. {@link #verifyLoaded()} is invoked by the
 * constructor of each page so a navigation bug fails at the page boundary with a clear message,
 * rather than three steps later as a confusing {@code NoSuchElementException}.
 */
public abstract class BasePage {

    protected static final ConfigManager CONFIG = ConfigManager.get();
    private static final Duration PROXY_LOOKUP_TIMEOUT = Duration.ofSeconds(2);
    /** Slices {@link #verifyLoadedClearingPromos()} splits the wait budget into. */
    private static final int PROMO_CLEAR_ATTEMPTS = 3;

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final AppiumDriver driver;
    protected final WebDriverWait wait;

    protected BasePage() {
        this.driver = DriverManager.get();
        this.wait = new WebDriverWait(driver, CONFIG.waitTimeout());
        // A short duration on the decorator on purpose: the proxied fields do their own implicit
        // wait, and pairing a long one with the explicit WebDriverWait below would multiply every
        // timeout (a 20s wait on a 20s proxy is a 400s worst case, not 20s).
        PageFactory.initElements(new AppiumFieldDecorator(driver, PROXY_LOOKUP_TIMEOUT), this);
    }

    /**
     * An element that uniquely proves this screen is displayed.
     *
     * <p>Returns the {@code @AndroidFindBy}-annotated field rather than a raw {@link By} so a page
     * declares each locator exactly once. The field is a lazy proxy, so returning it does not touch
     * the device until something waits on it.
     */
    protected abstract WebElement pageIdentifier();

    /** Human-readable name used in logs and failure messages. */
    protected String pageName() {
        return getClass().getSimpleName();
    }

    // --------------------------------------------------------------- page state

    /**
     * Blocks until this page is on screen.
     *
     * @throws AssertionError-free {@link IllegalStateException} carrying the page name, so the
     *                             failure says "SearchPage never appeared" instead of dumping a raw locator.
     */
    public void verifyLoaded() {
        verifyLoaded(CONFIG.waitTimeout());
    }

    public void verifyLoaded(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(ExpectedConditions.visibilityOf(pageIdentifier()));
            log.debug("{} is displayed", pageName());
        } catch (TimeoutException | NoSuchElementException e) {
            throw new IllegalStateException(
                    pageName() + " did not appear within " + timeout.toSeconds() + "s", e);
        }
    }

    /**
     * Blocks until this page is on screen, clearing first-run promos in between.
     *
     * <p>Some promos are raised only once the screen underneath has finished rendering, so a single
     * dismissal before the wait can run too early and the promo then lands on top. Worse, a promo is
     * its own window: while one is up the page underneath is not in the accessibility tree at all,
     * so the wait cannot see the page even though it is there.
     *
     * <p>Splitting the same total budget into slices and re-clearing between them keeps the ordinary
     * case fast while making the late-promo case recoverable. The total wait is unchanged.
     */
    public void verifyLoadedClearingPromos() {
        Duration slice = CONFIG.waitTimeout().dividedBy(PROMO_CLEAR_ATTEMPTS);
        for (int attempt = 1; attempt <= PROMO_CLEAR_ATTEMPTS; attempt++) {
            new Interstitials().dismissAll();
            try {
                verifyLoaded(slice);
                return;
            } catch (IllegalStateException e) {
                if (attempt == PROMO_CLEAR_ATTEMPTS) {
                    throw e;
                }
                log.debug("{} not visible yet - clearing promos and retrying", pageName());
            }
        }
    }

    public boolean isDisplayed() {
        try {
            return wait.until(ExpectedConditions.visibilityOf(pageIdentifier())) != null;
        } catch (TimeoutException | NoSuchElementException e) {
            return false;
        }
    }

    // ------------------------------------------------------------- interactions

    protected WebElement waitForVisible(WebElement element) {
        return wait.until(ExpectedConditions.visibilityOf(element));
    }

    protected WebElement waitForVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected WebElement waitForClickable(WebElement element) {
        return wait.until(ExpectedConditions.elementToBeClickable(element));
    }

    protected void tap(WebElement element) {
        waitForClickable(element).click();
    }

    protected void tap(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }

    protected void type(WebElement element, String text) {
        WebElement field = waitForVisible(element);
        field.clear();
        field.sendKeys(text);
    }

    protected String textOf(WebElement element) {
        return waitForVisible(element).getText();
    }

    /** {@code true} if the locator resolves to a visible element within {@code timeout}. */
    protected boolean isPresent(By locator, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout)
                    .until(ExpectedConditions.visibilityOfElementLocated(locator));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Returns the first of several locators to appear, or empty if none do.
     *
     * <p>Used only where the app genuinely offers more than one route to the same action (for
     * example a transient snackbar with a long-press fallback). It is not a licence to guess at
     * locators - every locator in this project is taken from the app's own resource files.
     */
    protected Optional<WebElement> firstPresent(Duration timeout, By... locators) {
        Duration perLocator = timeout.dividedBy(Math.max(1, locators.length));
        for (By locator : locators) {
            try {
                return Optional.of(new WebDriverWait(driver, perLocator)
                        .until(ExpectedConditions.visibilityOfElementLocated(locator)));
            } catch (TimeoutException ignored) {
                log.debug("{} not present, trying the next locator", locator);
            }
        }
        return Optional.empty();
    }

    protected List<WebElement> findAll(By locator) {
        return driver.findElements(locator);
    }

    /** Long press, used where a short tap and a long press mean different things (e.g. Save). */
    protected void longPress(WebElement element) {
        String elementId = ((RemoteWebElement) waitForVisible(element)).getId();
        if (platform().isAndroid()) {
            driver.executeScript("mobile: longClickGesture", Map.of("elementId", elementId, "duration", 1000));
        } else {
            driver.executeScript("mobile: touchAndHold", Map.of("elementId", elementId, "duration", 1.0));
        }
    }

    /**
     * Scrolls the first scrollable container until an element containing {@code text} is visible.
     *
     * <p>Native scrolling primitives are used rather than a swipe loop with a counter: UiAutomator
     * and XCUITest both know when they have reached the end of a list, a swipe loop does not.
     */
    protected void scrollToText(String text) {
        if (platform().isAndroid()) {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true).instance(0))"
                            + ".scrollIntoView(new UiSelector().textContains(\"" + escape(text) + "\"))"));
        } else {
            driver.executeScript("mobile: scroll",
                    Map.of("direction", "down", "predicateString", "label CONTAINS '" + escape(text) + "'"));
        }
    }

    /** Dismisses the soft keyboard when it is up; a no-op otherwise. */
    protected void hideKeyboardIfShown() {
        try {
            if (driver instanceof io.appium.java_client.HidesKeyboard hidesKeyboard) {
                hidesKeyboard.hideKeyboard();
            }
        } catch (RuntimeException e) {
            // Appium throws when the keyboard is not up. That is not a failure worth propagating.
            log.debug("Keyboard was not shown: {}", e.getMessage());
        }
    }

    protected void navigateBack() {
        driver.navigate().back();
    }

    protected MobilePlatform platform() {
        return DriverManager.platform();
    }

    /** Picks the right locator for the platform in flight, for values that cannot be annotated. */
    protected By forPlatform(By android, By ios) {
        return platform().isAndroid() ? android : ios;
    }

    /** Escapes the quote characters that would otherwise break a UiSelector / NSPredicate string. */
    private static String escape(String text) {
        return text.replace("\"", "\\\"").replace("'", "\\'");
    }

    /** Convenience for locators that must be built from runtime data. */
    protected WebElement findByAndroidUiSelector(String uiSelector) {
        try {
            return driver.findElement(AppiumBy.androidUIAutomator(uiSelector));
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("No element matched UiSelector: " + uiSelector, e);
        }
    }
}
