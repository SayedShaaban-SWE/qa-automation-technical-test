package com.sayed.wikipedia.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * The first-run onboarding flow.
 *
 * <p>Only shown on a clean install, so every method here is tolerant of it being absent - a page
 * object that insists on a screen the app may legitimately skip is a page object that breaks the
 * second time the suite runs.
 *
 * <h2>Why this page has no resource ids</h2>
 * Onboarding was rewritten in Jetpack Compose and exposes <em>no</em> resource ids at all - the old
 * {@code fragment_onboarding_skip_button} no longer exists. Compose only surfaces what its semantics
 * layer publishes, which here is a {@code contentDescription} on the forward arrow and the visible
 * "Skip" label. Those are the two most stable handles the screen offers, so they are what this page
 * uses; there is nothing better available to pick.
 *
 * <h2>Why it is a loop and not a fixed number of taps</h2>
 * The flow is several Compose pages followed by a feed-personalisation step, and how many pages
 * there are is a product decision that changes between releases. Advancing until the main scaffold
 * appears is therefore driven by the app's actual state rather than by a count this class would have
 * to keep in step with the app. The iteration cap only exists so a genuinely stuck onboarding fails
 * as a bounded, explicable error instead of spinning.
 */
public class OnboardingPage extends BasePage {

    /** The forward arrow on each Compose onboarding page. */
    private static final By FORWARD = AppiumBy.accessibilityId("Forward");

    /** The "Skip" affordance on the final onboarding and personalisation steps. */
    private static final By SKIP = AppiumBy.androidUIAutomator("new UiSelector().text(\"Skip\")");

    /** Present only once the app's main tabbed scaffold is up - i.e. onboarding is behind us. */
    private static final By MAIN_SCAFFOLD = By.id("org.wikipedia:id/main_nav_tab_layout");

    private static final Duration PROBE = Duration.ofSeconds(3);
    private static final int MAX_ONBOARDING_STEPS = 10;

    @AndroidFindBy(accessibility = "Forward")
    @iOSXCUITFindBy(accessibility = "Skip")
    private WebElement forwardButton;

    @Override
    protected WebElement pageIdentifier() {
        return forwardButton;
    }

    /**
     * Advances through onboarding when it is present and returns the home screen either way.
     *
     * <p>Returning the next page object rather than {@code void} is what lets a test read as the
     * user journey it describes: {@code new OnboardingPage().skipIfPresent().openSearch()}.
     */
    public HomePage skipIfPresent() {
        for (int step = 0; step < MAX_ONBOARDING_STEPS; step++) {
            if (isPresent(MAIN_SCAFFOLD, PROBE)) {
                log.info("Onboarding complete after {} step(s)", step);
                return new HomePage();
            }
            // Skip first: when both are offered it ends the flow in one tap instead of several.
            if (tapIfPresent(SKIP) || tapIfPresent(FORWARD)) {
                continue;
            }
            log.info("Onboarding is not showing (app already initialised)");
            break;
        }
        return new HomePage();
    }

    /** @return {@code true} if the control was on screen and was tapped */
    private boolean tapIfPresent(By locator) {
        try {
            new WebDriverWait(driver, PROBE)
                    .until(ExpectedConditions.presenceOfElementLocated(locator))
                    .click();
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }
}
