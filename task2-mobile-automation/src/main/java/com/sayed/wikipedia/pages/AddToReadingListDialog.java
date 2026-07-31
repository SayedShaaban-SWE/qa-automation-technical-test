package com.sayed.wikipedia.pages;

import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * The "Add to reading list" bottom sheet: pick an existing list, or create a new one.
 */
public class AddToReadingListDialog extends BasePage {

    private static final By LIST_TITLE = By.id("org.wikipedia:id/item_title");

    @AndroidFindBy(id = "org.wikipedia:id/dialog_title")
    @iOSXCUITFindBy(accessibility = "Add to reading list")
    private WebElement dialogTitle;

    @AndroidFindBy(id = "org.wikipedia:id/create_button")
    @iOSXCUITFindBy(accessibility = "Create new")
    private WebElement createNewListButton;

    @Override
    protected WebElement pageIdentifier() {
        return dialogTitle;
    }

    public List<String> existingListNames() {
        return findAll(LIST_TITLE).stream().map(WebElement::getText).toList();
    }

    public CreateReadingListDialog tapCreateNewList() {
        log.info("Creating a new reading list");
        tap(createNewListButton);
        CreateReadingListDialog dialog = new CreateReadingListDialog();
        dialog.verifyLoaded();
        return dialog;
    }

    /** Adds the article to a list that already exists. */
    public void selectExistingList(String listName) {
        WebElement list = findAll(LIST_TITLE).stream()
                .filter(element -> element.getText().equals(listName))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "No reading list named '" + listName + "'. Available: " + existingListNames()));
        list.click();
    }
}
