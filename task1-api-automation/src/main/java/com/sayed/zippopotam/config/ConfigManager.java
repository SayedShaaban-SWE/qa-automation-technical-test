package com.sayed.zippopotam.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;

/**
 * Single, immutable source of configuration for the framework.
 *
 * <p>Resolution order for every key is <em>system property &rarr; environment variable &rarr;
 * {@code config.properties}</em>. That ordering is what lets the same artefact run unchanged on a
 * developer machine, in CI and against a different environment, e.g.
 * {@code mvn test -Dapi.base.uri=https://staging.example.com}.
 *
 * <p>Environment variables use the SCREAMING_SNAKE_CASE form of the key
 * ({@code api.base.uri} &rarr; {@code API_BASE_URI}), because dots are not legal in shell
 * variable names.
 *
 * <p>Implemented as an eagerly-initialised enum-free singleton: the properties file is read once,
 * and the instance is safely published by the JVM class-initialisation guarantees.
 */
public final class ConfigManager {

    private static final System.Logger LOG = System.getLogger(ConfigManager.class.getName());
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
                throw new ConfigurationException(CONFIG_FILE + " was not found on the classpath");
            }
            props.load(in);
            LOG.log(System.Logger.Level.DEBUG, () -> "Loaded " + props.size() + " keys from " + CONFIG_FILE);
            return props;
        } catch (IOException e) {
            throw new ConfigurationException("Unable to read " + CONFIG_FILE, e);
        }
    }

    // ------------------------------------------------------------------ raw access

    /**
     * @throws ConfigurationException if the key is defined nowhere - failing fast beats running a
     *                                suite against {@code null}.
     */
    public String getString(String key) {
        String value = resolve(key);
        if (value == null || value.isBlank()) {
            throw new ConfigurationException("Missing configuration key: '" + key + "'");
        }
        return value.trim();
    }

    public String getString(String key, String defaultValue) {
        String value = resolve(key);
        return (value == null || value.isBlank()) ? defaultValue : value.trim();
    }

    public int getInt(String key) {
        String value = getString(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ConfigurationException("Key '" + key + "' is not an integer: '" + value + "'", e);
        }
    }

    private String resolve(String key) {
        String fromSystemProperty = System.getProperty(key);
        if (fromSystemProperty != null) {
            return fromSystemProperty;
        }
        String fromEnvironment = System.getenv(toEnvVariableName(key));
        if (fromEnvironment != null) {
            return fromEnvironment;
        }
        return properties.getProperty(key);
    }

    private static String toEnvVariableName(String key) {
        return key.replace('.', '_').replace('-', '_').toUpperCase(Locale.ROOT);
    }

    // ------------------------------------------------------------- typed accessors
    // Named accessors keep magic strings out of the rest of the codebase; a typo becomes a
    // compile error instead of a runtime surprise.

    public String baseUri() {
        return getString("api.base.uri");
    }

    public int connectionTimeoutMs() {
        return getInt("http.connection.timeout.ms");
    }

    public int socketTimeoutMs() {
        return getInt("http.socket.timeout.ms");
    }

    public long responseTimeSlaMs() {
        return getInt("performance.response.time.sla.ms");
    }

    public String loggingOnFailureDetail() {
        return getString("logging.on.failure.detail", "ALL");
    }
}
