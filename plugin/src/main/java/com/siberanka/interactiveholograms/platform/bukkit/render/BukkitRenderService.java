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

package com.siberanka.interactiveholograms.platform.bukkit.render;

import com.siberanka.interactiveholograms.nms.api.display.NmsBlockDisplayRenderer;
import com.siberanka.interactiveholograms.nms.api.display.NmsDisplayRendererFactory;
import com.siberanka.interactiveholograms.nms.api.display.NmsItemDisplayRenderer;
import com.siberanka.interactiveholograms.nms.api.display.NmsTextDisplayRenderer;
import com.siberanka.interactiveholograms.platform.api.player.PlatformPlayer;
import com.siberanka.interactiveholograms.platform.api.render.PlatformRenderService;
import com.siberanka.interactiveholograms.platform.api.render.RenderObjectHandle;
import com.siberanka.interactiveholograms.platform.api.render.intent.RenderIntent;
import com.siberanka.interactiveholograms.platform.bukkit.player.BukkitPlayer;
import com.siberanka.interactiveholograms.platform.bukkit.render.display.BukkitBlockDisplayRenderService;
import com.siberanka.interactiveholograms.platform.bukkit.render.display.BukkitDisplayRenderService;
import com.siberanka.interactiveholograms.platform.bukkit.render.display.BukkitItemDisplayRenderService;
import com.siberanka.interactiveholograms.platform.bukkit.render.display.BukkitTextDisplayRenderService;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BukkitRenderService implements PlatformRenderService {

    private final Map<String, BukkitDisplayRenderService<?>> renderServices = new ConcurrentHashMap<>();
    private final NmsDisplayRendererFactory rendererFactory;
    private final BukkitItemFactory itemFactory;

    public BukkitRenderService(NmsDisplayRendererFactory rendererFactory, BukkitItemFactory itemFactory) {
        this.rendererFactory = rendererFactory;
        this.itemFactory = itemFactory;
    }

    @Override
    public void render(@NotNull PlatformPlayer player, @NotNull RenderObjectHandle handle, @NotNull List<RenderIntent> intents) {
        Player bukkitPlayer = ((BukkitPlayer) player).getBukkitPlayer();
        BukkitDisplayRenderService<?> renderService = getRenderService(handle);
        renderService.apply(bukkitPlayer, intents);
    }

    public void unloadDisplay(String name) {
        renderServices.remove(name);
    }

    private BukkitDisplayRenderService<?> getRenderService(RenderObjectHandle handle) {
        return renderServices.computeIfAbsent(handle.getId(), k -> createRenderService(handle));
    }

    private BukkitDisplayRenderService<?> createRenderService(RenderObjectHandle handle) {
        switch (handle.getDisplayType()) {
            case TEXT:
                NmsTextDisplayRenderer textDisplayRenderer = rendererFactory.createTextDisplayRenderer();
                return new BukkitTextDisplayRenderService(textDisplayRenderer);
            case ITEM:
                NmsItemDisplayRenderer itemDisplayRenderer = rendererFactory.createItemDisplayRenderer();
                return new BukkitItemDisplayRenderService(itemDisplayRenderer, itemFactory);
            case BLOCK:
                NmsBlockDisplayRenderer blockDisplayRenderer = rendererFactory.createBlockDisplayRenderer();
                return new BukkitBlockDisplayRenderService(blockDisplayRenderer);
        }
        throw new IllegalArgumentException("Unknown display type: " + handle.getDisplayType());
    }
}
