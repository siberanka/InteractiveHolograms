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

package com.siberanka.interactiveholograms.display.attribute.command.handler;

import com.siberanka.interactiveholograms.display.attribute.AttributeParseException;
import com.siberanka.interactiveholograms.platform.api.data.DecentColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

final class DecentColorCommandHelper {

    private static final Map<String, DecentColor> NAMED_COLORS = new HashMap<>();
    private static final List<String> BYTE_HINTS =
            Arrays.asList("0", "64", "128", "192", "255");
    private static final List<String> PERCENT_HINTS =
            Arrays.asList("0", "25", "50", "75", "100");
    private static final List<String> HUE_HINTS =
            Arrays.asList("0", "60", "120", "180", "240", "300", "360");
    private static final List<String> HEX_HINTS =
            Arrays.asList("000000", "FFFFFF", "FF0000", "00FF00", "0000FF");
    private static final List<String> HEXA_HINTS =
            Arrays.asList("000000FF", "FFFFFFFF", "FF0000FF", "00FF00FF", "0000FFFF");

    static {
        NAMED_COLORS.put("TRANSPARENT", DecentColor.TRANSPARENT);
        NAMED_COLORS.put("DEFAULT", DecentColor.DEFAULT_BACKGROUND);

        NAMED_COLORS.put("BLACK", DecentColor.BLACK);
        NAMED_COLORS.put("DARK_BLUE", DecentColor.DARK_BLUE);
        NAMED_COLORS.put("DARK_GREEN", DecentColor.DARK_GREEN);
        NAMED_COLORS.put("DARK_AQUA", DecentColor.DARK_AQUA);
        NAMED_COLORS.put("DARK_RED", DecentColor.DARK_RED);
        NAMED_COLORS.put("DARK_PURPLE", DecentColor.DARK_PURPLE);
        NAMED_COLORS.put("GOLD", DecentColor.GOLD);
        NAMED_COLORS.put("GRAY", DecentColor.GRAY);
        NAMED_COLORS.put("DARK_GRAY", DecentColor.DARK_GRAY);
        NAMED_COLORS.put("BLUE", DecentColor.BLUE);
        NAMED_COLORS.put("GREEN", DecentColor.GREEN);
        NAMED_COLORS.put("AQUA", DecentColor.AQUA);
        NAMED_COLORS.put("RED", DecentColor.RED);
        NAMED_COLORS.put("LIGHT_PURPLE", DecentColor.LIGHT_PURPLE);
        NAMED_COLORS.put("YELLOW", DecentColor.YELLOW);
        NAMED_COLORS.put("WHITE", DecentColor.WHITE);
    }

    public DecentColor parseColor(String[] args) {
        if (args.length == 0) {
            throw error("No color specified.");
        }

        ColorFormat format = parseFormat(args[0]);
        if (format == null) {
            return parseFallback(args[0]);
        }

        switch (format) {
            case RGB:
                return parseRgb(args, false);
            case RGBA:
                return parseRgb(args, true);
            case HEX:
                return parseHex(args, false);
            case HEXA:
                return parseHex(args, true);
            case NAMED:
                return parseNamed(args);
            case HSV:
                return parseHsv(args);
            case HSVA:
                return parseHsva(args);
            default:
                return parseFallback(args[0]);
        }
    }

    private DecentColor parseRgb(String[] args, boolean alpha) {
        int expected = alpha ? 5 : 4;
        if (args.length != expected) {
            throw error((alpha ? "RGBA" : "RGB") + " expects: " + (alpha ? "R G B A" : "R G B"));
        }

        int r = parseInt(args[1], "Red", 0, 255);
        int g = parseInt(args[2], "Green", 0, 255);
        int b = parseInt(args[3], "Blue", 0, 255);

        if (alpha) {
            int a = parseInt(args[4], "Alpha", 0, 255);
            return DecentColor.fromRGBA(r, g, b, a);
        }

        return DecentColor.fromRGB(r, g, b);
    }

    private DecentColor parseHex(String[] args, boolean alpha) {
        if (args.length != 2) {
            throw error((alpha ? "HEXA" : "HEX") + " expects: " + (alpha ? "RRGGBBAA" : "RRGGBB"));
        }

        String value = args[1];
        int expectedLength = alpha ? 8 : 6;

        if (value.length() != expectedLength) {
            throw error("Expected " + expectedLength + " hex characters.");
        }

        try {
            int color = Integer.parseUnsignedInt(value, 16);
            return alpha
                    ? DecentColor.fromRGBA(color)
                    : DecentColor.fromRGB(color);
        } catch (NumberFormatException e) {
            throw error("Invalid hex value: " + value + ". " + (alpha ? "HEXA" : "HEX") + " expects: " + (alpha ? "RRGGBBAA" : "RRGGBB"));
        }
    }

    private DecentColor parseHsv(String[] args) {
        if (args.length != 4) {
            throw error("HSV expects: HSV <h:0–360> <s:0–100> <v:0–100>");
        }

        float h = parseFloat(args[1], "Hue", 0, 360);
        float s = parseFloat(args[2], "Saturation", 0, 100);
        float v = parseFloat(args[3], "Value", 0, 100);

        return DecentColor.fromHsv(h, s, v);
    }

