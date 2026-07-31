package com.sayed.wikipedia.utils;

import com.sayed.wikipedia.config.ConfigManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Generates the run-specific data the scenario needs.
 *
 * <p>The reading list name carries a timestamp because the app persists lists on the device. A
 * hard-coded name would pass on a clean emulator and then fail - or worse, pass for the wrong
 * reason by finding last run's list - on every subsequent run.
 */
public final class TestDataFactory {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private TestDataFactory() {
        throw new AssertionError("Utility class - not instantiable");
    }

    public static String uniqueReadingListName() {
        return "%s %s".formatted(
                ConfigManager.get().readingListNamePrefix(),
                LocalDateTime.now().format(TIMESTAMP));
    }
}
