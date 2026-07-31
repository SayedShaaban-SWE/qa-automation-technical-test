package com.sayed.wikipedia.exceptions;

/**
 * Raised when the framework itself cannot proceed - a missing capability, an unreachable Appium
 * server, an unsupported platform.
 *
 * <p>Distinct from a test failure on purpose: an assertion failure means the app is wrong, this
 * means the harness is wrong, and the two need different people to look at them.
 */
public class FrameworkException extends RuntimeException {

    public FrameworkException(String message) {
        super(message);
    }

    public FrameworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
