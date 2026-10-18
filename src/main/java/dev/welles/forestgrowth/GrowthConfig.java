package dev.welles.forestgrowth;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Settings stored in {@code config/forestgrowth.properties}.
 * The file is re-read automatically whenever it changes on disk.
 */
public final class GrowthConfig {
    public boolean enabled = true;
    public double spreadSpeed = 1.0;
    public int spreadRadius = 8;
    public int minSpacing = 4;
    public int minLight = 9;
    public String excludedPlants = "";
    public String additionalPairs = "";
    public boolean logPlanting = false;

    /** {@link #excludedPlants} parsed into block ids. */
    private Set<String> excluded = Set.of();
    /** {@link #additionalPairs} parsed into leaves id → sapling id. */
    private Map<String, String> pairs = Map.of();

    private static final String HEADER = """
        Forest Growth configuration
        Changes are picked up automatically, no restart required.

        Natural leaves (not placed by players) now and then drop a seed. If it lands on suitable ground
        near the tree, a sapling of the same kind sprouts there and grows like any other sapling.
        Seeds only fall in loaded chunks, and the rate follows the randomTickSpeed gamerule.

        enabled          Master switch. false = no new saplings are planted.
        spreadSpeed      Speed multiplier. 1.0 = roughly one seed per tree every four in-game days
                         (most seeds do not find a suitable spot). 0 = off, 2.0 = twice as fast, 0.5 = half as fast.
        spreadRadius     How far (in blocks, horizontally) from the leaves a seed can land.
        minSpacing       New saplings keep at least this many blocks distance to logs and other saplings.
        minLight         Minimum light level (ignoring the time of day) at the spot. Saplings need 9 to grow.
        excludedPlants   Comma separated block ids of saplings or leaves that should not spread,
                         e.g. minecraft:dark_oak_sapling, minecraft:azalea_leaves
        additionalPairs  Comma separated leaves=sapling pairs for modded trees that do not follow the usual
                         naming (<name>_leaves -> <name>_sapling), e.g. somemod:redwood_foliage=somemod:redwood_seedling
        logPlanting      Write a line to the log for every planted sapling (useful for testing).""";

    /** The effective chance for one leaves random tick to drop a seed, taking the master switch into account. */
    public double seedChance() {
        return enabled ? spreadSpeed * SeedDispersal.BASE_SEED_CHANCE : 0;
    }

    public boolean isExcluded(String blockId) {
        return excluded.contains(blockId);
    }

    /** The sapling id configured for the given leaves id, or {@code null}. */
    public String pairedSapling(String leavesId) {
        return pairs.get(leavesId);
    }

    private static Path file;
    private static FileTime loadedAt;
    private static final long CHECK_INTERVAL_NANOS = 1_000_000_000L;
    private static long checkedAt = System.nanoTime();
    private static GrowthConfig current = new GrowthConfig();

    public static synchronized void init(Path configDir) {
        file = configDir.resolve(ForestGrowth.MOD_ID + ".properties");
        reload();
    }

    /**
     * Returns the current settings, re-reading the file if it was modified.
     * Called on every leaves random tick, so the file is checked at most once per second.
     */
    public static synchronized GrowthConfig get() {
        long now = System.nanoTime();
        if (file != null && now - checkedAt >= CHECK_INTERVAL_NANOS) {
            checkedAt = now;
            try {
                FileTime modified = Files.exists(file) ? Files.getLastModifiedTime(file) : null;
                if (modified == null || !modified.equals(loadedAt)) reload();
            } catch (IOException e) {
                ForestGrowth.LOGGER.warn("Could not check config file {}", file, e);
            }
        }
        return current;
    }

