/*
 * This file is part of InteractiveHolograms, licensed under the GNU GPL v3.0 License.
 * Copyright (C) DecentSoftware.eu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.siberanka.interactiveholograms.platform.bukkit.text;

import com.siberanka.interactiveholograms.platform.api.text.TextFormat;
import com.siberanka.interactiveholograms.platform.api.text.TextFormatter;
import com.siberanka.interactiveholograms.profiler.DecentProfiler;
import com.siberanka.interactiveholograms.profiler.Metrics;
import com.siberanka.interactiveholograms.profiler.TimerHandle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A caching implementation of the {@link TextFormatter} interface designed for
 * Bukkit's legacy text formatting. This formatter applies color codes and formatting
 * styles, caching the results for improved performance when processing recurring text.
 *
 * <h4>Thread Safety:</h4>
 * Cache operations are synchronized, so recurring text can be formatted safely
 * from Bukkit and region-scheduler threads.
 *
 * <h4>Usage Notes:</h4>
 * - Call {@link #invalidate()} to clear the cache when necessary, such as when
 * text formatting rules have changed or when the plugin is being shut down.
 *
 * @author d0by
 * @see TextFormatter
 * @see TextFormat#LEGACY
 * @since 2.10.0
 */
public class LegacyCachingBukkitTextFormatter implements TextFormatter {

