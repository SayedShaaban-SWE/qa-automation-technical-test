package com.sayed.wikipedia.pages;

import com.sayed.wikipedia.components.SnackbarComponent;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import org.openqa.selenium.WebElement;

import java.util.Optional;

/**
 * A rendered article, and the action bar underneath it.
 *
 * <p>The article body is a WebView, so the only reliable native handles are the WebView container
 * itself and the action-bar buttons. That is deliberate: reaching into the WebView's DOM to read a
 * heading would couple the suite to Wikipedia's page markup, which changes far more often than the
 * app's own layout.
 */
public class ArticlePage extends BasePage {

    @AndroidFindBy(id = "org.wikipedia:id/page_web_view")
    @iOSXCUITFindBy(className = "XCUIElementTypeWebView")
    private WebElement articleWebView;

    @AndroidFindBy(id = "org.wikipedia:id/page_save")
    @iOSXCUITFindBy(accessibility = "Save for later")
    private WebElement saveButton;

    @Override
    protected WebElement pageIdentifier() {
        return articleWebView;
    }

    /**
     * Taps Save (the bookmark). The article is added to the default "Saved" list and the app shows
     * a snackbar offering to move it to a specific list.
     */
    public SnackbarComponent saveArticle() {
        log.info("Saving the article");
        tap(saveButton);
        return new SnackbarComponent();
    }

    /**
     * Opens the "Add to reading list" dialog after saving.
     *
     * <p>Primary route is the snackbar's "Add to list" action, which is what the app offers the
     * user. The snackbar auto-dismisses after a few seconds, so if it has already gone the method
     * falls back to long-pressing Save, which the app maps to the same dialog. Both routes are real
     * app behaviour - this is not locator guesswork.
     */
    public AddToReadingListDialog openAddToReadingListDialog(SnackbarComponent snackbar) {
        if (snackbar.tapAction()) {
            log.info("Opened the reading-list dialog via the snackbar action");
        } else {
            log.info("Snackbar had already dismissed - long-pressing Save instead");
            longPress(saveButton);
        }
        AddToReadingListDialog dialog = new AddToReadingListDialog();
        dialog.verifyLoaded();
        return dialog;
    }

    /** Convenience for the common "save, then choose a list" sequence. */
    public AddToReadingListDialog saveAndOpenReadingListDialog() {
        return openAddToReadingListDialog(saveArticle());
    }

    /** The confirmation text the app showed after saving, if it is still on screen. */
    public Optional<String> lastConfirmationMessage() {
        return new SnackbarComponent().message();
    }
}