    private static void reload() {
        GrowthConfig config = new GrowthConfig();
        Properties props = new Properties();
        boolean complete = false;

        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                props.load(reader);
                complete = config.read(props);
            } catch (IOException | RuntimeException e) {
                ForestGrowth.LOGGER.error("Could not read config file {}, using defaults", file, e);
                current = config;
                return;
            }
        }

        if (!complete) {
            try {
                Files.createDirectories(file.getParent());
                try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                    config.write(writer);
                }
            } catch (IOException e) {
                ForestGrowth.LOGGER.error("Could not write config file {}", file, e);
            }
        }

        try {
            loadedAt = Files.exists(file) ? Files.getLastModifiedTime(file) : null;
        } catch (IOException e) {
            loadedAt = null;
        }
        current = config;
        SeedDispersal.clearCache();
    }

    /** Applies all values present in {@code props}; returns whether every key was present. */
    private boolean read(Properties props) {
        enabled = bool(props, "enabled", enabled);
        spreadSpeed = decimal(props, "spreadSpeed", spreadSpeed, 0, 1000);
        spreadRadius = integer(props, "spreadRadius", spreadRadius, 1, 32);
        minSpacing = integer(props, "minSpacing", minSpacing, 0, 16);
        minLight = integer(props, "minLight", minLight, 0, 15);
        excludedPlants = props.getProperty("excludedPlants", excludedPlants);
        additionalPairs = props.getProperty("additionalPairs", additionalPairs);
        logPlanting = bool(props, "logPlanting", logPlanting);

        excluded = list(excludedPlants).map(GrowthConfig::id).collect(Collectors.toUnmodifiableSet());
        pairs = list(additionalPairs)
            .map(pair -> pair.split("=", 2))
            .filter(pair -> {
                boolean valid = pair.length == 2 && !pair[0].isBlank() && !pair[1].isBlank();
                if (!valid) ForestGrowth.LOGGER.warn("Invalid entry in additionalPairs: {}", String.join("=", pair));
                return valid;
            })
            .collect(Collectors.toUnmodifiableMap(pair -> id(pair[0]), pair -> id(pair[1]), (a, b) -> b));
        return props.keySet().containsAll(toProperties().keySet());
    }

    private void write(Writer writer) throws IOException {
        for (String line : HEADER.split("\n")) writer.write(("# " + line).stripTrailing() + "\n");
        writer.write("\n");
        for (var entry : toProperties().entrySet()) {
            String value = entry.getValue().replace("\\", "\\\\").replace("\n", "\\n");
            writer.write(entry.getKey() + "=" + value + "\n");
        }
    }

    private Map<String, String> toProperties() {
        Map<String, String> props = new LinkedHashMap<>();
        props.put("enabled", String.valueOf(enabled));
        props.put("spreadSpeed", String.valueOf(spreadSpeed));
        props.put("spreadRadius", String.valueOf(spreadRadius));
        props.put("minSpacing", String.valueOf(minSpacing));
        props.put("minLight", String.valueOf(minLight));
        props.put("excludedPlants", excludedPlants);
        props.put("additionalPairs", additionalPairs);
        props.put("logPlanting", String.valueOf(logPlanting));
        return props;
    }

    private static java.util.stream.Stream<String> list(String value) {
        return Arrays.stream(value.split(",")).map(String::trim).filter(entry -> !entry.isEmpty());
    }

    /** Normalizes a block id, adding the {@code minecraft} namespace if it is missing. */
    private static String id(String value) {
        String id = value.trim().toLowerCase(Locale.ROOT);
        return id.contains(":") ? id : "minecraft:" + id;
    }

    private static boolean bool(Properties props, String key, boolean fallback) {
        String value = props.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value.trim());
    }

    private static int integer(Properties props, String key, int fallback, int min, int max) {
        String value = props.getProperty(key);
        if (value == null) return fallback;
        try {
            return Math.clamp(Integer.parseInt(value.trim()), min, max);
        } catch (NumberFormatException e) {
            ForestGrowth.LOGGER.warn("Invalid number for {}: {}", key, value);
            return fallback;
        }
    }

    private static double decimal(Properties props, String key, double fallback, double min, double max) {
        String value = props.getProperty(key);
        if (value == null) return fallback;
        try {
            double parsed = Double.parseDouble(value.trim());
            if (Double.isNaN(parsed)) throw new NumberFormatException();
            return Math.clamp(parsed, min, max);
        } catch (NumberFormatException e) {
            ForestGrowth.LOGGER.warn("Invalid number for {}: {}", key, value);
            return fallback;
        }
    }
}