    public static final int DEFAULT_MAX_CACHE_SIZE = 2000;
    private static final int MAX_INPUT_LENGTH = 16_384;
    private static final int MAX_MINIMESSAGE_TAGS = 512;
    private static final char COLOR_CHAR = '\u00a7';
    private static final char SECTION_SENTINEL = '\uE000';
    private static final Pattern ANSI_SEQUENCE = Pattern.compile("\\u001B\\[([0-9;]*)m");
    private static final Pattern HISTORICAL_GRADIENT = Pattern.compile(
            "(?is)<#([a-f0-9]{6})>(.*?)</#([a-f0-9]{6})>");
    private static final Pattern HISTORICAL_RAINBOW = Pattern.compile(
            "(?is)<rainbow[0-9]{1,3}>(.*?)</rainbow>");
    private static final Pattern LEGACY_HEX = Pattern.compile(
            "(?i)(?:[&\\u00a7]?#|\\{#)([a-f0-9]{6})}?");
    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder()
            .tags(TagResolver.resolver(
                    StandardTags.color(),
                    StandardTags.decorations(),
                    StandardTags.gradient(),
                    StandardTags.rainbow(),
                    StandardTags.transition(),
                    StandardTags.reset(),
                    TagResolver.resolver("newline", (argumentQueue, context) -> Tag.inserting(Component.newline())),
                    TagResolver.resolver("br", (argumentQueue, context) -> Tag.inserting(Component.newline()))
            ))
            .build();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character(COLOR_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();
    private final Map<String, String> cache;

    /**
     * Creates a new instance of the {@code LegacyCachingBukkitTextFormatter} with the default maximum cache size.
     *
     * @see #LegacyCachingBukkitTextFormatter(int)
     * @see #DEFAULT_MAX_CACHE_SIZE
     * @since 2.10.0
     */
    public LegacyCachingBukkitTextFormatter() {
        this(DEFAULT_MAX_CACHE_SIZE);
    }

    /**
     * Constructs a new {@code LegacyCachingBukkitTextFormatter} instance with the specified maximum cache size.
     *
     * <p>The formatter uses the least recently used (LRU) eviction policy, implemented via a
     * {@link LinkedHashMap} with access order enabled. Text formatting results are cached
     * to improve performance for frequently recurring input strings.</p>
     *
     * @param maxSize the maximum number of entries that the cache can hold. When the size
     *                exceeds this limit, the least recently accessed entry will be evicted.
     *                Must be a positive integer.
     * @throws IllegalArgumentException if {@code maxSize} is not a positive integer.
     * @since 2.10.0
     */
    public LegacyCachingBukkitTextFormatter(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be a positive integer");
        }
        // Using accessOrder=true to implement LRU eviction policy
        this.cache = new LinkedHashMap<String, String>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > maxSize;
            }
        };
    }

    @Override
    public synchronized @NotNull String format(@NotNull String text) {
        try (TimerHandle ignored = DecentProfiler.getInstance().startTimer(Metrics.POST_PROCESS_TEXT_FORMAT_LINE)) {
            text = bound(text);
            String cached = cache.get(text);
            if (cached != null) {
                return cached;
            }

            String formatted = formatUncached(text);
            cache.put(text, formatted);
            return formatted;
        }
    }

    private String formatUncached(String text) {
        String normalized = translateAnsi(text);
        // Preserve InteractiveHolograms' historical two-color and rainbow syntax
        // before MiniMessage interprets angle-bracket tags.
        normalized = normalizeHistoricalTags(normalized);
        if (count(normalized, '<') <= MAX_MINIMESSAGE_TAGS) {
            try {
                String miniMessageInput = normalized.replace(COLOR_CHAR, SECTION_SENTINEL);
                normalized = LEGACY_SERIALIZER.serialize(MINI_MESSAGE.deserialize(miniMessageInput))
                        .replace(SECTION_SENTINEL, COLOR_CHAR);
            } catch (RuntimeException ignored) {
                // Malformed third-party placeholder output remains visible and is
                // still processed by the legacy formatter below.
            }
        }
        normalized = translateLegacyHex(normalized);
        return ChatColor.translateAlternateColorCodes('&', normalized);
    }

    private String normalizeHistoricalTags(String text) {
        Matcher gradient = HISTORICAL_GRADIENT.matcher(text);
        StringBuffer normalized = new StringBuffer(text.length());
        while (gradient.find()) {
            String replacement = "<gradient:#" + gradient.group(1) + ":#" + gradient.group(3)
                    + ">" + gradient.group(2) + "</gradient>";
            gradient.appendReplacement(normalized, Matcher.quoteReplacement(replacement));
        }
        gradient.appendTail(normalized);

        Matcher rainbow = HISTORICAL_RAINBOW.matcher(normalized.toString());
        StringBuffer result = new StringBuffer(normalized.length());
        while (rainbow.find()) {
            rainbow.appendReplacement(result, Matcher.quoteReplacement("<rainbow>" + rainbow.group(1) + "</rainbow>"));
        }
        rainbow.appendTail(result);
        return result.toString();
    }

    private String translateLegacyHex(String text) {
        Matcher matcher = LEGACY_HEX.matcher(text);
        StringBuffer output = new StringBuffer(text.length());
        while (matcher.find()) {
            StringBuilder replacement = new StringBuilder(14);
            appendHex(replacement, Integer.parseInt(matcher.group(1), 16));
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private String bound(String text) {
        if (text.length() <= MAX_INPUT_LENGTH) {
            return text;
        }
        int end = MAX_INPUT_LENGTH;
        if (Character.isHighSurrogate(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(0, end);
    }

    private int count(String text, char value) {
        int result = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == value) result++;
        }
        return result;
    }

    private String translateAnsi(String text) {
        if (text.indexOf('\u001B') < 0) return text;
        Matcher matcher = ANSI_SEQUENCE.matcher(text);
        StringBuffer output = new StringBuffer(text.length());
        while (matcher.find()) {
            matcher.appendReplacement(output, Matcher.quoteReplacement(ansiCodes(matcher.group(1))));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private String ansiCodes(String sequence) {
        String[] parts = sequence.isEmpty() ? new String[]{"0"} : sequence.split(";");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            int code;
            try {
                code = Integer.parseInt(parts[i]);
            } catch (NumberFormatException ignored) {
                continue;
            }
            if (code == 38 && i + 4 < parts.length && "2".equals(parts[i + 1])) {
                try {
                    int red = clampColor(Integer.parseInt(parts[i + 2]));
                    int green = clampColor(Integer.parseInt(parts[i + 3]));
                    int blue = clampColor(Integer.parseInt(parts[i + 4]));
                    appendHex(result, (red << 16) | (green << 8) | blue);
                    i += 4;
                    continue;
                } catch (NumberFormatException ignored) {
                    // Ignore the malformed color while retaining the visible text.
                }
            }
            if (code == 38 && i + 2 < parts.length && "5".equals(parts[i + 1])) {
                try {
                    appendHex(result, ansi256Color(clampColor(Integer.parseInt(parts[i + 2]))));
                    i += 2;
                    continue;
                } catch (NumberFormatException ignored) {
                    // Ignore the malformed palette color while retaining visible text.
                }
            }
            char legacy = ansiLegacy(code);
            if (legacy != 0) result.append(COLOR_CHAR).append(legacy);
        }
        return result.toString();
    }

    private char ansiLegacy(int code) {
        switch (code) {
            case 0: return 'r';
            case 1: return 'l';
            case 3: return 'o';
            case 4: return 'n';
            case 5: return 'k';
            case 9: return 'm';
            case 22:
            case 23:
            case 24:
            case 25:
            case 29:
            case 39: return 'r';
            case 30: return '0';
            case 31: return '4';
            case 32: return '2';
            case 33: return '6';
            case 34: return '1';
            case 35: return '5';
            case 36: return '3';
            case 37: return '7';
            case 90: return '8';
            case 91: return 'c';
            case 92: return 'a';
            case 93: return 'e';
            case 94: return '9';
            case 95: return 'd';
            case 96: return 'b';
            case 97: return 'f';
            default: return 0;
        }
    }

    private int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private int ansi256Color(int index) {
        int[] basic = {
                0x000000, 0x800000, 0x008000, 0x808000, 0x000080, 0x800080, 0x008080, 0xC0C0C0,
                0x808080, 0xFF0000, 0x00FF00, 0xFFFF00, 0x0000FF, 0xFF00FF, 0x00FFFF, 0xFFFFFF
        };
        if (index < 16) return basic[index];
        if (index < 232) {
            int cube = index - 16;
            int red = ansiCubeComponent(cube / 36);
            int green = ansiCubeComponent((cube / 6) % 6);
            int blue = ansiCubeComponent(cube % 6);
            return (red << 16) | (green << 8) | blue;
        }
        int gray = 8 + (index - 232) * 10;
        return (gray << 16) | (gray << 8) | gray;
    }

    private int ansiCubeComponent(int component) {
        return component == 0 ? 0 : 55 + component * 40;
    }

    private void appendHex(StringBuilder output, int rgb) {
        String hex = String.format("%06X", rgb);
        output.append(COLOR_CHAR).append('x');
        for (int i = 0; i < hex.length(); i++) {
            output.append(COLOR_CHAR).append(hex.charAt(i));
        }
    }

    /**
     * Clears the internal cache of formatted text strings.
     *
     * <p>This operation is synchronized with formatting and is safe to call while
     * scheduler threads are rendering displays.</p>
     *
     * @since 2.10.0
     */
    public synchronized void invalidate() {
        cache.clear();
    }
}
