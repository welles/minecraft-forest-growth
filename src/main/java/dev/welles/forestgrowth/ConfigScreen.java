package dev.welles.forestgrowth;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Edits the mod's properties file, opened with the "Config" button of NeoForge's mod list. Booleans get an on/off
 * button, all other values a text field; numbers are checked against their range and choices against the values they
 * allow while typing, and the config can only be saved once every value is valid. Every value can be reset to its default, one at a time or all at once. Saving
 * only replaces the values in the file and keeps its comments, the mod picks the changes up like any other edit of the
 * file. The descriptions in the file's header are shown as tooltips.
 */
public class ConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int COLUMN_WIDTH = 150;
    private static final int RESET_WIDTH = 20;
    private static final int TOP = 32;
    private static final int BOTTOM = 36;
    private static final int TEXT_COLOR = 0xFFE0E0E0;
    private static final int INVALID_COLOR = 0xFFFF5555;
    /** A header line describing a key: the key, at least two spaces, the description. */
    private static final Pattern DESCRIPTION = Pattern.compile("^#\\s*(\\S+)\\s{2,}(.*)$");
    /** A header line continuing the description above it, indented further than the key. */
    private static final Pattern CONTINUATION = Pattern.compile("^#\\s{2,}(\\S.*)$");

    private final Screen parent;
    private final Path file;
    private final Map<String, String> defaults;
    private final Map<String, GrowthConfig.Range> ranges;
    private final Map<String, List<String>> choices;
    private final List<String> lines = new ArrayList<>();
    private final List<Entry> entries = new ArrayList<>();
    private Button doneButton;
    private int page;

    /**
     * @param defaults the default value of each key, keys without one cannot be reset
     * @param ranges   the allowed values of number keys, keys without one accept any number
     * @param choices  the allowed values of text keys, compared ignoring case; keys without them accept any text
     */
    public ConfigScreen(Screen parent, Path file, Component title, Map<String, String> defaults, Map<String, GrowthConfig.Range> ranges,
                        Map<String, List<String>> choices) {
        super(title);
        this.parent = parent;
        this.file = file;
        this.defaults = defaults;
        this.ranges = ranges;
        this.choices = choices;
        load();
    }

    private void load() {
        try {
            lines.addAll(Files.readAllLines(file, StandardCharsets.UTF_8));
            Properties props = new Properties();
            props.load(new StringReader(String.join("\n", lines)));

            for (String line : lines) {
                int separator = line.indexOf('=');
                if (line.startsWith("#") || separator < 0) continue;
                String key = line.substring(0, separator).trim();
                if (props.containsKey(key)) entries.add(new Entry(key, props.getProperty(key), defaults.get(key), ranges.get(key), choices.get(key)));
            }

            Map<String, Entry> byKey = new HashMap<>();
            for (Entry entry : entries) byKey.put(entry.key, entry);
            Entry described = null;
            for (String line : lines) {
                Matcher description = DESCRIPTION.matcher(line);
                Matcher continuation = CONTINUATION.matcher(line);
                if (description.matches() && byKey.containsKey(description.group(1))) {
                    described = byKey.get(description.group(1));
                    described.description.append(description.group(2).trim());
                } else if (described != null && continuation.matches()) {
                    described.description.append('\n').append(continuation.group(1).trim());
                } else {
                    described = null;
                }
            }
        } catch (IOException | RuntimeException e) {
            ForestGrowth.LOGGER.error("Could not read config file {}", file, e);
        }
    }

    @Override
    protected void init() {
        int left = width / 2 - COLUMN_WIDTH - 5;
        int right = width / 2 + 5;
        int valueWidth = COLUMN_WIDTH - RESET_WIDTH - 4;
        addRenderableWidget(new StringWidget(width / 2 - font.width(title) / 2, 12, font.width(title), 9, title, font));
        Button resetAll = addRenderableWidget(Button.builder(Component.literal("Reset all"), button -> resetAll())
            .bounds(right + COLUMN_WIDTH - 80, 6, 80, 20).build());
        resetAll.setTooltip(Tooltip.create(Component.literal("Resets every value to its default. Nothing is saved until Done.")));
        resetAll.active = entries.stream().anyMatch(Entry::canReset);

        int rows = Math.max(1, (height - TOP - BOTTOM) / ROW_HEIGHT);
        int pages = Math.max(1, (entries.size() + rows - 1) / rows);
        page = Math.min(page, pages - 1);
        for (int i = 0; i < rows && page * rows + i < entries.size(); i++) {
            Entry entry = entries.get(page * rows + i);
            int y = TOP + i * ROW_HEIGHT;
            Tooltip tooltip = Tooltip.create(Component.literal(entry.tooltip()));

            Component label = Component.literal(entry.label());
            StringWidget labelWidget = addRenderableWidget(new StringWidget(left, y + 6, Math.min(font.width(label), COLUMN_WIDTH), 9, label, font));
            labelWidget.setTooltip(tooltip);

            Button reset = Button.builder(Component.literal("↺"), button -> {
                entry.value = entry.defaultValue;
                rebuildWidgets();
            }).bounds(right + COLUMN_WIDTH - RESET_WIDTH, y, RESET_WIDTH, 20).build();
            reset.setTooltip(Tooltip.create(Component.literal(entry.defaultValue == null ? "No default" : "Reset to " + entry.defaultValue)));
            Runnable changed = () -> {
                reset.active = entry.canReset();
                resetAll.active = entries.stream().anyMatch(Entry::canReset);
                updateDone();
            };

            if (entry.type == Type.BOOLEAN) {
                CycleButton<Boolean> button = CycleButton.onOffBuilder(Boolean.parseBoolean(entry.value)).displayOnlyValue()
                    .create(right, y, valueWidth, 20, label, (cycle, value) -> {
                        entry.value = String.valueOf(value);
                        changed.run();
                    });
                button.setTooltip(tooltip);
                addRenderableWidget(button);
            } else {
                EditBox box = new EditBox(font, right, y, valueWidth, 20, label);
                box.setMaxLength(Integer.MAX_VALUE);
                box.setValue(entry.value);
                // long values show their beginning
                box.moveCursorToStart(false);
                box.setTextColor(entry.isValid() ? TEXT_COLOR : INVALID_COLOR);
                box.setResponder(value -> {
                    entry.value = value;
                    box.setTextColor(entry.isValid() ? TEXT_COLOR : INVALID_COLOR);
                    changed.run();
                });
                box.setTooltip(tooltip);
                addRenderableWidget(box);
            }
            addRenderableWidget(reset);
            reset.active = entry.canReset();
        }

        int bottom = height - 28;
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose()).bounds(left, bottom, 100, 20).build());
        doneButton = addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> save()).bounds(right + COLUMN_WIDTH - 100, bottom, 100, 20).build());
        updateDone();
        if (pages > 1) {
            Button previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> turnPage(-1)).bounds(width / 2 - 45, bottom, 20, 20).build());
            Button next = addRenderableWidget(Button.builder(Component.literal(">"), button -> turnPage(1)).bounds(width / 2 + 25, bottom, 20, 20).build());
            previous.active = page > 0;
            next.active = page < pages - 1;
            Component pageLabel = Component.literal((page + 1) + " / " + pages);
            addRenderableWidget(new StringWidget(width / 2 - font.width(pageLabel) / 2, bottom + 6, font.width(pageLabel), 9, pageLabel, font));
        }
    }

    /** Done only saves valid values, the tooltip names the invalid ones, also those on other pages. */
    private void updateDone() {
        List<String> invalid = entries.stream().filter(entry -> !entry.isValid()).map(Entry::describeValid).toList();
        doneButton.active = invalid.isEmpty();
        doneButton.setTooltip(invalid.isEmpty() ? null : Tooltip.create(Component.literal("Invalid values:\n" + String.join("\n", invalid))));
    }

    private void resetAll() {
        for (Entry entry : entries) {
            if (entry.defaultValue != null) entry.value = entry.defaultValue;
        }
        rebuildWidgets();
    }

    private void turnPage(int offset) {
        page += offset;
        rebuildWidgets();
    }

    /** Writes the values back into the file, unless a value is invalid. */
    private void save() {
        if (!entries.stream().allMatch(Entry::isValid)) return;
        Map<String, String> values = new HashMap<>();
        for (Entry entry : entries) values.put(entry.key, entry.value.trim());

        List<String> updated = new ArrayList<>();
        for (String line : lines) {
            int separator = line.indexOf('=');
            String key = line.startsWith("#") || separator < 0 ? null : line.substring(0, separator).trim();
            updated.add(key != null && values.containsKey(key) ? key + "=" + values.get(key) : line);
        }
        try {
            Files.write(file, updated, StandardCharsets.UTF_8);
        } catch (IOException e) {
            ForestGrowth.LOGGER.error("Could not write config file {}", file, e);
        }
        onClose();
    }

    @Override
    public void onClose() {
        //? if >=26.2 {
        /*minecraft.gui.setScreen(parent);
        *///?} else {
        minecraft.setScreen(parent);
        //?}
    }

    /** {@code 30.0} becomes {@code 30}, {@code 0.5} stays. */
    private static String format(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    private enum Type {
        BOOLEAN, INTEGER, NUMBER, TEXT;

        static Type of(String value) {
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) return BOOLEAN;
            if (value.matches("-?\\d+")) return INTEGER;
            try {
                Double.parseDouble(value);
                return NUMBER;
            } catch (NumberFormatException e) {
                return TEXT;
            }
        }
    }

    private static final class Entry {
        final String key;
        final Type type;
        final String defaultValue;
        final GrowthConfig.Range range;
        final List<String> choices;
        final StringBuilder description = new StringBuilder();
        String value;

        Entry(String key, String value, String defaultValue, GrowthConfig.Range range, List<String> choices) {
            this.key = key;
            this.value = value.trim();
            this.defaultValue = defaultValue;
            this.range = range;
            this.choices = choices;
            // the default knows the type even if the file holds something invalid
            this.type = Type.of(defaultValue != null ? defaultValue : this.value);
        }

        boolean isValid() {
            try {
                return switch (type) {
                    case BOOLEAN -> value.trim().equalsIgnoreCase("true") || value.trim().equalsIgnoreCase("false");
                    case INTEGER -> inRange(Integer.parseInt(value.trim()));
                    case NUMBER -> {
                        double number = Double.parseDouble(value.trim());
                        yield Double.isFinite(number) && inRange(number);
                    }
                    case TEXT -> choices == null || choices.stream().anyMatch(choice -> choice.equalsIgnoreCase(value.trim()));
                };
            } catch (NumberFormatException e) {
                return false;
            }
        }

        private boolean inRange(double number) {
            return range == null || range.contains(number);
        }

        /** Whether there is a default that differs from the value, numbers compare by value: 10 is 10.0. */
        boolean canReset() {
            if (defaultValue == null) return false;
            try {
                return switch (type) {
                    case INTEGER, NUMBER -> Double.parseDouble(value.trim()) != Double.parseDouble(defaultValue);
                    case BOOLEAN -> !value.trim().equalsIgnoreCase(defaultValue);
                    case TEXT -> choices == null ? !value.equals(defaultValue) : !value.trim().equalsIgnoreCase(defaultValue);
                };
            } catch (NumberFormatException e) {
                return true;
            }
        }

        String tooltip() {
            StringBuilder tooltip = new StringBuilder(key);
            if (!description.isEmpty()) tooltip.append('\n').append(description);
            if (defaultValue != null) tooltip.append("\n\nDefault: ").append(defaultValue);
            return tooltip.toString();
        }

        /** The label with what it accepts: "Launch height: 1 to 64". */
        String describeValid() {
            return switch (type) {
                case BOOLEAN -> label() + ": true or false";
                case INTEGER, NUMBER -> label() + (range == null ? ": a number" : ": " + format(range.min()) + " to " + format(range.max()));
                case TEXT -> choices == null ? label() : label() + ": " + String.join(", ", choices);
            };
        }

        /** "defaultMode" becomes "Default mode". */
        String label() {
            String words = key.replaceAll("([a-z0-9])([A-Z])", "$1 $2").toLowerCase(Locale.ROOT);
            return Character.toUpperCase(words.charAt(0)) + words.substring(1);
        }
    }
}
