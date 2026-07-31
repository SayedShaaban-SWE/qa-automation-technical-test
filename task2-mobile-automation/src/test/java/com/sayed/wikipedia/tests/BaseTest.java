package com.sayed.wikipedia.tests;

import com.sayed.wikipedia.config.ConfigManager;
import com.sayed.wikipedia.driver.AppiumServerManager;
import com.sayed.wikipedia.driver.DriverFactory;
import com.sayed.wikipedia.driver.DriverManager;
import io.appium.java_client.AppiumDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

/**
 * Session lifecycle for every test.
 *
 * <p>Scoping is deliberate:
 * <ul>
 *   <li><b>Suite</b> - the Appium server. Starting one per test would dominate the runtime.</li>
 *   <li><b>Method</b> - the driver session. A fresh session per test means one test can never
 *       inherit another's app state, which is the difference between a suite you can trust and one
 *       whose results depend on execution order.</li>
 * </ul>
 *
 * <p>{@code @AfterMethod(alwaysRun = true)} matters: without it a failure in setup leaks the
 * session, and the next test fails for a reason that has nothing to do with the app.
 */
public abstract class BaseTest {

    private static final Logger LOG = LoggerFactory.getLogger(BaseTest.class);

    protected static final ConfigManager CONFIG = ConfigManager.get();

    @BeforeSuite(alwaysRun = true)
    public void startAppiumServer() {
        AppiumServerManager.start();
    }

    @BeforeMethod(alwaysRun = true)
    public void createSession() {
        LOG.info("Creating a {} session on '{}'", CONFIG.platform(), CONFIG.deviceName());
        AppiumDriver driver = DriverFactory.create();
        DriverManager.set(driver, CONFIG.platform());
    }

    @AfterMethod(alwaysRun = true)
    public void quitSession() {
        DriverManager.quit();
    }

    @AfterSuite(alwaysRun = true)
    public void stopAppiumServer() {
        AppiumServerManager.stop();
    }

    /**
     * Logs a numbered scenario step.
     *
     * <p>Maps the executing code back to the numbered steps in the requirement, so a console log or
     * CI output can be read against the specification without reading the code.
     */
    protected void step(int number, String description) {
        LOG.info("STEP {}: {}", number, description);
    }
}
