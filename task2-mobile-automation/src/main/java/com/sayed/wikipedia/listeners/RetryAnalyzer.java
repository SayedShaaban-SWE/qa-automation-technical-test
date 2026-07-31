package com.sayed.wikipedia.listeners;

import com.sayed.wikipedia.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Retries a failed test a bounded number of times.
 *
 * <p>Retries are a compromise, so the bound is deliberately small and configurable
 * ({@code test.retry.count}, default 1). One retry absorbs the genuinely non-deterministic layer of
 * a mobile run - a stuck animation, an emulator hiccup, a slow first launch. More than that starts
 * hiding real, intermittent product bugs, which is worse than a red build.
 *
 * <p>Every retry is logged loudly so a test that only ever passes on the second attempt is visible
 * rather than silently green.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger LOG = LoggerFactory.getLogger(RetryAnalyzer.class);
    private static final int MAX_RETRIES = ConfigManager.get().retryCount();

    private int attempts = 0;

    @Override
    public boolean retry(ITestResult result) {
        if (attempts < MAX_RETRIES) {
            attempts++;
            LOG.warn("Retrying '{}' (attempt {} of {}) after: {}",
                    result.getMethod().getMethodName(), attempts + 1, MAX_RETRIES + 1,
                    result.getThrowable() == null ? "unknown failure" : result.getThrowable().getMessage());
            return true;
        }
        return false;
    }
}
