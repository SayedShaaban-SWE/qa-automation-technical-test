package com.sayed.wikipedia.driver;

import com.sayed.wikipedia.config.MobilePlatform;
import com.sayed.wikipedia.exceptions.FrameworkException;
import io.appium.java_client.AppiumDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Owns the driver instance for the current thread.
 *
 * <p>{@code ThreadLocal} rather than a static field: TestNG can run methods and classes in parallel,
 * and a shared static driver is the classic way a mobile suite becomes unexplainably flaky. Each
 * thread gets its own session, and {@link #quit()} guarantees the reference is removed so the
 * thread-local cannot leak a dead session into a pooled thread.
 */
public final class DriverManager {

    private static final Logger LOG = LoggerFactory.getLogger(DriverManager.class);
    private static final ThreadLocal<AppiumDriver> DRIVER = new ThreadLocal<>();
    private static final ThreadLocal<MobilePlatform> PLATFORM = new ThreadLocal<>();

    private DriverManager() {
        throw new AssertionError("Utility class - not instantiable");
    }

    public static void set(AppiumDriver driver, MobilePlatform platform) {
        DRIVER.set(driver);
        PLATFORM.set(platform);
    }

    public static AppiumDriver get() {
        AppiumDriver driver = DRIVER.get();
        if (driver == null) {
            throw new FrameworkException(
                    "No driver for thread '" + Thread.currentThread().getName()
                            + "'. A test must extend BaseTest so the session is created in @BeforeMethod.");
        }
        return driver;
    }

    public static boolean hasDriver() {
        return DRIVER.get() != null;
    }

    public static MobilePlatform platform() {
        MobilePlatform platform = PLATFORM.get();
        if (platform == null) {
            throw new FrameworkException("Platform requested before a session was created");
        }
        return platform;
    }

    /** Quits the session and clears the thread-local, even if quitting fails. */
    public static void quit() {
        AppiumDriver driver = DRIVER.get();
        if (driver == null) {
            return;
        }
        try {
            driver.quit();
        } catch (RuntimeException e) {
            // A session that already died on the device must not mask the test's own result.
            LOG.warn("Ignoring failure while quitting the driver: {}", e.getMessage());
        } finally {
            DRIVER.remove();
            PLATFORM.remove();
        }
    }
}
