package com.sayed.wikipedia.listeners;

import com.sayed.wikipedia.config.ConfigManager;
import com.sayed.wikipedia.utils.ScreenshotUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Captures a screenshot the moment a test fails, before teardown quits the session.
 *
 * <p>A listener rather than a try/catch in the test: cross-cutting diagnostics do not belong in the
 * scenario, and this way every test written later gets the behaviour for free.
 */
public class ScreenshotListener implements ITestListener {

    private static final Logger LOG = LoggerFactory.getLogger(ScreenshotListener.class);

    @Override
    public void onTestFailure(ITestResult result) {
        if (!ConfigManager.get().screenshotOnFailure()) {
            return;
        }
        String testName = result.getMethod().getMethodName();
        LOG.error("Test '{}' failed: {}", testName,
                result.getThrowable() == null ? "no throwable" : result.getThrowable().getMessage());
        ScreenshotUtil.capture(testName);
    }

    @Override
    public void onTestStart(ITestResult result) {
        LOG.info("=== START {} ===", result.getMethod().getMethodName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        LOG.info("=== PASS  {} ===", result.getMethod().getMethodName());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        LOG.warn("=== SKIP  {} ===", result.getMethod().getMethodName());
    }
}
