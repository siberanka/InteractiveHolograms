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

package com.siberanka.interactiveholograms.display.render.state;

import com.siberanka.interactiveholograms.display.DisplayBase;
import com.siberanka.interactiveholograms.display.attribute.AttributeKey;
import com.siberanka.interactiveholograms.display.attribute.DisplayAttribute;
import com.siberanka.interactiveholograms.display.attribute.value.AttributeValue;
import com.siberanka.interactiveholograms.display.attribute.value.CompiledAttributeValue;
import com.siberanka.interactiveholograms.display.attribute.value.StaticCompiledAttributeValue;
import com.siberanka.interactiveholograms.display.render.DisplayRenderContext;
import com.siberanka.interactiveholograms.display.render.content.CompiledDisplayContent;
import com.siberanka.interactiveholograms.display.type.DisplayTypeDefinition;
import com.siberanka.interactiveholograms.display.type.DisplayTypeRegistry;

public class LogicalRenderStateService {

    private final DisplayTypeRegistry displayTypeRegistry;

    public LogicalRenderStateService(DisplayTypeRegistry displayTypeRegistry) {
        this.displayTypeRegistry = displayTypeRegistry;
    }

    public LogicalRenderState updateState(DisplayBase display, DisplayRenderContext context, LogicalRenderState currentState) {
        if (currentState == null) {
            return createNewLogicalDisplayRenderState(display, context);
        }

        if (display.checkContentDirty() || currentState.getContent().isDynamic()) {
            applyContent(display, currentState, context);
        }
        if (display.checkConfigDirty()) {
            currentState.setLocation(display.getLocation());
            currentState.clearAttributes();
            applyAttributes(display, currentState, context);
        }

        return currentState;
    }

    public LogicalRenderState refreshContent(DisplayBase display,
                                             DisplayRenderContext context,
                                             LogicalRenderState currentState) {
        if (currentState == null) {
            return createNewLogicalDisplayRenderState(display, context);
        }
        applyContent(display, currentState, context);
        return currentState;
    }

    private LogicalRenderState createNewLogicalDisplayRenderState(DisplayBase display, DisplayRenderContext context) {
        LogicalRenderState state = new LogicalRenderState(display.getName(), display.getType());
        state.setLocation(display.getLocation());
        applyContent(display, state, context);
        applyAttributes(display, state, context);
        return state;
    }

    private void applyAttributes(DisplayBase display, LogicalRenderState state, DisplayRenderContext context) {
        for (AttributeKey<?> attributeKey : display.getAttributesMap().keySet()) {
            applyAttribute(attributeKey, display, state, context);
        }
    }

    private <T> void applyAttribute(AttributeKey<T> key, DisplayBase display, LogicalRenderState state, DisplayRenderContext context) {
        DisplayAttribute<T> attribute = display.getAttribute(key);
        CompiledAttributeValue<T> value = compileAttribute(attribute, context);
        state.addAttribute(key, value);
    }

    private <T> CompiledAttributeValue<T> compileAttribute(DisplayAttribute<T> attribute, DisplayRenderContext context) {
        AttributeValue<T> value = attribute.getValue();
        if (value == null) {
            return StaticCompiledAttributeValue.empty();
        }
        return value.compile(context);
    }

    private void applyContent(DisplayBase display, LogicalRenderState state, DisplayRenderContext context) {
        DisplayTypeDefinition<?> displayTypeDefinition = displayTypeRegistry.getDefinition(display.getType());
        CompiledDisplayContent<?> content = displayTypeDefinition.resolveContent(display, context);
        if (content.equals(state.getContent())) {
            return; // Content hasn't changed
        }
        state.setContent(content);
    }
}
