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

package com.siberanka.interactiveholograms.platform.bukkit;

import com.siberanka.interactiveholograms.nms.api.display.NmsDisplayRendererFactory;
import com.siberanka.interactiveholograms.platform.api.PlatformAdapter;
import com.siberanka.interactiveholograms.platform.api.PlatformEventListener;
import com.siberanka.interactiveholograms.platform.api.capability.PlatformCapabilities;
import com.siberanka.interactiveholograms.platform.api.capability.PlatformMaterialService;
import com.siberanka.interactiveholograms.platform.api.placeholder.PlaceholderProvider;
import com.siberanka.interactiveholograms.platform.api.player.PlatformPlayerService;
import com.siberanka.interactiveholograms.platform.api.render.PlatformRenderService;
import com.siberanka.interactiveholograms.platform.api.resource.SaveResourceService;
import com.siberanka.interactiveholograms.platform.bukkit.placeholder.BukkitPlaceholderApiProvider;
import com.siberanka.interactiveholograms.platform.bukkit.player.BukkitPlayerService;
import com.siberanka.interactiveholograms.platform.bukkit.render.BukkitItemFactory;
import com.siberanka.interactiveholograms.platform.bukkit.render.BukkitRenderService;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class BukkitPlatformAdapter implements PlatformAdapter {

    private final BukkitPlatformCapabilities capabilities;
    private final BukkitMaterialService materialService;
    private final BukkitPlayerService playerService;
    private final BukkitRenderService renderService;
    private final BukkitEventListener eventListener;
    private final List<PlaceholderProvider> placeholderProviders;
    private final BukkitSaveResourceService saveResourceService;

    public BukkitPlatformAdapter(JavaPlugin plugin, NmsDisplayRendererFactory rendererFactory) {
        capabilities = new BukkitPlatformCapabilities();
        materialService = new BukkitMaterialService();
        playerService = new BukkitPlayerService();
        renderService = new BukkitRenderService(rendererFactory, new BukkitItemFactory());
        eventListener = new BukkitEventListener(renderService);
        placeholderProviders = Collections.singletonList(
                new BukkitPlaceholderApiProvider()
        );
        saveResourceService = new BukkitSaveResourceService(plugin);
    }

    @NotNull
    @Override
    public PlatformCapabilities getCapabilities() {
        return capabilities;
    }

    @NotNull
    @Override
    public PlatformEventListener getEventListener() {
        return eventListener;
    }

    @NotNull
    @Override
    public PlatformMaterialService getMaterialService() {
        return materialService;
    }

    @NotNull
    @Override
    public PlatformPlayerService getPlayerService() {
        return playerService;
    }

    @NotNull
    @Override
    public PlatformRenderService getRenderService() {
        return renderService;
    }

    @NotNull
    @Override
    public List<PlaceholderProvider> getPlaceholderProviders() {
        return placeholderProviders;
    }

    @NotNull
    @Override
    public SaveResourceService getSaveResourceService() {
        return saveResourceService;
    }
}
