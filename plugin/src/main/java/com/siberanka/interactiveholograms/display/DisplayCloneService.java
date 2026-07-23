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

package com.siberanka.interactiveholograms.display;

import com.siberanka.interactiveholograms.display.attribute.AttributeKey;
import com.siberanka.interactiveholograms.display.attribute.DisplayAttribute;
import com.siberanka.interactiveholograms.platform.api.data.display.DisplayType;

import java.util.Map;
import java.util.Objects;

public class DisplayCloneService {

    public DisplayBase cloneDisplay(DisplayBase display, String newName) {
        Objects.requireNonNull(display, "display cannot be null");
        Objects.requireNonNull(newName, "newName cannot be null");

        DisplayType type = display.getType();
        DisplayBase clone;
        switch (type) {
            case TEXT:
                clone = cloneTextDisplay((TextDisplay) display, newName);
                break;
            case BLOCK:
                clone = cloneBlockDisplay((BlockDisplay) display, newName);
                break;
            case ITEM:
                clone = cloneItemDisplay((ItemDisplay) display, newName);
                break;
            default:
                throw new IllegalArgumentException("Unknown display type: " + type.name());
        }
        Map<AttributeKey<?>, DisplayAttribute<?>> clonedAttributes = cloneAttributes(display.getAttributesMap());
        clone.setAttributes(clonedAttributes);
        clone.setActions(display.getActions());
        clone.setModelProvider(display.getModelProvider());
        clone.setModel(display.getModel());
        clone.setAnimation(display.getAnimation());
        return clone;
    }

    private TextDisplay cloneTextDisplay(TextDisplay display, String newName) {
        DisplaySettings settings = cloneSettings(display);
        TextDisplay clone = new TextDisplay(newName, display.getLocation(), settings);
        clone.setPages(display.getPages());
        return clone;
    }

    private BlockDisplay cloneBlockDisplay(BlockDisplay display, String newName) {
        DisplaySettings settings = cloneSettings(display);
        BlockDisplay clone = new BlockDisplay(newName, display.getLocation(), settings);
        clone.setMaterial(display.getMaterial());
        return clone;
    }

    private ItemDisplay cloneItemDisplay(ItemDisplay display, String newName) {
        DisplaySettings settings = cloneSettings(display);
        ItemDisplay clone = new ItemDisplay(newName, display.getLocation(), settings);
        clone.setMaterial(display.getMaterial());
        return clone;
    }

    private DisplaySettings cloneSettings(DisplayBase source) {
        return source.getSettings().copy();
    }

    private Map<AttributeKey<?>, DisplayAttribute<?>> cloneAttributes(Map<AttributeKey<?>, DisplayAttribute<?>> attributes) {
        return attributes.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().copy()
                ));
    }
}
