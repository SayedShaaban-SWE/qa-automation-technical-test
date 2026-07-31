package com.sayed.zippopotam.tests;

import com.sayed.zippopotam.config.ConfigManager;
import com.sayed.zippopotam.service.ZipCodeService;
import org.junit.jupiter.api.Tag;

/**
 * Shared parent for every API test.
 *
 * <p>Keeps service construction and configuration access in one place so a test class contains
 * nothing but test intent. There is deliberately no shared mutable state here: each test gets its
 * own {@link ZipCodeService} instance, which is what makes the suite safe to run in parallel
 * (see {@code junit-platform.properties}).
 */
@Tag("api")
abstract class BaseApiTest {

    protected static final ConfigManager CONFIG = ConfigManager.get();

    protected final ZipCodeService zipCodeService = new ZipCodeService();
}
