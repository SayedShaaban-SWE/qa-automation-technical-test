package com.sayed.wikipedia.driver;

import com.sayed.wikipedia.config.ConfigManager;
import com.sayed.wikipedia.config.MobilePlatform;
import com.sayed.wikipedia.exceptions.FrameworkException;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Builds a platform-appropriate driver from configuration.
 *
 * <p>A factory rather than a constructor call in the base test: the test knows it needs "a driver",
 * not which options object to populate. Adding a platform, a device farm or a new capability
 * touches only this class.
 */
public final class DriverFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DriverFactory.class);
    private static final ConfigManager CONFIG = ConfigManager.get();

    private DriverFactory() {
        throw new AssertionError("Factory - not instantiable");
    }

    public static AppiumDriver create() {
        MobilePlatform platform = CONFIG.platform();
        URL serverUrl = toUrl(AppiumServerManager.serverUrl());
        LOG.info("Creating {} session against {}", platform, serverUrl);

        return switch (platform) {
            case ANDROID -> new AndroidDriver(serverUrl, androidOptions());
            case IOS -> new IOSDriver(serverUrl, iosOptions());
        };
    }

    private static UiAutomator2Options androidOptions() {
        UiAutomator2Options options = new UiAutomator2Options()
                .setDeviceName(CONFIG.deviceName())
                .setNewCommandTimeout(CONFIG.newCommandTimeout())
                // The app asks for notification/location permissions on first run; granting them
                // up front keeps system dialogs out of the test's way.
                .setAutoGrantPermissions(true)
                // Animations are the main source of flakiness on emulators.
                .setDisableWindowAnimation(true);

        CONFIG.platformVersion().ifPresent(options::setPlatformVersion);
        CONFIG.deviceUdid().ifPresent(options::setUdid);

        // Install from an .apk when one is supplied; otherwise drive the installed build.
        CONFIG.appPath().ifPresentOrElse(
                path -> options.setApp(resolveAppPath(path)),
                () -> options
                        .setAppPackage(CONFIG.androidAppPackage())
                        .setAppActivity(CONFIG.androidAppActivity()));

        applyResetStrategy(options::setNoReset, options::setFullReset);
        return options;
    }

    private static XCUITestOptions iosOptions() {
        XCUITestOptions options = new XCUITestOptions()
                .setDeviceName(CONFIG.deviceName())
                .setNewCommandTimeout(CONFIG.newCommandTimeout())
                .setAutoAcceptAlerts(true);

        CONFIG.platformVersion().ifPresent(options::setPlatformVersion);
        CONFIG.deviceUdid().ifPresent(options::setUdid);

        CONFIG.appPath().ifPresentOrElse(
                path -> options.setApp(resolveAppPath(path)),
                () -> options.setBundleId(CONFIG.iosBundleId()));

        applyResetStrategy(options::setNoReset, options::setFullReset);
        return options;
    }

    /**
     * {@code reset.app.state=true} means every session starts from a clean install state, which is
     * what makes the reading-list assertions deterministic across re-runs.
     */
    private static void applyResetStrategy(java.util.function.Consumer<Boolean> noReset,
                                           java.util.function.Consumer<Boolean> fullReset) {
        boolean reset = CONFIG.resetAppState();
        noReset.accept(!reset);
        fullReset.accept(false); // full reset uninstalls the app; clearing data is enough and far faster
    }

    private static String resolveAppPath(String configuredPath) {
        File app = new File(configuredPath);
        if (!app.exists()) {
            throw new FrameworkException("app.path points at a file that does not exist: "
                    + app.getAbsolutePath());
        }
        return app.getAbsolutePath();
    }

    private static URL toUrl(String url) {
        try {
            return new URI(url).toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new FrameworkException("Invalid Appium server URL: " + url, e);
        }
    }
}
