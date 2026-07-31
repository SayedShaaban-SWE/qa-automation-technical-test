package com.sayed.wikipedia.components;

import com.sayed.wikipedia.pages.BasePage;
import com.sayed.wikipedia.pages.HomePage;
import com.sayed.wikipedia.pages.SavedListsPage;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import org.openqa.selenium.WebElement;

/**
 * The persistent bottom navigation bar.
 *
 * <p>A component, not a page: it is present on top of several screens, and duplicating its two
 * locators into each of them is exactly the copy-paste that makes a page layer expensive to change.
 *
 * <p>Tab ids come from {@code NavTab.kt} / {@code res/values/ids.xml}.
 */
public class BottomNavBar extends BasePage {

    @AndroidFindBy(id = "org.wikipedia:id/nav_tab_home")
    @iOSXCUITFindBy(accessibility = "Explore")
    private WebElement exploreTab;

    @AndroidFindBy(id = "org.wikipedia:id/nav_tab_reading_lists")
    @iOSXCUITFindBy(accessibility = "Saved")
    private WebElement savedTab;

    @Override
    protected WebElement pageIdentifier() {
        return savedTab;
    }

    public SavedListsPage openSaved() {
        log.info("Navigating to the Saved (reading lists) tab");
        tap(savedTab);
        SavedListsPage page = new SavedListsPage();
        page.verifyLoaded();
        return page;
    }

    public HomePage openExplore() {
        log.info("Navigating to the Explore tab");
        tap(exploreTab);
        HomePage page = new HomePage();
        page.verifyLoaded();
        return page;
    }
}
