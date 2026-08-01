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
 * different screens.
 *
 * <p>The ids are Wikipedia's own, not the Material Components library's. The app ships a custom
 * snackbar layout, so the ids are {@code org.wikipedia:id/snackbar_*} rather than the
 * {@code com.google.android.material:id/snackbar_*} that a stock Material snackbar would carry.
 * That distinction is easy to get wrong and produces a component that silently never matches.
 *
 * <p>A snackbar auto-dismisses, so every accessor here is time-bounded and returns
 * {@link Optional}: "the snackbar was gone" is a legitimate outcome, not an error.
 */
public class SnackbarComponent extends BasePage {

    private static final By SNACKBAR_TEXT = By.id("org.wikipedia:id/snackbar_text");
    private static final By SNACKBAR_ACTION = By.id("org.wikipedia:id/snackbar_action");
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
