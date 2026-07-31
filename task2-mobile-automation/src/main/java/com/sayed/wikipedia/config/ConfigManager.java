package com.sayed.wikipedia.config;

import com.sayed.wikipedia.exceptions.FrameworkException;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

/**
 * Single source of configuration, resolved as <em>system property &rarr; environment variable &rarr;
 * {@code config.properties}</em>.
 *
 * <p>That ordering is what lets the same jar target an emulator locally and a device farm in CI
 * without a code change: {@code mvn test -Ddevice.name="Pixel 7" -Dplatform.version=14}.
 */
public final class ConfigManager {

    private static final String CONFIG_FILE = "config.properties";
    private static final ConfigManager INSTANCE = new ConfigManager();

    private final Properties properties;

    private ConfigManager() {
        this.properties = load();
    }

    public static ConfigManager get() {
        return INSTANCE;
    }

    private static Properties load() {
        Properties props = new Properties();
        try (InputStream in = ConfigManager.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (in == null) {
                throw new FrameworkException(CONFIG_FILE + " was not found on the classpath");
            }
            props.load(in);
            return props;
        } catch (IOException e) {
            throw new FrameworkException("Unable to read " + CONFIG_FILE, e);
        }
    }

    // ------------------------------------------------------------------ raw access

    public String getString(String key) {
        return optional(key).orElseThrow(
                () -> new FrameworkException("Missing configuration key: '" + key + "'"));
    }

    public String getString(String key, String defaultValue) {
        return optional(key).orElse(defaultValue);
    }

    /** Empty when unset or blank - used for capabilities that are legitimately optional. */
    public Optional<String> optional(String key) {
        String fromSystemProperty = System.getProperty(key);
        if (isPresent(fromSystemProperty)) {
            return Optional.of(fromSystemProperty.trim());
        }
        String fromEnvironment = System.getenv(toEnvVariableName(key));
        if (isPresent(fromEnvironment)) {
            return Optional.of(fromEnvironment.trim());
        }
        String fromFile = properties.getProperty(key);
        return isPresent(fromFile) ? Optional.of(fromFile.trim()) : Optional.empty();
    }

    public int getInt(String key) {
        String value = getString(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new FrameworkException("Key '" + key + "' is not an integer: '" + value + "'", e);
        }
    }

    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(getString(key));
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private static String toEnvVariableName(String key) {
        return key.replace('.', '_').replace('-', '_').toUpperCase(Locale.ROOT);
    }

    // ------------------------------------------------------------- typed accessors

    public MobilePlatform platform() {
        return MobilePlatform.from(getString("platform"));
    }

    public boolean autoStartAppiumServer() {
        return getBoolean("appium.server.autostart");
    }

    public String appiumServerUrl() {
        return getString("appium.server.url");
    }

    public Duration appiumServerStartupTimeout() {
        return Duration.ofSeconds(getInt("appium.server.startup.timeout.seconds"));
    }

    public String deviceName() {
        return getString("device.name");
    }

    public Optional<String> platformVersion() {
        return optional("platform.version");
    }

    public Optional<String> deviceUdid() {
        return optional("device.udid");
    }

    public Optional<String> appPath() {
        return optional("app.path");
    }

    public String androidAppPackage() {
        return getString("android.app.package");
    }

    public String androidAppActivity() {
        return getString("android.app.activity");
    }

    public String iosBundleId() {
        return getString("ios.bundle.id");
    }

    public Duration waitTimeout() {
        return Duration.ofSeconds(getInt("wait.timeout.seconds"));
    }

    public Duration startupTimeout() {
        return Duration.ofSeconds(getInt("wait.startup.timeout.seconds"));
    }

    public Duration newCommandTimeout() {
        return Duration.ofSeconds(getInt("appium.new.command.timeout.seconds"));
    }

    public boolean resetAppState() {
        return getBoolean("reset.app.state");
    }

    public boolean screenshotOnFailure() {
        return getBoolean("screenshot.on.failure");
    }

    public String screenshotDirectory() {
        return getString("screenshot.directory");
    }

    public int retryCount() {
        return getInt("test.retry.count");
    }

    public String articleSearchTerm() {
        return getString("article.search.term");
    }

    public String readingListNamePrefix() {
        return getString("reading.list.name.prefix");
    }
}
