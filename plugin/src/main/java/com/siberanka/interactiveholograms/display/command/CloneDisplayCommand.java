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
import com.siberanka.interactiveholograms.api.utils.Common;
import com.siberanka.interactiveholograms.platform.api.data.DecentLocation;
import com.siberanka.interactiveholograms.display.DisplayBase;
import com.siberanka.interactiveholograms.display.DisplayCloneService;
import com.siberanka.interactiveholograms.display.DisplayService;
import com.siberanka.interactiveholograms.plugin.Validator;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@CommandInfo(
        usage = "/ih d clone <name> <new_name>",
        description = "Clone an existing display.",
        permissions = {Permissions.COMMAND_DISPLAYS_CLONE},
        playerOnly = true,
        minArgs = 2
)
class CloneDisplayCommand extends DecentCommand {

    private final DisplayService displayService;
    private final DisplayCloneService displayCloneService;

    CloneDisplayCommand(DisplayService displayService, DisplayCloneService displayCloneService) {
        super("clone");
        this.displayService = displayService;
        this.displayCloneService = displayCloneService;
    }

    @Override
    public CommandHandler getCommandHandler() {
        return (sender, args) -> {
            Validator.validateArgsCount(2, args);
            DisplayBase display = Validator.getDisplay(displayService, args[0]);
            String name = args[1];
            if (!name.matches(Common.NAME_REGEX)) {
                Lang.DISPLAY_INVALID_NAME.send(sender, name);
                return true;
            }
            if (displayService.getDisplay(name) != null) {
                Lang.DISPLAY_ALREADY_EXISTS.send(sender, name);
                return true;
            }

            DisplayBase clonedDisplay = displayCloneService.cloneDisplay(display, name);
            DecentLocation displayLocation = clonedDisplay.getLocation();
            Location playerLocation = ((Player) sender).getLocation();
            clonedDisplay.setLocation(new DecentLocation(
                    playerLocation.getWorld().getName(),
                    playerLocation.getX(),
                    playerLocation.getY(),
                    playerLocation.getZ(),
                    displayLocation.getYaw(),
                    displayLocation.getPitch()
            ));
            displayService.updateDisplay(clonedDisplay);
            displayService.saveDisplay(clonedDisplay);

            Lang.DISPLAY_CLONED.send(sender, display.getName());
            return true;
        };
    }

    @Override
    public TabCompleteHandler getTabCompleteHandler() {
        return (sender, args) -> {
            if (args.length == 1) {
                return TabCompleteHandler.getPartialMatches(args[0], displayService.getRegisteredDisplayNames());
            }
            return null;
        };
    }
}
