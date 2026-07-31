package com.sayed.wikipedia.config;

import com.sayed.wikipedia.exceptions.FrameworkException;

import java.util.Arrays;
import java.util.Locale;

/**
 * The platforms the framework can drive.
 *
 * <p>Every platform-specific branch in the codebase switches on this enum, so adding a platform is
 * a compile-time exhaustiveness problem rather than a runtime surprise.
 */
public enum MobilePlatform {

    ANDROID,
    IOS;

    public static MobilePlatform from(String value) {
        return Arrays.stream(values())
                .filter(p -> p.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new FrameworkException(
                        "Unsupported platform '" + value + "'. Expected one of "
                                + Arrays.toString(values()).toLowerCase(Locale.ROOT)));
    }

    public boolean isAndroid() {
        return this == ANDROID;
    }

    public boolean isIos() {
        return this == IOS;
    }
}
