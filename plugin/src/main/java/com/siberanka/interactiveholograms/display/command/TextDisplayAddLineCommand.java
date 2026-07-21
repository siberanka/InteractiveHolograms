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

package com.siberanka.interactiveholograms.display.command;

import com.siberanka.interactiveholograms.Permissions;
import com.siberanka.interactiveholograms.api.Lang;
import com.siberanka.interactiveholograms.api.commands.CommandHandler;
import com.siberanka.interactiveholograms.api.commands.CommandInfo;
import com.siberanka.interactiveholograms.api.commands.DecentCommand;
import com.siberanka.interactiveholograms.api.commands.TabCompleteHandler;
import com.siberanka.interactiveholograms.display.DisplayBase;
import com.siberanka.interactiveholograms.display.DisplayService;
import com.siberanka.interactiveholograms.display.TextDisplay;
import com.siberanka.interactiveholograms.platform.api.data.display.DisplayType;
import com.siberanka.interactiveholograms.plugin.Validator;

import java.util.Arrays;

@CommandInfo(
        usage = "/ih d addline <name> <text>",
        description = "Add a line of text to a Text Display.",
        permissions = {Permissions.COMMAND_DISPLAYS_TEXT_ADD_LINE},
        aliases = {"appendline"},
        minArgs = 2
)
class TextDisplayAddLineCommand extends DecentCommand {

    private final DisplayService displayService;
    private final DisplayTabCompleteHelper tabCompleteHelper;

    TextDisplayAddLineCommand(DisplayService displayService, DisplayTabCompleteHelper tabCompleteHelper) {
        super("addline");
        this.displayService = displayService;
        this.tabCompleteHelper = tabCompleteHelper;
    }

    @Override
    public CommandHandler getCommandHandler() {
        return (sender, args) -> {
            Validator.validateArgsCount(2, args);
            DisplayBase display = Validator.getDisplayOfType(displayService, args[0], DisplayType.TEXT);

            String text = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            TextDisplay textDisplay = (TextDisplay) display;
            textDisplay.addLine(text);
            displayService.updateDisplay(display);
            displayService.saveDisplay(display);
            Lang.DISPLAY_TEXT_LINE_ADDED.send(sender, display.getName());
            return true;
        };
    }

    @Override
    public TabCompleteHandler getTabCompleteHandler() {
        return (sender, args) -> {
            if (args.length == 1) {
                return tabCompleteHelper.getDisplayNames(args[0]);
            }
            return null;
        };
    }
}
