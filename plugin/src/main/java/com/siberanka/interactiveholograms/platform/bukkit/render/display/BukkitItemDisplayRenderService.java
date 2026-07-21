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

package com.siberanka.interactiveholograms.platform.bukkit.render.display;

import com.siberanka.interactiveholograms.nms.api.display.NmsDisplayMetadata;
import com.siberanka.interactiveholograms.nms.api.display.NmsItemDisplayRenderer;
import com.siberanka.interactiveholograms.nms.api.display.NmsSpawnDisplayData;
import com.siberanka.interactiveholograms.nms.api.display.NmsUpdateDisplayContentData;
import com.siberanka.interactiveholograms.platform.api.data.ItemDescriptor;
import com.siberanka.interactiveholograms.platform.api.data.display.DisplayContent;
import com.siberanka.interactiveholograms.platform.api.data.display.ItemDisplayContent;
import com.siberanka.interactiveholograms.platform.api.render.intent.SpawnDisplayRenderIntent;
import com.siberanka.interactiveholograms.platform.api.render.intent.UpdateDisplayContentRenderIntent;
import com.siberanka.interactiveholograms.platform.bukkit.render.BukkitItemFactory;
import com.siberanka.interactiveholograms.shared.DecentPosition;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class BukkitItemDisplayRenderService extends BukkitDisplayRenderService<ItemStack> {

    private final BukkitItemFactory itemFactory;

    public BukkitItemDisplayRenderService(NmsItemDisplayRenderer renderer, BukkitItemFactory itemFactory) {
        super(renderer);
        this.itemFactory = itemFactory;
    }

    @Override
    protected NmsSpawnDisplayData<ItemStack> createNmsSpawnDisplayData(SpawnDisplayRenderIntent intent,
                                                                       DecentPosition position,
                                                                       List<NmsDisplayMetadata<?>> metadata) {
        ItemStack itemStack = getItemStackFromContent(intent.getContent());
        return new NmsSpawnDisplayData<>(position, metadata, itemStack);
    }

    @Override
    protected NmsUpdateDisplayContentData<ItemStack> createNmsUpdateDisplayContentData(UpdateDisplayContentRenderIntent intent) {
        ItemStack itemStack = getItemStackFromContent(intent.getContent());
        return new NmsUpdateDisplayContentData<>(itemStack);
    }

    private ItemStack getItemStackFromContent(DisplayContent<?> content) {
        if (!(content instanceof ItemDisplayContent)) {
            throw new IllegalArgumentException("Unsupported content type for Item display: " + content.getClass().getName());
        }
        ItemDescriptor itemDescriptor = ((ItemDisplayContent) content).getContent();
        return itemFactory.createItemStack(itemDescriptor);
    }
}
