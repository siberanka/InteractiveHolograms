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

import com.siberanka.interactiveholograms.display.attribute.AttributeKey;
import com.siberanka.interactiveholograms.display.attribute.definition.AttributeDefinition;
import com.siberanka.interactiveholograms.display.attribute.definition.AttributeDefinitionRegistry;
import com.siberanka.interactiveholograms.display.attribute.value.CompiledAttributeValue;
import com.siberanka.interactiveholograms.display.render.content.CompiledBlockDisplayContent;
import com.siberanka.interactiveholograms.display.render.content.CompiledItemDisplayContent;
import com.siberanka.interactiveholograms.display.render.content.CompiledTextDisplayContent;
import com.siberanka.interactiveholograms.display.render.state.LogicalRenderState;
import com.siberanka.interactiveholograms.display.render.state.PresentedRenderState;
import com.siberanka.interactiveholograms.platform.api.data.display.BlockDisplayContent;
import com.siberanka.interactiveholograms.platform.api.data.display.DisplayType;
import com.siberanka.interactiveholograms.platform.api.data.display.ItemDisplayContent;
import com.siberanka.interactiveholograms.platform.api.data.display.TextDisplayContent;
import com.siberanka.interactiveholograms.profiler.DecentProfiler;
import com.siberanka.interactiveholograms.profiler.Metrics;
import com.siberanka.interactiveholograms.profiler.TimerHandle;

import java.util.List;
import java.util.Map;

public class DisplayPostProcessor {

    private final AttributeDefinitionRegistry attributeDefinitionRegistry;
    private final TextPostProcessor textPostProcessor;

    public DisplayPostProcessor(AttributeDefinitionRegistry attributeDefinitionRegistry, TextPostProcessor textPostProcessor) {
        this.attributeDefinitionRegistry = attributeDefinitionRegistry;
        this.textPostProcessor = textPostProcessor;
    }

    public PresentedRenderState postProcess(LogicalRenderState logicalState, PresentedRenderState presentedState) {
        try (TimerHandle ignored = DecentProfiler.getInstance().startTimer(Metrics.POST_PROCESS)) {
            return postProcessInternal(logicalState, presentedState);
        }
    }

    private PresentedRenderState postProcessInternal(LogicalRenderState logicalState, PresentedRenderState presentedState) {
        if (logicalState == null) {
            return null;
        }

        if (presentedState == null) {
            return createMutableRenderState(logicalState);
        }

        presentedState.beginFrame();

        if (!logicalState.getLocation().equals(presentedState.getLocation())) {
            presentedState.setLocation(logicalState.getLocation());
        }

        if (logicalState.getContent().isDirty()) {
            applyContent(logicalState, presentedState);
        }

        applyDirtyAttributes(logicalState, presentedState);
        return presentedState;
    }

    private PresentedRenderState createMutableRenderState(LogicalRenderState logicalState) {
        PresentedRenderState state = new PresentedRenderState(logicalState.getId(), logicalState.getDisplayType());
        state.setLocation(logicalState.getLocation());
        applyContent(logicalState, state);
        applyAttributes(logicalState, state);
        return state;
    }

    private void applyContent(LogicalRenderState logicalState, PresentedRenderState presentedState) {
        if (logicalState.getDisplayType() == DisplayType.TEXT) {
            applyTextContent(presentedState, (CompiledTextDisplayContent) logicalState.getContent());
        } else if (logicalState.getDisplayType() == DisplayType.ITEM) {
            applyItemContent(presentedState, (CompiledItemDisplayContent) logicalState.getContent());
        } else if (logicalState.getDisplayType() == DisplayType.BLOCK) {
            applyBlockContent(presentedState, (CompiledBlockDisplayContent) logicalState.getContent());
        }
    }

    private void applyTextContent(PresentedRenderState presentedState, CompiledTextDisplayContent textContent) {
        List<String> postProcessedLines = textPostProcessor.postProcess(textContent);
        TextDisplayContent content = new TextDisplayContent(postProcessedLines);
        presentedState.setContent(content);
    }

    private void applyItemContent(PresentedRenderState presentedState, CompiledItemDisplayContent itemContent) {
        ItemDisplayContent content = new ItemDisplayContent(itemContent.getContent());
        presentedState.setContent(content);
    }

    private void applyBlockContent(PresentedRenderState presentedState, CompiledBlockDisplayContent blockContent) {
        BlockDisplayContent content = new BlockDisplayContent(blockContent.getContent());
        presentedState.setContent(content);
    }

    private void applyDirtyAttributes(LogicalRenderState logicalState, PresentedRenderState presentedState) {
        Map<AttributeKey<?>, CompiledAttributeValue<?>> attributeMap = logicalState.getAttributeValues();
        for (Map.Entry<AttributeKey<?>, CompiledAttributeValue<?>> entry : attributeMap.entrySet()) {
            CompiledAttributeValue<?> value = entry.getValue();
            if (value.isDirty()) {
                applyAttribute(entry.getKey(), value, presentedState);
            }
        }
    }

    private void applyAttributes(LogicalRenderState logicalState, PresentedRenderState presentedState) {
        Map<AttributeKey<?>, CompiledAttributeValue<?>> attributeMap = logicalState.getAttributeValues();
        for (Map.Entry<AttributeKey<?>, CompiledAttributeValue<?>> entry : attributeMap.entrySet()) {
            applyAttribute(entry.getKey(), entry.getValue(), presentedState);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void applyAttribute(AttributeKey<T> key, CompiledAttributeValue<?> attributeValue, PresentedRenderState presentedState) {
        AttributeDefinition<T> definition = attributeDefinitionRegistry.getDefinitionByKey(key);
        if (definition != null) {
            definition.apply((CompiledAttributeValue<T>) attributeValue, presentedState);
        }
    }
}
