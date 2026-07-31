package com.sayed.wikipedia.components;

import com.sayed.wikipedia.pages.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.Optional;

/**
 * The Material snackbar the app uses for transient confirmations.
 *
 * <p>Modelled as a component rather than folded into a page because it appears above several
 * different screens. Its ids belong to the Material Components library
 * ({@code com.google.android.material:id/...}), which is why they are not in Wikipedia's own
 * {@code ids.xml}.
 *
 * <p>A snackbar auto-dismisses, so every accessor here is time-bounded and returns
 * {@link Optional}: "the snackbar was gone" is a legitimate outcome, not an error.
 */
public class SnackbarComponent extends BasePage {

    private static final By SNACKBAR_TEXT = By.id("com.google.android.material:id/snackbar_text");
    private static final By SNACKBAR_ACTION = By.id("com.google.android.material:id/snackbar_action");
    private static final Duration SNACKBAR_TIMEOUT = Duration.ofSeconds(8);

    @Override
    protected WebElement pageIdentifier() {
        return driver.findElement(SNACKBAR_TEXT);
    }

    public Optional<String> message() {
        return firstPresent(SNACKBAR_TIMEOUT, SNACKBAR_TEXT).map(WebElement::getText);
    }

    public boolean isDisplayed() {
        return firstPresent(SNACKBAR_TIMEOUT, SNACKBAR_TEXT).isPresent();
    }

    /**
     * Taps the snackbar's action button.
     *
     * @return {@code true} if the action was still on screen and was tapped
     */
    public boolean tapAction() {
        Optional<WebElement> action = firstPresent(SNACKBAR_TIMEOUT, SNACKBAR_ACTION);
        action.ifPresent(WebElement::click);
        return action.isPresent();
    }
}
