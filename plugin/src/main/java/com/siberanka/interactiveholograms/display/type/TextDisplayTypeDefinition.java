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

import com.siberanka.interactiveholograms.api.animations.compile.AnimationCompiler;
import com.siberanka.interactiveholograms.api.animations.compile.CompiledAnimationsOutput;
import com.siberanka.interactiveholograms.display.DisplayBase;
import com.siberanka.interactiveholograms.display.TextDisplay;
import com.siberanka.interactiveholograms.display.render.DisplayRenderContext;
import com.siberanka.interactiveholograms.display.render.content.CompiledDisplayContent;
import com.siberanka.interactiveholograms.display.render.content.CompiledTextDisplayContent;
import com.siberanka.interactiveholograms.display.render.content.CompiledTextDisplayLine;
import com.siberanka.interactiveholograms.display.render.placeholder.DisplayPlaceholderService;
import com.siberanka.interactiveholograms.platform.api.data.display.DisplayType;

import java.util.ArrayList;
import java.util.List;

public class TextDisplayTypeDefinition implements DisplayTypeDefinition<List<CompiledTextDisplayLine>> {

    private final DisplayPlaceholderService displayPlaceholderService;
    private final AnimationCompiler animationCompiler;

    public TextDisplayTypeDefinition(DisplayPlaceholderService displayPlaceholderService, AnimationCompiler animationCompiler) {
        this.displayPlaceholderService = displayPlaceholderService;
        this.animationCompiler = animationCompiler;
    }

    @Override
    public DisplayType getType() {
        return DisplayType.TEXT;
    }

    @Override
    public CompiledDisplayContent<List<CompiledTextDisplayLine>> resolveContent(DisplayBase display, DisplayRenderContext context) {
        TextDisplay textDisplay = getTextDisplay(display);

        List<String> displayLines = textDisplay.getLines();
        List<String> resolvedContent = new ArrayList<>(displayLines.size());
        List<CompiledTextDisplayLine> resolvedLines = new ArrayList<>(displayLines.size());
        boolean anyLineAnimated = false;
        boolean anyLineHasPlaceholders = false;
        for (String line : displayLines) {
            String resolvedLine;
            if (displayPlaceholderService.containsPlaceholders(line)) {
                resolvedLine = displayPlaceholderService.replacePlaceholders(line, context);
                anyLineHasPlaceholders = true;
            } else {
                resolvedLine = line;
            }

            resolvedContent.add(resolvedLine);

            CompiledAnimationsOutput compiledAnimationsOutput = animationCompiler.compileAnimations(resolvedLine);
            resolvedLine = compiledAnimationsOutput.getStrippedString();

            CompiledTextDisplayLine displayLine = new CompiledTextDisplayLine(resolvedLine, compiledAnimationsOutput.getAnimations());

            anyLineAnimated |= displayLine.isAnimated();

            resolvedLines.add(displayLine);
        }
        return new CompiledTextDisplayContent(resolvedContent, resolvedLines, anyLineAnimated, anyLineHasPlaceholders);
    }

    private TextDisplay getTextDisplay(DisplayBase displayBase) {
        if (!(displayBase instanceof TextDisplay)) {
            throw new IllegalArgumentException("Display is not a text display");
        }
        return (TextDisplay) displayBase;
    }
}
