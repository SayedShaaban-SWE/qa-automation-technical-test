/**
 * Page objects for the Wikipedia app.
 *
 * <h2>Where the locators come from</h2>
 * Every Android locator in this package is a resource id taken from the Wikipedia Android app's own
 * source, not guessed from a UI dump:
 * <ul>
 *   <li>{@code res/values/ids.xml} - {@code page_save}, {@code nav_tab_reading_lists},
 *       {@code nav_tab_home}</li>
 *   <li>{@code res/layout/fragment_onboarding_pager.xml} - {@code fragment_onboarding_skip_button}</li>
 *   <li>{@code res/layout/view_search_bar.xml} - {@code search_container}, {@code search_text_view}</li>
 *   <li>{@code res/layout/fragment_search.xml} - {@code fragment_search_results}</li>
 *   <li>{@code res/layout/item_page_list_entry.xml} - {@code page_list_item_title}</li>
 *   <li>{@code res/layout/fragment_page.xml} - {@code page_web_view}, {@code page_actions_tab_layout}</li>
 *   <li>{@code res/layout/dialog_add_to_reading_list.xml} - {@code create_button}, {@code list_of_lists}</li>
 *   <li>{@code res/layout/dialog_text_input.xml} - {@code text_input}</li>
 *   <li>{@code res/layout/fragment_reading_lists.xml} - {@code recycler_view}</li>
 *   <li>{@code res/layout/item_reading_list.xml} - {@code item_title}</li>
 *   <li>{@code res/layout/fragment_reading_list.xml} - {@code reading_list_recycler_view}</li>
 *   <li>{@code res/menu/menu_main.xml} - {@code menu_search_lists}</li>
 * </ul>
 * Resource ids are the most stable Android locator available: they survive translation, theming and
 * layout changes in a way that XPath over text never does. Where an id is genuinely absent the code
 * falls back to an accessibility id, and only then to text.
 *
 * <h2>iOS</h2>
 * The framework is platform-aware end to end ({@code MobilePlatform}, {@code DriverFactory}, the
 * {@code @iOSXCUITFindBy} annotations below), but only the Android locators are source-verified.
 * The iOS accessibility identifiers are a starting point taken from the app's standard labels and
 * should be confirmed against an Appium Inspector session before an iOS run is trusted.
 */
package com.sayed.wikipedia.pages;
