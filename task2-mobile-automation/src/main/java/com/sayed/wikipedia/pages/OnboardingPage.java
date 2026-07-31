package com.sayed.wikipedia.pages;

import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import org.openqa.selenium.WebElement;

import java.time.Duration;

/**
 * The first-run onboarding carousel.
 *
 * <p>Only shown on a clean install, so every method here is tolerant of it being absent - a page
 * object that insists on a screen the app may legitimately skip is a page object that breaks the
 * second time the suite runs.
 */
public class OnboardingPage extends BasePage {

    private static final Duration ONBOARDING_POLL = Duration.ofSeconds(5);

    @AndroidFindBy(id = "org.wikipedia:id/fragment_onboarding_skip_button")
    @iOSXCUITFindBy(accessibility = "Skip")
    private WebElement skipButton;

    @Override
    protected WebElement pageIdentifier() {
        return skipButton;
    }

    /**
     * Skips onboarding when it is present and returns the home screen either way.
     *
     * <p>Returning the next page object rather than {@code void} is what lets a test read as the
     * user journey it describes: {@code new OnboardingPage().skipIfPresent().openSearch()}.
     */
    public HomePage skipIfPresent() {
        if (isPresentQuickly()) {
            log.info("Onboarding shown - skipping it");
            tap(skipButton);
        } else {
            log.info("Onboarding not shown (app already initialised)");
        }
        return new HomePage();
    }

    private boolean isPresentQuickly() {
        try {
            return new org.openqa.selenium.support.ui.WebDriverWait(driver, ONBOARDING_POLL)
                    .until(org.openqa.selenium.support.ui.ExpectedConditions.visibilityOf(skipButton)) != null;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
