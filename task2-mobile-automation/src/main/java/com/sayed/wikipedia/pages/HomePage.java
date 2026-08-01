package com.sayed.wikipedia.pages;

import com.sayed.wikipedia.components.BottomNavBar;
import com.sayed.wikipedia.components.Interstitials;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import org.openqa.selenium.WebElement;

/**
 * The app's landing screen after launch, and the entry point to search.
 *
 * <h2>Why the identifier is the tab bar, not a search box</h2>
 * The feed used to carry a {@code search_container} bar across its top, and that was this page's
 * identifier. The bar is gone: search is now a bottom-navigation tab. The tab scaffold
 * ({@code main_nav_tab_layout}) is what actually proves "the app finished launching and the main UI
 * is up", which is the precondition the scenario's first step depends on, so that is the identifier
 * now. The feed content itself is a Compose surface with no stable handle worth asserting on.
 */
public class HomePage extends BasePage {

    @AndroidFindBy(id = "org.wikipedia:id/main_nav_tab_layout")
    @iOSXCUITFindBy(accessibility = "Explore")
    private WebElement mainTabBar;

    /** The card on the Search tab that opens the actual search input. */
    @AndroidFindBy(id = "org.wikipedia:id/search_card")
    @iOSXCUITFindBy(accessibility = "Search Wikipedia")
    private WebElement searchCard;

    @Override
    protected WebElement pageIdentifier() {
        return mainTabBar;
    }

    /** Waits for the app using the longer startup budget: this is the first screen after launch. */
    public HomePage waitUntilLaunched() {
        verifyLoaded(CONFIG.startupTimeout());
        return this;
    }

    /**
     * Opens search.
     *
     * <p>Two hops, because that is what the app now does: the Search tab lands on a recent-searches
     * screen whose card opens the real input. The promo sheet dismissed in between is a first-run
     * interstitial the app raises over the Search tab; see {@link Interstitials}.
     */
    public SearchPage openSearch() {
        log.info("Opening search from the bottom navigation");
        new BottomNavBar().openSearchTab();
        new Interstitials().dismissAll();
        tap(searchCard);

        SearchPage searchPage = new SearchPage();
        searchPage.verifyLoaded();
        return searchPage;
    }

    public BottomNavBar bottomNav() {
        return new BottomNavBar();
    }
}
