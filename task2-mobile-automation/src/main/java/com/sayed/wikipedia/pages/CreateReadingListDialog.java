package com.sayed.wikipedia.pages;

import com.sayed.wikipedia.components.SnackbarComponent;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * The name-your-list dialog.
 *
 * <p>Confirming here does two things in one step: it creates the list and adds the current article
 * to it. The optional description field is intentionally left alone - the scenario does not call
 * for it, and filling fields a user would not fill is how a test starts passing for the wrong reason.
 */
public class CreateReadingListDialog extends BasePage {

    /** The framework's own confirm button, not one of Wikipedia's ids. */
    private static final By CONFIRM_BUTTON = By.id("android:id/button1");

    @AndroidFindBy(id = "org.wikipedia:id/text_input")
    @iOSXCUITFindBy(className = "XCUIElementTypeTextField")
    private WebElement listNameInput;

    @Override
    protected WebElement pageIdentifier() {
        return listNameInput;
    }

    public CreateReadingListDialog enterName(String listName) {
        log.info("Naming the new reading list '{}'", listName);
        type(listNameInput, listName);
        return this;
    }

    /**
     * Confirms creation.
     *
     * @return the snackbar the app raises to confirm the article was added
     */
    public SnackbarComponent confirm() {
        hideKeyboardIfShown();
        tap(CONFIRM_BUTTON);
        return new SnackbarComponent();
    }

    public SnackbarComponent createList(String listName) {
        return enterName(listName).confirm();
    }
}
