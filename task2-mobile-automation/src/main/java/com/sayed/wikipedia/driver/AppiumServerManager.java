package com.sayed.wikipedia.driver;

import com.sayed.wikipedia.config.ConfigManager;
import com.sayed.wikipedia.exceptions.FrameworkException;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Starts and stops a local Appium server for the duration of the suite.
 *
 * <p>Rationale: "run {@code appium} in another terminal first" is the single most common reason a
 * reviewer cannot run a mobile project. Managing the server from the suite removes that step, and
 * setting {@code appium.server.autostart=false} hands control back for CI or a remote grid.
 *
 * <p>Lifecycle is suite-scoped, not test-scoped - starting a server per test would dominate the
 * runtime.
 */
public final class AppiumServerManager {

    private static final Logger LOG = LoggerFactory.getLogger(AppiumServerManager.class);
    private static final ConfigManager CONFIG = ConfigManager.get();

    private static AppiumDriverLocalService service;

    private AppiumServerManager() {
        throw new AssertionError("Utility class - not instantiable");
    }

    public static synchronized void start() {
        if (!CONFIG.autoStartAppiumServer()) {
            LOG.info("appium.server.autostart=false - expecting a server already listening on {}",
                    CONFIG.appiumServerUrl());
            return;
        }
        if (service != null && service.isRunning()) {
            LOG.debug("Appium server already running on {}", service.getUrl());
            return;
        }

        URI serverUri = parse(CONFIG.appiumServerUrl());
        LOG.info("Starting Appium server on {}:{}", serverUri.getHost(), serverUri.getPort());

        service = new AppiumServiceBuilder()
                .withIPAddress(serverUri.getHost())
                .usingPort(serverUri.getPort())
                // Without SESSION_OVERRIDE a crashed previous run leaves a session that blocks the next one.
                .withArgument(GeneralServerFlag.SESSION_OVERRIDE)
                .withArgument(GeneralServerFlag.LOG_LEVEL, "error")
                .withTimeout(CONFIG.appiumServerStartupTimeout())
                .build();

        try {
            service.start();
        } catch (RuntimeException e) {
            throw new FrameworkException(
                    "Could not start a local Appium server. Either install it (npm i -g appium) or "
                            + "set appium.server.autostart=false and start one yourself.", e);
        }

        if (!service.isRunning()) {
            throw new FrameworkException("Appium server failed to reach a running state");
        }
        LOG.info("Appium server is up at {}", service.getUrl());
    }

    public static synchronized void stop() {
        if (service != null && service.isRunning()) {
            LOG.info("Stopping Appium server");
            service.stop();
        }
        service = null;
    }

    /** The URL sessions should connect to: the managed server if there is one, otherwise the configured one. */
    public static String serverUrl() {
        return (service != null && service.isRunning())
                ? service.getUrl().toString()
                : CONFIG.appiumServerUrl();
    }

    private static URI parse(String url) {
        try {
            URI uri = new URI(url);
            if (uri.getHost() == null || uri.getPort() == -1) {
                throw new FrameworkException("appium.server.url must include host and port, e.g. http://127.0.0.1:4723");
            }
            return uri;
        } catch (URISyntaxException e) {
            throw new FrameworkException("Malformed appium.server.url: " + url, e);
        }
    }
}
