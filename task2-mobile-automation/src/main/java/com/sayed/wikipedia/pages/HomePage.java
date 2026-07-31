package com.sayed.wikipedia.pages;

import com.sayed.wikipedia.components.BottomNavBar;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import org.openqa.selenium.WebElement;

/**
 * The Explore feed - the app's landing screen, and the entry point to search.
 */
public class HomePage extends BasePage {

    @AndroidFindBy(id = "org.wikipedia:id/search_container")
    @iOSXCUITFindBy(accessibility = "Search Wikipedia")
    private WebElement searchBar;

    @Override
    protected WebElement pageIdentifier() {
        return searchBar;
    }

    /** Waits for the feed using the longer startup budget: this is the first screen after launch. */
    public HomePage waitUntilLaunched() {
        verifyLoaded(CONFIG.startupTimeout());
        return this;
    }

    public SearchPage openSearch() {
        log.info("Opening search from the home feed");
        tap(searchBar);
        SearchPage searchPage = new SearchPage();
        searchPage.verifyLoaded();
        return searchPage;
    }

    public BottomNavBar bottomNav() {
        return new BottomNavBar();
    }
}
