package com.sayed.wikipedia.tests;

import com.sayed.wikipedia.components.BottomNavBar;
import com.sayed.wikipedia.components.SnackbarComponent;
import com.sayed.wikipedia.pages.AddToReadingListDialog;
import com.sayed.wikipedia.pages.ArticlePage;
import com.sayed.wikipedia.pages.HomePage;
import com.sayed.wikipedia.pages.OnboardingPage;
import com.sayed.wikipedia.pages.ReadingListDetailPage;
import com.sayed.wikipedia.pages.SavedListsPage;
import com.sayed.wikipedia.pages.SearchPage;
import com.sayed.wikipedia.utils.TestDataFactory;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end scenario: save an article to a newly created reading list.
 *
 * <p>The test reads as the nine steps of the requirement, one after the other, with an assertion at
 * each boundary where the app could plausibly have done the wrong thing. All the "how" - locators,
 * waits, gestures - lives in the page layer, so this class changes only when the <em>scenario</em>
 * changes, never when the app's UI does.
 *
 * <p>Assertions are hard rather than soft on purpose. Each step is a precondition for the next: if
 * the article never opened there is nothing meaningful to say about whether it was saved, and
 * collecting three cascading failures would only obscure the first real one.
 */
public class SaveArticleToReadingListTest extends BaseTest {

    @Test(description = "An article saved into a newly created reading list appears in that list")
    public void savedArticleAppearsInTheNewReadingList() {

        String articleTitle = CONFIG.articleSearchTerm();
        String readingListName = TestDataFactory.uniqueReadingListName();

        // 1 - Launch the Wikipedia application ------------------------------------------------
        step(1, "Launch the app and get past first-run onboarding");
        HomePage homePage = new OnboardingPage().skipIfPresent().waitUntilLaunched();
        assertThat(homePage.isDisplayed())
                .as("the Explore feed should be shown after launch")
                .isTrue();

        // 2 - Search for the article -----------------------------------------------------------
        step(2, "Search for '%s'".formatted(articleTitle));
        SearchPage searchPage = homePage.openSearch().searchFor(articleTitle);
        assertThat(searchPage.hasResults())
                .as("searching for '%s' should return at least one result", articleTitle)
                .isTrue();
        assertThat(searchPage.resultTitles())
                .as("the article being searched for should be among the results")
                .anyMatch(title -> title.equalsIgnoreCase(articleTitle));

        // 3 - Open the article from the search results ------------------------------------------
        step(3, "Open the article from the search results");
        ArticlePage articlePage = searchPage.openResultByTitle(articleTitle);
        assertThat(articlePage.isDisplayed())
                .as("the article should open after tapping the search result")
                .isTrue();

        // 4 - Save the article ------------------------------------------------------------------
        step(4, "Save the article");
        SnackbarComponent saveConfirmation = articlePage.saveArticle();
        assertThat(saveConfirmation.isDisplayed())
                .as("the app should confirm the article was saved")
                .isTrue();

        // 5 - Add the article to a reading list --------------------------------------------------
        step(5, "Open the 'Add to reading list' dialog");
        AddToReadingListDialog addToListDialog = articlePage.openAddToReadingListDialog(saveConfirmation);
        assertThat(addToListDialog.isDisplayed())
                .as("the reading-list chooser should be shown")
                .isTrue();

        // 6 - Create a new reading list ----------------------------------------------------------
        step(6, "Create the reading list '%s'".formatted(readingListName));
        addToListDialog.tapCreateNewList().createList(readingListName);

        // 7 - Navigate to the Reading Lists section ----------------------------------------------
        step(7, "Navigate to the Saved / reading lists tab");
        SavedListsPage savedListsPage = new BottomNavBar().openSaved();
        assertThat(savedListsPage.isDisplayed())
                .as("the Saved tab should be shown")
                .isTrue();

        // 8 - Search for the newly created reading list -------------------------------------------
        step(8, "Filter the reading lists by '%s'".formatted(readingListName));
        savedListsPage.searchForList(readingListName);
        assertThat(savedListsPage.hasList(readingListName))
                .as("the reading list just created should be found by name. Visible lists: %s",
                        savedListsPage.visibleListNames())
                .isTrue();

        // 9 - Verify the saved article is in the reading list --------------------------------------
        step(9, "Verify '%s' is inside '%s'".formatted(articleTitle, readingListName));
        ReadingListDetailPage readingList = savedListsPage.openList(readingListName);

        assertThat(readingList.containsArticle(articleTitle))
                .as("'%s' should be in the reading list '%s'. The list contains: %s",
                        articleTitle, readingListName, readingList.articleTitles())
                .isTrue();
        assertThat(readingList.articleCount())
                .as("a freshly created list should hold exactly the one article that was saved into it")
                .isEqualTo(1);
    }
}
