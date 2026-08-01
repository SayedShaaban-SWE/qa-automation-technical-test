package com.sayed.wikipedia.components;

import com.sayed.wikipedia.driver.DriverManager;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

/**
 * The first-run promotions the app raises on top of whatever screen the user is on.
 *
 * <p>These are not screens the scenario cares about, but they are modal, and because the suite runs
 * with {@code reset.app.state=true} every session gets a clean profile and therefore sees all of
 * them again. Left alone they swallow the next tap and the failure surfaces somewhere unrelated.
 *
 * <p>Deliberately <em>not</em> a page object: it has no identifier, no "loaded" state and no
 * navigation. It is a best-effort dismisser, so every probe is short and absence is the normal case
 * - a promo that is not showing must cost the run a fraction of a second, not a full wait budget.
 *
 * <p>Each entry is a promo the app genuinely shows on a clean profile in the build these locators
 * were verified against; this is a fixed list, not a catch-all that clicks anything resembling a
 * close button.
 */
public final class Interstitials {

    /** A dismissible promo: a name for the log, and the control that gets rid of it. */
    private record Promo(String name, By dismissControl) {}

    private static final List<Promo> PROMOS = List.of(
            new Promo("\"Wikipedia games\" dialog",
                    By.id("org.wikipedia:id/closeButton")),
            // The search-widget sheet is Compose, so its close control has no resource id. Scoping
            // the content-desc to the sheet keeps it from matching some other screen's "Close".
            new Promo("\"A faster way to search\" bottom sheet",
                    AppiumBy.xpath("//*[@resource-id='org.wikipedia:id/design_bottom_sheet']"
                            + "//*[@content-desc='Close']")),
            new Promo("reading-list sharing coach mark",
                    AppiumBy.xpath("//*[@resource-id='org.wikipedia:id/balloon_card']"
                            + "//*[@resource-id='org.wikipedia:id/buttonView']")),
            new Promo("personalised-feed suggestion card",
                    AppiumBy.xpath("//*[@resource-id='org.wikipedia:id/onboarding_view']"
                            + "//*[@resource-id='org.wikipedia:id/negativeButton']")));

    /** Short on purpose: this runs at screen boundaries where usually nothing is showing. */
    private static final Duration PROBE = Duration.ofMillis(750);

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final AppiumDriver driver = DriverManager.get();

    /**
     * Dismisses every known first-run promo that is currently on screen.
     *
     * <p>Safe to call at any point, and a no-op when nothing is showing.
     */
    public void dismissAll() {
        PROMOS.forEach(this::dismiss);
    }

    private void dismiss(Promo promo) {
        try {
            WebElement control = new WebDriverWait(driver, PROBE)
                    .until(ExpectedConditions.presenceOfElementLocated(promo.dismissControl()));
            control.click();
            log.info("Dismissed the {}", promo.name());
        } catch (TimeoutException e) {
            log.debug("{} is not showing", promo.name());
        } catch (RuntimeException e) {
            // It was there a moment ago and has since animated away - not a failure.
            log.debug("Could not dismiss the {}: {}", promo.name(), e.getMessage());
        }
    }
}
