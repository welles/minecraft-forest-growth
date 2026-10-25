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
 * button, all other values a text field; numbers are checked while typing. Saving only replaces the values in the file
 * and keeps its comments, the mod picks the changes up like any other edit of the file. The descriptions in the file's
 * header are shown as tooltips.
 */
public class ConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int COLUMN_WIDTH = 150;
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
    private final List<String> lines = new ArrayList<>();
    private final List<Entry> entries = new ArrayList<>();
    private int page;

    public ConfigScreen(Screen parent, Path file, Component title) {
        super(title);
        this.parent = parent;
        this.file = file;
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
                if (props.containsKey(key)) entries.add(new Entry(key, props.getProperty(key)));
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
        addRenderableWidget(new StringWidget(width / 2 - font.width(title) / 2, 12, font.width(title), 9, title, font));

        int rows = Math.max(1, (height - TOP - BOTTOM) / ROW_HEIGHT);
        int pages = Math.max(1, (entries.size() + rows - 1) / rows);
        page = Math.min(page, pages - 1);
        for (int i = 0; i < rows && page * rows + i < entries.size(); i++) {
            Entry entry = entries.get(page * rows + i);
            int y = TOP + i * ROW_HEIGHT;
            Tooltip tooltip = Tooltip.create(Component.literal(entry.key + (entry.description.isEmpty() ? "" : "\n" + entry.description)));

            Component label = Component.literal(entry.label());
            StringWidget labelWidget = addRenderableWidget(new StringWidget(left, y + 6, Math.min(font.width(label), COLUMN_WIDTH), 9, label, font));
            labelWidget.setTooltip(tooltip);

            if (entry.type == Type.BOOLEAN) {
                CycleButton<Boolean> button = CycleButton.onOffBuilder(Boolean.parseBoolean(entry.value)).displayOnlyValue()
                    .create(right, y, COLUMN_WIDTH, 20, label, (cycle, value) -> entry.value = String.valueOf(value));
                button.setTooltip(tooltip);
                addRenderableWidget(button);
            } else {
                EditBox box = new EditBox(font, right, y, COLUMN_WIDTH, 20, label);
                box.setMaxLength(Integer.MAX_VALUE);
                box.setValue(entry.value);
                // long values show their beginning
                box.moveCursorToStart(false);
                box.setTextColor(entry.isValid() ? TEXT_COLOR : INVALID_COLOR);
                box.setResponder(value -> {
                    entry.value = value;
                    box.setTextColor(entry.isValid() ? TEXT_COLOR : INVALID_COLOR);
                });
                box.setTooltip(tooltip);
                addRenderableWidget(box);
            }
        }

        int bottom = height - 28;
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose()).bounds(left, bottom, 100, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> save()).bounds(right + COLUMN_WIDTH - 100, bottom, 100, 20).build());
        if (pages > 1) {
            Button previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> turnPage(-1)).bounds(width / 2 - 45, bottom, 20, 20).build());
            Button next = addRenderableWidget(Button.builder(Component.literal(">"), button -> turnPage(1)).bounds(width / 2 + 25, bottom, 20, 20).build());
            previous.active = page > 0;
            next.active = page < pages - 1;
            Component pageLabel = Component.literal((page + 1) + " / " + pages);
            addRenderableWidget(new StringWidget(width / 2 - font.width(pageLabel) / 2, bottom + 6, font.width(pageLabel), 9, pageLabel, font));
        }
    }

    private void turnPage(int offset) {
        page += offset;
        rebuildWidgets();
    }

    /** Writes the values back into the file, unless a value is invalid. */
    private void save() {
        if (!entries.stream().allMatch(Entry::isValid)) return;
        Map<String, String> values = new HashMap<>();
        for (Entry entry : entries) values.put(entry.key, entry.value);

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
        final StringBuilder description = new StringBuilder();
        String value;

        Entry(String key, String value) {
            this.key = key;
            this.value = value.trim();
            this.type = Type.of(this.value);
        }

        boolean isValid() {
            try {
                return switch (type) {
                    case INTEGER -> {
                        Integer.parseInt(value.trim());
                        yield true;
                    }
                    case NUMBER -> Double.isFinite(Double.parseDouble(value.trim()));
                    default -> true;
                };
            } catch (NumberFormatException e) {
                return false;
            }
        }

        /** "defaultMode" becomes "Default mode". */
        String label() {
            String words = key.replaceAll("([a-z0-9])([A-Z])", "$1 $2").toLowerCase(Locale.ROOT);
            return Character.toUpperCase(words.charAt(0)) + words.substring(1);
        }
    }
}
