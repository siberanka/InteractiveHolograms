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

package com.siberanka.interactiveholograms.display.type;

import com.siberanka.interactiveholograms.display.DisplayBase;
import com.siberanka.interactiveholograms.display.ItemDisplay;
import com.siberanka.interactiveholograms.display.render.DisplayRenderContext;
import com.siberanka.interactiveholograms.platform.api.data.ItemDescriptor;
import com.siberanka.interactiveholograms.display.render.content.CompiledDisplayContent;
import com.siberanka.interactiveholograms.display.render.content.CompiledItemDisplayContent;
import com.siberanka.interactiveholograms.platform.api.data.display.DisplayType;

public class ItemDisplayTypeDefinition implements DisplayTypeDefinition<ItemDescriptor> {

    @Override
    public DisplayType getType() {
        return DisplayType.ITEM;
    }

    @Override
    public CompiledDisplayContent<ItemDescriptor> resolveContent(DisplayBase display, DisplayRenderContext context) {
        ItemDisplay itemDisplay = getItemDisplay(display);

        ItemDescriptor itemDescriptor = new ItemDescriptor(itemDisplay.getMaterial());
        return new CompiledItemDisplayContent(itemDescriptor);
    }

    private ItemDisplay getItemDisplay(DisplayBase displayBase) {
        if (!(displayBase instanceof ItemDisplay)) {
            throw new IllegalArgumentException("Display is not a text display");
        }
        return (ItemDisplay) displayBase;
    }
}
