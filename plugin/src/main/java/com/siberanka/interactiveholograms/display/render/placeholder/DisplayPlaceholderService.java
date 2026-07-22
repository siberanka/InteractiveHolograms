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

package com.siberanka.interactiveholograms.display.render.placeholder;

import com.siberanka.interactiveholograms.api.utils.Log;
import com.siberanka.interactiveholograms.display.render.DisplayRenderContext;
import com.siberanka.interactiveholograms.platform.api.PlatformAdapter;
import com.siberanka.interactiveholograms.platform.api.placeholder.PlaceholderContext;
import com.siberanka.interactiveholograms.platform.api.placeholder.PlaceholderProvider;

import java.util.List;

public class DisplayPlaceholderService {

    private static final int MAX_PLACEHOLDER_PASSES = 3;
    private static final int MAX_RESULT_LENGTH = 16_384;
    private final PlatformAdapter platformAdapter;

    public DisplayPlaceholderService(PlatformAdapter platformAdapter) {
        this.platformAdapter = platformAdapter;
    }

    public String replacePlaceholders(String content, DisplayRenderContext context) {
        content = replaceInternalPlaceholders(content, context);
        content = replacePlatformPlaceholders(content, context);
        return content;
    }

    public boolean containsPlaceholders(String content) {
        if (content.contains("{player}")) {
            return true;
        }

        List<PlaceholderProvider> placeholderProviders = platformAdapter.getPlaceholderProviders();
        for (PlaceholderProvider placeholderProvider : placeholderProviders) {
            try {
                if (placeholderProvider.containsPlaceholders(content)) return true;
            } catch (RuntimeException e) {
                Log.warn("Failed to inspect placeholders using provider '%s'.",
                        placeholderProvider.getClass().getName(), e);
            }
        }

        return false;
    }

    private String replacePlatformPlaceholders(String content, DisplayRenderContext context) {
        PlaceholderContext placeholderContext = createPlaceholderContext(context);
        List<PlaceholderProvider> placeholderProviders = platformAdapter.getPlaceholderProviders();
        for (int pass = 0; pass < MAX_PLACEHOLDER_PASSES; pass++) {
            String beforePass = content;
            for (PlaceholderProvider placeholderProvider : placeholderProviders) {
                try {
                    if (pass > 0 && !placeholderProvider.containsPlaceholders(content)) continue;
                    String replaced = placeholderProvider.replace(content, placeholderContext);
                    if (replaced != null) content = bound(replaced);
                } catch (Exception e) {
                    Log.warn("Failed to resolve placeholders using provider '%s'.", placeholderProvider.getClass().getName(), e);
                }
            }
            if (content.equals(beforePass)) break;
        }
        return content;
    }

    private String bound(String content) {
        if (content.length() <= MAX_RESULT_LENGTH) return content;
        int end = MAX_RESULT_LENGTH;
        if (Character.isHighSurrogate(content.charAt(end - 1))) end--;
        return content.substring(0, end);
    }

    private PlaceholderContext createPlaceholderContext(DisplayRenderContext displayRenderContext) {
        return new DisplayPlaceholderContext(displayRenderContext.getPlayer());
    }

    private String replaceInternalPlaceholders(String content, DisplayRenderContext context) {
        return content.replace("{player}", context.getPlayer().getName());
    }
}
