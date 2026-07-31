package com.sayed.wikipedia.utils;

import com.sayed.wikipedia.config.ConfigManager;
import com.sayed.wikipedia.driver.DriverManager;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Captures a screenshot on failure.
 *
 * <p>Every method here swallows its own errors. A failed screenshot must never replace the real
 * test failure in the report - the reviewer needs to see why the test failed, not why the
 * screenshot did.
 */
public final class ScreenshotUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ScreenshotUtil.class);
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private ScreenshotUtil() {
        throw new AssertionError("Utility class - not instantiable");
    }

    /** @return the path written, or empty if a screenshot could not be taken */
    public static Optional<Path> capture(String testName) {
        if (!DriverManager.hasDriver()) {
            LOG.debug("No active session - skipping screenshot for {}", testName);
            return Optional.empty();
        }
        try {
            byte[] image = ((TakesScreenshot) DriverManager.get()).getScreenshotAs(OutputType.BYTES);

            Path directory = Path.of(ConfigManager.get().screenshotDirectory());
            Files.createDirectories(directory);

            Path file = directory.resolve("%s_%s.png".formatted(
                    sanitise(testName), LocalDateTime.now().format(TIMESTAMP)));
            Files.write(file, image);

            LOG.info("Screenshot written to {}", file.toAbsolutePath());
            return Optional.of(file);
        } catch (IOException | RuntimeException e) {
            LOG.warn("Could not capture a screenshot for {}: {}", testName, e.getMessage());
            return Optional.empty();
        }
    }

    private static String sanitise(String testName) {
        return testName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
