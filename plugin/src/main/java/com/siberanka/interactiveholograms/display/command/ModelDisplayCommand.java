package com.siberanka.interactiveholograms.display.command;

import com.siberanka.interactiveholograms.Permissions;
import com.siberanka.interactiveholograms.api.commands.CommandHandler;
import com.siberanka.interactiveholograms.api.commands.CommandInfo;
import com.siberanka.interactiveholograms.api.commands.DecentCommand;
import com.siberanka.interactiveholograms.api.commands.TabCompleteHandler;
import com.siberanka.interactiveholograms.display.DisplayBase;
import com.siberanka.interactiveholograms.display.DisplayService;
import com.siberanka.interactiveholograms.display.integration.ModelCatalog;
import com.siberanka.interactiveholograms.display.integration.ModelProvider;
import com.siberanka.interactiveholograms.plugin.Validator;
import org.bukkit.ChatColor;

import java.util.Collection;

@CommandInfo(
        usage = "/ih holograms model <name> <NONE|BETTERMODEL|MYTHICMOBS|MODELENGINE> [model] [animation]",
        description = "Select an entity-free custom model with dynamic tab completion.",
        permissions = {Permissions.COMMAND_DISPLAYS_MODEL},
        minArgs = 2
)
final class ModelDisplayCommand extends DecentCommand {
    private final DisplayService displays;
    private final ModelCatalog catalog;

    ModelDisplayCommand(DisplayService displays, ModelCatalog catalog) {
        super("model");
        this.displays = displays;
        this.catalog = catalog;
    }

    @Override
    public CommandHandler getCommandHandler() {
        return (sender, args) -> {
            Validator.validateArgsCount(2, args);
            DisplayBase display = Validator.getDisplay(displays, args[0]);
            ModelProvider provider = ModelProvider.parse(args[1]);
            if (provider == null) {
                sender.sendMessage(ChatColor.RED + "Unknown model provider.");
                return true;
            }
            if (provider == ModelProvider.NONE) {
                display.setModelProvider("NONE"); display.setModel(null); display.setAnimation(null);
            } else {
                Validator.validateArgsCount(3, args);
                if (!catalog.isAvailable(provider)) {
                    sender.sendMessage(ChatColor.RED + provider.name() + " is not enabled on this server.");
                    return true;
                }
                String model = canonical(args[2], catalog.models(provider));
                if (model == null) {
                    sender.sendMessage(ChatColor.RED + "Unknown " + provider.name() + " model/mob: " + args[2]);
                    return true;
                }
                String animation = null;
                if (args.length > 3 && !"none".equalsIgnoreCase(args[3])) {
                    animation = canonical(args[3], catalog.animations(provider, model));
                    if (animation == null) {
                        sender.sendMessage(ChatColor.RED + "Unknown animation: " + args[3]);
                        return true;
                    }
                }
                display.setModelProvider(provider.name()); display.setModel(model); display.setAnimation(animation);
            }
            displays.updateDisplay(display);
            displays.saveDisplay(display);
            sender.sendMessage(ChatColor.GREEN + "Model selection updated for " + display.getName() + '.');
            return true;
        };
    }

    @Override
    public TabCompleteHandler getTabCompleteHandler() {
        return (sender, args) -> {
            if (args.length == 1) return TabCompleteHandler.getPartialMatches(args[0], displays.getRegisteredDisplayNames());
            if (args.length == 2) return TabCompleteHandler.getPartialMatches(args[1], catalog.providers());
            ModelProvider provider = ModelProvider.parse(args[1]);
            if (args.length == 3) return TabCompleteHandler.getPartialMatches(args[2], catalog.models(provider));
            if (args.length == 4) {
                Collection<String> animations = catalog.animations(provider, args[2]);
                java.util.List<String> options = new java.util.ArrayList<>(animations);
                options.add("none");
                return TabCompleteHandler.getPartialMatches(args[3], options);
            }
            return null;
        };
    }

    private String canonical(String input, Collection<String> options) {
        if (input == null || input.length() > 128) return null;
        for (String option : options) if (option.equalsIgnoreCase(input)) return option;
        return null;
    }
}
