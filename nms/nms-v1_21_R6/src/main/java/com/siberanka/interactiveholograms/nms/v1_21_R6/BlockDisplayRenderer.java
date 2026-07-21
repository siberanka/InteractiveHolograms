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

package com.siberanka.interactiveholograms.nms.v1_21_R6;

import com.siberanka.interactiveholograms.nms.api.display.NmsBlockDisplayRenderer;
import com.siberanka.interactiveholograms.nms.api.display.NmsSpawnDisplayData;
import com.siberanka.interactiveholograms.nms.api.display.NmsUpdateDisplayContentData;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

class BlockDisplayRenderer extends AbstractDisplayRenderer<Material> implements NmsBlockDisplayRenderer {

    BlockDisplayRenderer(int entityId) {
        super(entityId);
    }

    @Override
    public void spawn(Player player, NmsSpawnDisplayData<Material> data) {
        EntityMetadataBuilder metadataBuilder = EntityMetadataBuilder.create();
        applyMetadata(data.getMetadata(), metadataBuilder);
        metadataBuilder.withBlockDisplayBlockData(data.getContent());

        EntityPacketsBuilder.create()
                .withSpawnEntity(entityId, EntityType.BLOCK_DISPLAY, data.getPosition())
                .withEntityMetadata(entityId, metadataBuilder.toWatchableObjects())
                .sendTo(player);
    }

    @Override
    public void updateContent(Player player, NmsUpdateDisplayContentData<Material> data) {
        EntityMetadataBuilder metadataBuilder = EntityMetadataBuilder.create()
                .withBlockDisplayBlockData(data.getContent());

        EntityPacketsBuilder.create()
                .withEntityMetadata(entityId, metadataBuilder.toWatchableObjects())
                .sendTo(player);
    }
}
