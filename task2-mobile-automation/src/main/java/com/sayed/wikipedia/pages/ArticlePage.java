package com.sayed.wikipedia.pages;

import com.sayed.wikipedia.components.SnackbarComponent;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.Optional;

/**
 * A rendered article, and the action bar underneath it.
 *
 * <p>The article body is a WebView, so the only reliable native handles are the action-bar buttons
 * beneath it. That is deliberate: reaching into the WebView's DOM to read a heading would couple the
 * suite to Wikipedia's page markup, which changes far more often than the app's own layout.
 *
 * <h2>Why the identifier is the action bar and not the WebView</h2>
 * {@code page_web_view} looks like the obvious identifier and is not usable as one: it is only in
 * the native tree until the WebView publishes its own content, at which point UiAutomator replaces
 * that node with the rendered page's accessibility nodes and the id stops resolving. Waiting on it
 * is therefore a race that a fast device loses - it disappears precisely because the article
 * finished loading. The action bar is native, is present for as long as an article is open, and is
 * what this page actually operates on.
 */
public class ArticlePage extends BasePage {

    @AndroidFindBy(id = "org.wikipedia:id/page_actions_tab_layout")
    @iOSXCUITFindBy(className = "XCUIElementTypeToolbar")
    private WebElement articleActionBar;

    @AndroidFindBy(id = "org.wikipedia:id/page_save")
    @iOSXCUITFindBy(accessibility = "Save for later")
    private WebElement saveButton;

    /**
     * Entries of the context menu a long press on Save raises once the article is already saved
     * ("Add to another reading list", "Move from Saved to another reading list", "Remove from
     * Saved"). They share one id and are told apart by their label.
     */
    private static final By SAVE_MENU_ENTRY = By.id("org.wikipedia:id/title");

    private static final String ADD_TO_ANOTHER_LIST = "Add to another reading list";

    @Override
    protected WebElement pageIdentifier() {
        return articleActionBar;
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
     * falls back to long-pressing Save. Both routes are real app behaviour - this is not locator
     * guesswork.
     *
     * <p>The two routes do not land in the same place. The snackbar action opens the chooser
     * directly; a long press opens a context menu whose "Add to another reading list" entry is what
     * opens the chooser. Treating the long press as if it reached the chooser is what made this
     * step time out against an already-saved article.
     */
    public AddToReadingListDialog openAddToReadingListDialog(SnackbarComponent snackbar) {
        if (snackbar.tapAction()) {
            log.info("Opened the reading-list dialog via the snackbar action");
        } else {
            log.info("Snackbar had already dismissed - long-pressing Save instead");
            longPress(saveButton);
            openChooserFromSaveMenu();
        }
        AddToReadingListDialog dialog = new AddToReadingListDialog();
        dialog.verifyLoaded();
        return dialog;
    }

    /**
     * Steps through the long-press context menu to the list chooser.
     *
     * <p>A no-op when the menu is not showing: the app raises it only for an article that is
     * already saved, and the caller's next action verifies the chooser either way.
     */
    private void openChooserFromSaveMenu() {
        findAll(SAVE_MENU_ENTRY).stream()
                .filter(entry -> ADD_TO_ANOTHER_LIST.equals(textOf(entry)))
                .findFirst()
                .ifPresent(entry -> {
                    log.info("Choosing '{}' from the Save context menu", ADD_TO_ANOTHER_LIST);
                    entry.click();
                });
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
