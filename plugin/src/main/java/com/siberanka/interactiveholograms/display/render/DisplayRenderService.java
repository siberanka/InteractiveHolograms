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

import com.siberanka.interactiveholograms.display.render.state.LogicalRenderState;
import com.siberanka.interactiveholograms.display.render.state.PresentedRenderState;
import com.siberanka.interactiveholograms.display.render.state.PresentedRenderStateManager;
import com.siberanka.interactiveholograms.platform.api.render.PlatformRenderService;
import com.siberanka.interactiveholograms.platform.api.render.RenderObjectHandle;
import com.siberanka.interactiveholograms.platform.api.render.intent.RenderIntent;
import com.siberanka.interactiveholograms.profiler.DecentProfiler;
import com.siberanka.interactiveholograms.profiler.Metrics;
import com.siberanka.interactiveholograms.profiler.TimerHandle;

import java.util.List;
import java.util.UUID;

public class DisplayRenderService {

    private final DisplayRenderIntentMaterializer intentMaterializer;
    private final PlatformRenderService platformRenderService;
    private final PresentedRenderStateManager presentedRenderStateManager;
    private final DisplayPostProcessor postProcessor;

    public DisplayRenderService(DisplayRenderIntentMaterializer intentMaterializer,
                                PlatformRenderService platformRenderService,
                                PresentedRenderStateManager presentedRenderStateManager,
                                DisplayPostProcessor postProcessor) {
        this.intentMaterializer = intentMaterializer;
        this.platformRenderService = platformRenderService;
        this.presentedRenderStateManager = presentedRenderStateManager;
        this.postProcessor = postProcessor;
    }

    public void render(RenderObjectHandle handle, LogicalRenderState logicalState, DisplayRenderContext context) {
        PresentedRenderState previousState = getPreviousState(handle.getId(), context);
        PresentedRenderState currentState = postProcessor.postProcess(logicalState, previousState);
        if (logicalState != null && currentState != null && !currentState.hasChanges()) {
            return;
        }
        List<RenderIntent> intents = intentMaterializer.materializeIntents(currentState);
        if (intents.isEmpty()) {
            return; // No changes, skip rendering
        }

        try (TimerHandle ignored = DecentProfiler.getInstance().startTimer(Metrics.RENDER_PLATFORM)) {
            platformRenderService.render(context.getPlayer(), handle, intents);
        }

        if (previousState == null || currentState == null) {
            saveCurrentState(handle.getId(), currentState, context);
        }
    }

    private void saveCurrentState(String displayName, PresentedRenderState state, DisplayRenderContext context) {
        UUID playerUniqueId = context.getPlayer().getUniqueId();
        presentedRenderStateManager.updateState(displayName, playerUniqueId, state);
    }

    private PresentedRenderState getPreviousState(String displayName, DisplayRenderContext context) {
        UUID playerUniqueId = context.getPlayer().getUniqueId();
        return presentedRenderStateManager.getCurrentState(displayName, playerUniqueId);
    }
}
