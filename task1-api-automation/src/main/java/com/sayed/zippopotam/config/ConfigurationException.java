package com.sayed.zippopotam.config;

/**
 * Thrown when the framework cannot be configured. Unchecked on purpose: a misconfigured suite is a
 * programmer/environment error, not something an individual test should be handling.
 */
public class ConfigurationException extends RuntimeException {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
