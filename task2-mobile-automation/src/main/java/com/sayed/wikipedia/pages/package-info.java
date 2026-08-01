/**
 * Page objects for the Wikipedia app.
 *
 * <h2>Where the locators come from</h2>
 * Every Android locator in this package is a resource id taken from the Wikipedia Android app's own
 * source and then confirmed against a running build ({@code r/50598}, July 2026):
 * <ul>
 *   <li>{@code res/values/ids.xml} - {@code page_save}, {@code nav_tab_reading_lists},
 *       {@code nav_tab_home}, {@code nav_tab_search}</li>
 *   <li>{@code res/layout/fragment_search.xml} - {@code fragment_search_results},
 *       {@code search_src_text}, {@code search_card}</li>
 *   <li>{@code res/layout/item_page_list_entry.xml} - {@code page_list_item_title}</li>
 *   <li>{@code res/layout/fragment_page.xml} - {@code page_web_view}, {@code page_actions_tab_layout}</li>
 *   <li>{@code res/layout/dialog_add_to_reading_list.xml} - {@code create_button}, {@code list_of_lists}</li>
 *   <li>{@code res/layout/dialog_text_input.xml} - {@code text_input}</li>
 *   <li>{@code res/layout/fragment_reading_lists.xml} - {@code recycler_view}</li>
 *   <li>{@code res/layout/item_reading_list.xml} - {@code item_title}</li>
 *   <li>{@code res/layout/fragment_reading_list.xml} - {@code reading_list_recycler_view}</li>
 *   <li>{@code res/layout/activity_main.xml} - {@code main_nav_tab_layout}</li>
 *   <li>{@code res/menu/menu_main.xml} - {@code menu_search_lists}</li>
 * </ul>
 * Resource ids are the most stable Android locator available: they survive translation, theming and
 * layout changes in a way that XPath over text never does. Where an id is genuinely absent the code
 * falls back to an accessibility id, and only then to text.
 *
 * <h2>The two screens that have no ids, and why</h2>
 * Onboarding and the search-results list have been rewritten in Jetpack Compose, which publishes no
 * resource ids. There is no id to prefer on those screens, so:
 * <ul>
 *   <li>{@link com.sayed.wikipedia.pages.OnboardingPage} drives the {@code contentDescription} on
 *       the forward arrow and the visible "Skip" label - the only handles Compose exposes there.</li>
 *   <li>{@link com.sayed.wikipedia.pages.SearchPage} anchors to the surviving
 *       {@code fragment_search_results} id and then reads each row's title <em>positionally</em>,
 *       so it stays independent of language and of result ranking.</li>
 * </ul>
 * Both are documented on the classes themselves. The rest of the flow - article, save, the
 * reading-list dialogs, the Saved tab and list contents - is still XML-backed and still located by
 * id.
 *
 * <h2>iOS</h2>
 * The framework is platform-aware end to end ({@code MobilePlatform}, {@code DriverFactory}, the
 * {@code @iOSXCUITFindBy} annotations below), but only the Android locators are source-verified.
 * The iOS accessibility identifiers are a starting point taken from the app's standard labels and
 * should be confirmed against an Appium Inspector session before an iOS run is trusted.
 */
package com.sayed.wikipedia.pages;
