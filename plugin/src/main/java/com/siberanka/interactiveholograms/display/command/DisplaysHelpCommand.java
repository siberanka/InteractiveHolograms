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
import com.siberanka.interactiveholograms.api.commands.CommandHandler;
import com.siberanka.interactiveholograms.api.commands.CommandInfo;
import com.siberanka.interactiveholograms.api.commands.DecentCommand;
import com.siberanka.interactiveholograms.api.commands.TabCompleteHandler;
import com.siberanka.interactiveholograms.api.utils.Common;

@CommandInfo(
        usage = "/ih d help",
        description = "Show general displays help.",
        permissions = {Permissions.COMMAND_DISPLAYS_HELP},
        aliases = {"?"}
)
class DisplaysHelpCommand extends DecentCommand {

    private final DisplaysCommand rootCommand;

    DisplaysHelpCommand(DisplaysCommand rootCommand) {
        super("help");
        this.rootCommand = rootCommand;
    }

    @Override
    public CommandHandler getCommandHandler() {
        return (sender, args) -> {
            sender.sendMessage("");
            Common.tell(sender, " &3&lINTERACTIVEHOLOGRAMS HELP");
            Common.tell(sender, " Packet hologram creation, editing and interactions.");
            sender.sendMessage("");
            printHelpSubCommandsAndAliases(sender, rootCommand);
            return true;
        };
    }

    @Override
    public TabCompleteHandler getTabCompleteHandler() {
        return null;
    }
}