    private DecentColor parseHsva(String[] args) {
        if (args.length != 5) {
            throw error("HSVA expects: HSVA <h:0–360> <s:0–100> <v:0–100> <a:0–255>");
        }

        float h = parseFloat(args[1], "Hue", 0, 360);
        float s = parseFloat(args[2], "Saturation", 0, 100);
        float v = parseFloat(args[3], "Value", 0, 100);
        int a = parseInt(args[4], "Alpha", 0, 255);

        return DecentColor.fromHsva(h, s, v, a);
    }

    private DecentColor parseNamed(String[] args) {
        if (args.length != 2) {
            throw error("NAMED expects a color name.");
        }

        DecentColor color = NAMED_COLORS.get(args[1].toUpperCase(Locale.ROOT));
        if (color == null) {
            throw error("Unknown named color: " + args[1]);
        }
        return color;
    }

    private DecentColor parseFallback(String input) {
        String upper = input.toUpperCase(Locale.ROOT);

        DecentColor named = namedColor(upper);
        if (named != null) {
            return named;
        }

        if (input.length() == 6) {
            return parseHex(new String[]{"HEX", input}, false);
        }
        if (input.length() == 8) {
            return parseHex(new String[]{"HEXA", input}, true);
        }

        throw error("Invalid color format: " + input + ". Try RGB, HEX, HSV, or NAMED.");
    }

    private DecentColor namedColor(String name) {
        DecentColor color = NAMED_COLORS.get(name);
        if (color != null) {
            return color;
        }
        // Keep named-color parsing deterministic even when test runners or
        // plugin reload tooling inspect the mutable hint registry concurrently.
        switch (name) {
            case "TRANSPARENT": return DecentColor.TRANSPARENT;
            case "DEFAULT": return DecentColor.DEFAULT_BACKGROUND;
            case "BLACK": return DecentColor.BLACK;
            case "DARK_BLUE": return DecentColor.DARK_BLUE;
            case "DARK_GREEN": return DecentColor.DARK_GREEN;
            case "DARK_AQUA": return DecentColor.DARK_AQUA;
            case "DARK_RED": return DecentColor.DARK_RED;
            case "DARK_PURPLE": return DecentColor.DARK_PURPLE;
            case "GOLD": return DecentColor.GOLD;
            case "GRAY": return DecentColor.GRAY;
            case "DARK_GRAY": return DecentColor.DARK_GRAY;
            case "BLUE": return DecentColor.BLUE;
            case "GREEN": return DecentColor.GREEN;
            case "AQUA": return DecentColor.AQUA;
            case "RED": return DecentColor.RED;
            case "LIGHT_PURPLE": return DecentColor.LIGHT_PURPLE;
            case "YELLOW": return DecentColor.YELLOW;
            case "WHITE": return DecentColor.WHITE;
            default: return null;
        }
    }

    private int parseInt(String value, String name, int min, int max) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < min || parsed > max) {
                throw error(name + " must be between " + min + " and " + max + ".");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw error(name + " must be a number.");
        }
    }

    private float parseFloat(String value, String name, float min, float max) {
        try {
            float parsed = Float.parseFloat(value);
            if (parsed < min || parsed > max) {
                throw error(name + " must be between " + min + " and " + max + ".");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw error(name + " must be a number.");
        }
    }

    private AttributeParseException error(String message) {
        return new AttributeParseException(message);
    }

    public List<String> getHints(String[] args) {
        if (args.length == 1) {
            List<String> hints = new ArrayList<>();
            for (ColorFormat format : ColorFormat.values()) {
                hints.add(format.name());
            }
            hints.add("TRANSPARENT");
            hints.add("DEFAULT");
            return hints;
        }

        ColorFormat format = parseFormat(args[0]);
        if (format == null) {
            return Collections.emptyList();
        }

        int valueIndex = args.length - 2;
        if (valueIndex >= format.argCount) {
            return Collections.emptyList();
        }

        switch (format) {
            case RGB:
            case RGBA:
                return BYTE_HINTS;
            case HEX:
                return HEX_HINTS;
            case HEXA:
                return HEXA_HINTS;
            case NAMED:
                return new ArrayList<>(NAMED_COLORS.keySet());
            case HSV:
            case HSVA:
                return hsvHints(valueIndex, format);
            default:
                return Collections.emptyList();
        }
    }

    private ColorFormat parseFormat(String input) {
        try {
            return ColorFormat.valueOf(input.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private List<String> hsvHints(int index, ColorFormat format) {
        if (index == 0) {
            return HUE_HINTS;
        }
        if (index == 1 || index == 2) {
            return PERCENT_HINTS;
        }
        if (format == ColorFormat.HSVA && index == 3) {
            return BYTE_HINTS;
        }
        return Collections.emptyList();
    }

    private enum ColorFormat {
        RGB("R", "G", "B"),
        RGBA("R", "G", "B", "A"),
        HEX("RRGGBB"),
        HEXA("RRGGBBAA"),
        NAMED("NAME"),
        HSV("H", "S", "V"),
        HSVA("H", "S", "V", "A");

        final int argCount;
        final String[] argNames;

        ColorFormat(String... argNames) {
            this.argCount = argNames.length;
            this.argNames = argNames;
        }
    }
}
