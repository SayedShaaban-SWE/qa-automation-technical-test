package com.sayed.wikipedia.pages;

import com.sayed.wikipedia.components.Interstitials;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

/**
 * Article search: type a term, read the results, open one.
 *
 * <h2>Why the results are located by structure rather than by id</h2>
 * The results list was rebuilt in Jetpack Compose, so the {@code page_list_item_title} id that used
 * to identify each row's title is gone - Compose publishes no resource ids here. What the tree does
 * expose is stable and unambiguous: the results container keeps its {@code fragment_search_results}
 * id, each result is the only clickable node beneath it, and a row's first {@code TextView} is its
 * title (any second one is the description).
 *
 * <p>So the row locator is anchored to a real id and the title is read positionally from within the
 * row, rather than by matching on visible text. That keeps the page working across languages and
 * across ranking changes, which is the property the id was providing before.
 */
public class SearchPage extends BasePage {

    /** Result rows are recycled, so they are located on demand rather than cached in a field. */
    private static final By RESULT_ROW = AppiumBy.xpath(
            "//*[@resource-id='org.wikipedia:id/fragment_search_results']"
                    + "//android.view.View[@clickable='true']");

    private static final By ROW_TEXT = By.className("android.widget.TextView");

    @AndroidFindBy(id = "org.wikipedia:id/search_src_text")
    @iOSXCUITFindBy(accessibility = "Search Wikipedia")
    private WebElement searchInput;

    @AndroidFindBy(id = "org.wikipedia:id/fragment_search_results")
    @iOSXCUITFindBy(className = "XCUIElementTypeTable")
    private WebElement resultsList;

    @Override
    protected WebElement pageIdentifier() {
        return searchInput;
    }

    public SearchPage searchFor(String term) {
        log.info("Searching for '{}'", term);
        type(searchInput, term);
        // Wait for the results container rather than a fixed pause: search is debounced and
        // network-backed, so its latency varies with the device and connection.
        wait.until(ExpectedConditions.visibilityOf(resultsList));
        wait.until(driver -> !driver.findElements(RESULT_ROW).isEmpty());
        return this;
    }

    public List<String> resultTitles() {
        return findAll(RESULT_ROW).stream().map(SearchPage::titleOf).toList();
    }

    public boolean hasResults() {
        return !findAll(RESULT_ROW).isEmpty();
    }

    /**
     * Opens the first result whose title matches {@code title}, case-insensitively.
     *
     * <p>Matching on the title instead of blindly tapping index 0 means the test verifies it opened
     * the article it asked for. Wikipedia's ranking can and does change; the assertion should not.
     */
    public ArticlePage openResultByTitle(String title) {
        WebElement match = findAll(RESULT_ROW).stream()
                .filter(row -> titleOf(row).equalsIgnoreCase(title))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "No search result titled '" + title + "'. Results were: " + resultTitles()));

        log.info("Opening search result '{}'", titleOf(match));
        match.click();
        return openedArticle();
    }

    /** Fallback for terms whose exact title differs from the query (e.g. a redirect). */
    public ArticlePage openFirstResult() {
        List<WebElement> results = findAll(RESULT_ROW);
        if (results.isEmpty()) {
            throw new NoSuchElementException("Search returned no results");
        }
        log.info("Opening first search result '{}'", titleOf(results.get(0)));
        results.get(0).click();
        return openedArticle();
    }

    /**
     * An opening article attracts two first-run promos - a dialog as it opens and a toolbar coach
     * mark once it has rendered - and each is a window that hides the article underneath it. They
     * have to go before the page can be verified, or they swallow the Save tap that follows.
     */
    private ArticlePage openedArticle() {
        ArticlePage articlePage = new ArticlePage();
        articlePage.verifyLoadedClearingPromos();
        return articlePage;
    }

    /** A row's title is its first text node; a second one, when present, is the description. */
    private static String titleOf(WebElement row) {
        List<WebElement> texts = row.findElements(ROW_TEXT);
        return texts.isEmpty() ? "" : texts.get(0).getText();
    }
}
