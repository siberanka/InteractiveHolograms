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

package com.siberanka.interactiveholograms.display.attribute.value.primitives;

import com.siberanka.interactiveholograms.display.attribute.value.AttributeValue;
import com.siberanka.interactiveholograms.display.attribute.value.CompiledAttributeValue;
import com.siberanka.interactiveholograms.display.attribute.value.StaticCompiledAttributeValue;
import com.siberanka.interactiveholograms.display.render.DisplayRenderContext;
import com.siberanka.interactiveholograms.display.render.placeholder.DisplayPlaceholderService;

public final class StringValue implements AttributeValue<String> {

    private final String value;
    private final DisplayPlaceholderService placeholderService;

    public StringValue(String value, DisplayPlaceholderService placeholderService) {
        this.value = value;
        this.placeholderService = placeholderService;
    }

    @Override
    public String getTypeKey() {
        return StringValueType.TYPE_ID;
    }

    @Override
    public CompiledAttributeValue<String> compile(DisplayRenderContext context) {
        if (value == null) {
            return new StaticCompiledAttributeValue<>(null);
        }
        String resolvedValue = placeholderService.replacePlaceholders(value, context);
        return new StaticCompiledAttributeValue<>(resolvedValue);
    }

    @Override
    public String toHumanReadableString() {
        return value;
    }

    public String getValue() {
        return value;
    }
}
