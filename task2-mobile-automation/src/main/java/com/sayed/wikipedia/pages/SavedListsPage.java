package com.sayed.wikipedia.pages;

import com.sayed.wikipedia.components.Interstitials;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * The Saved tab: every reading list the user owns, with a filter.
 *
 * <p>The filter is an action-mode {@code SearchView} opened from the toolbar
 * ({@code menu_search_lists} in {@code menu_main.xml}); its text field is the framework-standard
 * {@code search_src_text}.
 */
public class SavedListsPage extends BasePage {

    private static final By LIST_TITLE = By.id("org.wikipedia:id/item_title");

    @AndroidFindBy(id = "org.wikipedia:id/recycler_view")
    @iOSXCUITFindBy(className = "XCUIElementTypeTable")
    private WebElement readingListsRecycler;

    @AndroidFindBy(id = "org.wikipedia:id/menu_search_lists")
    @iOSXCUITFindBy(accessibility = "Search")
    private WebElement filterListsButton;

    @AndroidFindBy(id = "org.wikipedia:id/search_src_text")
    @iOSXCUITFindBy(className = "XCUIElementTypeSearchField")
    private WebElement filterInput;

    @Override
    protected WebElement pageIdentifier() {
        return readingListsRecycler;
    }

    /** Opens the filter and narrows the visible lists down to those matching {@code query}. */
    public SavedListsPage searchForList(String query) {
        log.info("Filtering reading lists by '{}'", query);
        // The tab can carry a suggestion card above the lists; it consumes enough vertical space to
        // push rows off screen, so it goes before the list is filtered or read.
        new Interstitials().dismissAll();
        tap(filterListsButton);
        type(filterInput, query);
        // Wait for the filtered result rather than a fixed pause - filtering is asynchronous.
        wait.until(driver -> findAll(LIST_TITLE).stream()
                .anyMatch(element -> element.getText().contains(query)));
        return this;
    }

    public List<String> visibleListNames() {
        return findAll(LIST_TITLE).stream().map(WebElement::getText).toList();
    }

    public boolean hasList(String listName) {
        return visibleListNames().contains(listName);
    }

    public ReadingListDetailPage openList(String listName) {
        WebElement list = findAll(LIST_TITLE).stream()
                .filter(element -> element.getText().equals(listName))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "No reading list named '" + listName + "'. Visible lists: " + visibleListNames()));

        log.info("Opening reading list '{}'", listName);
        list.click();

        // Opening a list for the first time raises a "share this list" coach mark over it.
        ReadingListDetailPage detail = new ReadingListDetailPage();
        detail.verifyLoadedClearingPromos();
        return detail;
    }
}
