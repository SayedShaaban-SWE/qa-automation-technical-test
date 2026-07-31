package com.sayed.wikipedia.pages;

import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

/**
 * Article search: type a term, read the results, open one.
 */
public class SearchPage extends BasePage {

    /** Result rows are recycled, so they are located on demand rather than cached in a field. */
    private static final By RESULT_TITLE = By.id("org.wikipedia:id/page_list_item_title");

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
        wait.until(driver -> !driver.findElements(RESULT_TITLE).isEmpty());
        return this;
    }

    public List<String> resultTitles() {
        return findAll(RESULT_TITLE).stream().map(WebElement::getText).toList();
    }

    public boolean hasResults() {
        return !findAll(RESULT_TITLE).isEmpty();
    }

    /**
     * Opens the first result whose title matches {@code title}, case-insensitively.
     *
     * <p>Matching on the title instead of blindly tapping index 0 means the test verifies it opened
     * the article it asked for. Wikipedia's ranking can and does change; the assertion should not.
     */
    public ArticlePage openResultByTitle(String title) {
        WebElement match = findAll(RESULT_TITLE).stream()
                .filter(element -> element.getText().equalsIgnoreCase(title))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "No search result titled '" + title + "'. Results were: " + resultTitles()));

        log.info("Opening search result '{}'", match.getText());
        match.click();

        ArticlePage articlePage = new ArticlePage();
        articlePage.verifyLoaded();
        return articlePage;
    }

    /** Fallback for terms whose exact title differs from the query (e.g. a redirect). */
    public ArticlePage openFirstResult() {
        List<WebElement> results = findAll(RESULT_TITLE);
        if (results.isEmpty()) {
            throw new NoSuchElementException("Search returned no results");
        }
        log.info("Opening first search result '{}'", results.get(0).getText());
        results.get(0).click();

        ArticlePage articlePage = new ArticlePage();
        articlePage.verifyLoaded();
        return articlePage;
    }
}
