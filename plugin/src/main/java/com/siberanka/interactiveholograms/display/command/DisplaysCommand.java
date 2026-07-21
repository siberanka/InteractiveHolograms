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
import com.siberanka.interactiveholograms.display.DisplayCloneService;
import com.siberanka.interactiveholograms.display.DisplayService;
import com.siberanka.interactiveholograms.display.attribute.AttributeCommandService;
import com.siberanka.interactiveholograms.display.attribute.DisplayAttributeService;
import com.siberanka.interactiveholograms.display.attribute.defaults.AttributeDefaultService;
import com.siberanka.interactiveholograms.platform.api.capability.PlatformMaterialService;
import com.siberanka.interactiveholograms.display.config.FancyHologramsImporter;
import com.siberanka.interactiveholograms.display.config.DecentHologramsImporter;
import com.siberanka.interactiveholograms.display.integration.ModelCatalog;

@CommandInfo(
        usage = "/ih holograms help",
        description = "All commands for editing packet holograms.",
        permissions = {Permissions.COMMAND_DISPLAYS},
        aliases = {"hologram", "display", "displays", "d"}
)
public class DisplaysCommand extends DecentCommand {

    public DisplaysCommand(DisplayService displayService,
                           DisplayCloneService displayCloneService,
                           AttributeCommandService attributeCommandService,
                           AttributeDefaultService attributeDefaultService,
                           DisplayAttributeService displayAttributeService,
                           PlatformMaterialService materialService,
                           FancyHologramsImporter fancyImporter,
                           DecentHologramsImporter decentImporter,
                           ModelCatalog modelCatalog) {
        super("holograms");

        addSubCommand(new DisplaysHelpCommand(this));
        addSubCommand(new CreateDisplayCommand(displayService, attributeDefaultService, materialService));
        addSubCommand(new DeleteDisplayCommand(displayService));
        addSubCommand(new MoveDisplayCommand(displayService));
        addSubCommand(new RenameDisplayCommand(displayService, displayCloneService));
        addSubCommand(new CloneDisplayCommand(displayService, displayCloneService));
        addSubCommand(new EnableDisplayCommand(displayService));
        addSubCommand(new DisableDisplayCommand(displayService));
        addSubCommand(new DisplayRangeDisplayCommand(displayService));
        addSubCommand(new UpdateIntervalDisplayCommand(displayService));
        addSubCommand(new AttributeDisplayCommand(displayService, attributeCommandService));
        addSubCommand(new AttributeResetDisplayCommand(displayService, attributeCommandService));
        addSubCommand(new AttributeListDisplayCommand(displayService, displayAttributeService));
        addSubCommand(new CenterDisplayCommand(displayService));
        addSubCommand(new MoveHereDisplayCommand(displayService));
        addSubCommand(new TeleportDisplayCommand(displayService));
        addSubCommand(new FacingDisplayCommand(displayService));
        addSubCommand(new ListDisplaysCommand(displayService));
        addSubCommand(new NearbyDisplaysCommand(displayService));
        addSubCommand(new BlockDisplaySetBlockCommand(displayService, materialService));
        addSubCommand(new ItemDisplaySetItemCommand(displayService, materialService));
        addSubCommand(new FancyImportDisplayCommand(fancyImporter, displayService));
        addSubCommand(new DecentImportDisplayCommand(decentImporter, displayService));
        addSubCommand(new ModelDisplayCommand(displayService, modelCatalog));
        addSubCommand(new ActionDisplayCommand(displayService));
        addSubCommand(new SettingDisplayCommand(displayService));
        DisplayTabCompleteHelper tabCompleteHelper = new DisplayTabCompleteHelper(displayService);
        addSubCommand(new TextDisplayAddLineCommand(displayService, tabCompleteHelper));
        addSubCommand(new TextDisplayInsertLineCommand(displayService, tabCompleteHelper));
        addSubCommand(new TextDisplayRemoveLineCommand(displayService, tabCompleteHelper));
        addSubCommand(new TextDisplaySetLineCommand(displayService, tabCompleteHelper));
        addSubCommand(new TextDisplaySwapLineCommand(displayService, tabCompleteHelper));
    }

    @Override
    public CommandHandler getCommandHandler() {
        return (sender, args) -> {
            if (args.length == 0) {
                Lang.USE_HELP.send(sender);
                return true;
            }
            Lang.UNKNOWN_SUB_COMMAND.send(sender);
            Lang.USE_HELP.send(sender);
            return true;
        };
    }

    @Override
    public TabCompleteHandler getTabCompleteHandler() {
        return null;
    }
}
