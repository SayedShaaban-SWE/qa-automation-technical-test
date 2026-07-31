package com.sayed.wikipedia.pages;

import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Locale;

/**
 * The contents of a single reading list - the screen the scenario's final assertion is made against.
 *
 * <p>Article rows reuse {@code item_page_list_entry.xml}, the same layout as search results, so the
 * title id is the same {@code page_list_item_title}.
 */
public class ReadingListDetailPage extends BasePage {

    private static final By ARTICLE_TITLE = By.id("org.wikipedia:id/page_list_item_title");

    @AndroidFindBy(id = "org.wikipedia:id/reading_list_recycler_view")
    @iOSXCUITFindBy(className = "XCUIElementTypeTable")
    private WebElement articlesRecycler;

    @Override
    protected WebElement pageIdentifier() {
        return articlesRecycler;
    }

    public List<String> articleTitles() {
        return findAll(ARTICLE_TITLE).stream().map(WebElement::getText).toList();
    }

    /**
     * Whether an article is in this list.
     *
     * <p>Scrolls first: a list long enough to need scrolling would otherwise report a false
     * negative purely because the row is off screen. The scroll is a no-op when the row is already
     * visible.
     */
    public boolean containsArticle(String articleTitle) {
        if (isTitleVisible(articleTitle)) {
            return true;
        }
        try {
            scrollToText(articleTitle);
        } catch (RuntimeException e) {
            log.debug("Could not scroll to '{}': {}", articleTitle, e.getMessage());
            return false;
        }
        return isTitleVisible(articleTitle);
    }

    public int articleCount() {
        return findAll(ARTICLE_TITLE).size();
    }

    private boolean isTitleVisible(String articleTitle) {
        return articleTitles().stream()
                .anyMatch(title -> title.toLowerCase(Locale.ROOT).equals(articleTitle.toLowerCase(Locale.ROOT)));
    }
}
