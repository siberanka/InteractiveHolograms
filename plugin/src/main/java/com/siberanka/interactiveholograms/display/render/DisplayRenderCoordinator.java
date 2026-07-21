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

package com.siberanka.interactiveholograms.display.render;

import com.siberanka.interactiveholograms.api.utils.Log;
import com.siberanka.interactiveholograms.display.DisplayBase;
import com.siberanka.interactiveholograms.display.render.state.LogicalRenderState;
import com.siberanka.interactiveholograms.display.render.state.LogicalRenderStateService;
import com.siberanka.interactiveholograms.display.render.state.LogicalRenderStateManager;
import com.siberanka.interactiveholograms.platform.api.player.PlatformPlayer;
import com.siberanka.interactiveholograms.platform.api.player.PlatformPlayerService;
import com.siberanka.interactiveholograms.platform.api.render.RenderObjectHandle;

public class DisplayRenderCoordinator {

    private final DisplayVisibilityService visibilityService;
    private final PlatformPlayerService playerService;
    private final LogicalRenderStateService logicalRenderStateService;
    private final DisplayRenderService renderService;
    private final LogicalRenderStateManager logicalRenderStateManager;

    public DisplayRenderCoordinator(DisplayVisibilityService visibilityService,
                                    PlatformPlayerService playerService,
                                    LogicalRenderStateService logicalRenderStateService,
                                    DisplayRenderService renderService,
                                    LogicalRenderStateManager logicalRenderStateManager) {
        this.visibilityService = visibilityService;
        this.playerService = playerService;
        this.logicalRenderStateService = logicalRenderStateService;
        this.renderService = renderService;
        this.logicalRenderStateManager = logicalRenderStateManager;
    }

    public void hideDisplayForPlayer(DisplayBase display, PlatformPlayer player) {
        if (isIsShownToPlayer(display, player)) {
            updateLogicalState(display, player, false);
        }
    }

    public void hideForEveryone(DisplayBase display) {
        for (PlatformPlayer player : playerService.getOnlinePlayers()) {
            updateLogicalState(display, player, false);
        }
    }

    public void updateVisibility(DisplayBase display) {
        for (PlatformPlayer player : playerService.getOnlinePlayers()) {
            updateVisibility(display, player);
        }
    }

    public void updateVisibility(DisplayBase display, PlatformPlayer player) {
        boolean shouldBeShownToPlayer = visibilityService.shouldBeShownToPlayer(display, player);
        boolean isShownToPlayer = isIsShownToPlayer(display, player);
        if (shouldBeShownToPlayer && !isShownToPlayer) {
            updateLogicalState(display, player, true);
        } else if (!shouldBeShownToPlayer && isShownToPlayer) {
            updateLogicalState(display, player, false);
        }
    }

    private boolean isIsShownToPlayer(DisplayBase display, PlatformPlayer player) {
        return logicalRenderStateManager.getCurrentState(display.getName(), player.getUniqueId()) != null;
    }

    public void update(DisplayBase display) {
        for (PlatformPlayer player : playerService.getOnlinePlayers()) {
            if (isIsShownToPlayer(display, player)) {
                updateLogicalState(display, player, true);
            }
        }
    }

    public void postProcess(DisplayBase display) {
        for (PlatformPlayer player : playerService.getOnlinePlayers()) {
            renderLogicalState(display, player);
        }
    }

    private void updateLogicalState(DisplayBase display, PlatformPlayer player, boolean visible) {
        try {
            RenderObjectHandle handle = getRenderObjectHandle(display);
            DisplayRenderContext context = getDisplayRenderContext(player);
            LogicalRenderState currentState = logicalRenderStateManager.getCurrentState(handle.getId(), context.getPlayer().getUniqueId());
            LogicalRenderState state;
            if (visible) {
                state = logicalRenderStateService.updateState(display, context, currentState);
            } else {
                state = null;
                renderService.render(handle, null, context);
            }

            if (currentState == null || state == null) {
                logicalRenderStateManager.updateState(handle.getId(), context.getPlayer().getUniqueId(), state);
            }
        } catch (Exception e) {
            Log.warn("Failed to update logical state of display '%s' for player '%s'.", e, display.getName(), player.getName());
        }
    }

    private void renderLogicalState(DisplayBase display, PlatformPlayer player) {
        try {
            RenderObjectHandle handle = getRenderObjectHandle(display);
            DisplayRenderContext context = getDisplayRenderContext(player);
            LogicalRenderState state = logicalRenderStateManager.getCurrentState(handle.getId(), context.getPlayer().getUniqueId());
            if (state == null) {
                return;
            }

            renderService.render(handle, state, context);
        } catch (Exception e) {
            Log.warn("Failed to render display '%s' for player '%s'.", e, display.getName(), player.getName());
        }
    }

    private RenderObjectHandle getRenderObjectHandle(DisplayBase display) {
        return new RenderObjectHandle(display.getName(), display.getType());
    }

    private DisplayRenderContext getDisplayRenderContext(PlatformPlayer player) {
        return new DisplayRenderContext(player);
    }
}
