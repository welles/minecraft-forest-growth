package dev.welles.forestgrowth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public final class ForestGrowth {
    public static final String MOD_ID = "forestgrowth";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private ForestGrowth() {}

    public static void init(Path configDir) {
        GrowthConfig.init(configDir);
        LOGGER.info("Forest Growth loaded");
    }
}
