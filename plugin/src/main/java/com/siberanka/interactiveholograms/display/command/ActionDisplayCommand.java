package com.siberanka.interactiveholograms.display.command;

import com.siberanka.interactiveholograms.Permissions;
import com.siberanka.interactiveholograms.api.actions.Action;
import com.siberanka.interactiveholograms.api.actions.ClickType;
import com.siberanka.interactiveholograms.api.commands.CommandHandler;
import com.siberanka.interactiveholograms.api.commands.CommandInfo;
import com.siberanka.interactiveholograms.api.commands.DecentCommand;
import com.siberanka.interactiveholograms.api.commands.TabCompleteHandler;
import com.siberanka.interactiveholograms.display.DisplayBase;
import com.siberanka.interactiveholograms.display.DisplayService;
import com.siberanka.interactiveholograms.plugin.Validator;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CommandInfo(
        usage = "/ih holograms action <name> <list|add|remove|clear> <click> [action|index]",
        description = "Manage packet-hitbox click actions.",
        permissions = {Permissions.COMMAND_DISPLAYS_ACTION},
        minArgs = 3
)
final class ActionDisplayCommand extends DecentCommand {

    private final DisplayService displayService;

    ActionDisplayCommand(DisplayService displayService) {
        super("action");
        this.displayService = displayService;
    }

    @Override
    public CommandHandler getCommandHandler() {
        return (sender, args) -> {
            Validator.validateArgsCount(3, args);
            DisplayBase display = Validator.getDisplay(displayService, args[0]);
            String operation = args[1].toLowerCase(java.util.Locale.ROOT);
            ClickType click = ClickType.fromString(args[2]);
            if (click == null) {
                sender.sendMessage(ChatColor.RED + "Click type must be LEFT, RIGHT, SHIFT_LEFT or SHIFT_RIGHT.");
                return true;
            }

            Map<ClickType, List<Action>> actions = mutableActions(display);
            List<Action> entries = actions.computeIfAbsent(click, ignored -> new ArrayList<>());
            if ("list".equals(operation)) {
                sender.sendMessage(ChatColor.GOLD + display.getName() + " / " + click + " actions:");
                if (entries.isEmpty()) sender.sendMessage(ChatColor.GRAY + "(none)");
                for (int i = 0; i < entries.size(); i++) {
                    sender.sendMessage(ChatColor.YELLOW + String.valueOf(i + 1) + ". " + ChatColor.WHITE + entries.get(i));
                }
                return true;
            }
            if ("clear".equals(operation)) {
                actions.remove(click);
            } else if ("add".equals(operation)) {
                Validator.validateArgsCount(4, args);
                String actionText = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                try {
                    entries.add(new Action(actionText));
                } catch (IllegalArgumentException exception) {
                    sender.sendMessage(ChatColor.RED + exception.getMessage());
                    return true;
                }
            } else if ("remove".equals(operation)) {
                Validator.validateArgsCount(4, args);
                int index;
                try {
                    index = Integer.parseInt(args[3]) - 1;
                } catch (NumberFormatException exception) {
                    index = -1;
                }
                if (index < 0 || index >= entries.size()) {
                    sender.sendMessage(ChatColor.RED + "Action index is out of range.");
                    return true;
                }
                entries.remove(index);
                if (entries.isEmpty()) actions.remove(click);
            } else {
                sender.sendMessage(ChatColor.RED + "Operation must be list, add, remove or clear.");
                return true;
            }
            display.setActions(actions);
            displayService.saveDisplay(display);
            sender.sendMessage(ChatColor.GREEN + "Hologram actions updated. Packet hitbox is "
                    + (display.hasActions() ? "active." : "inactive."));
            return true;
        };
    }

    private Map<ClickType, List<Action>> mutableActions(DisplayBase display) {
        Map<ClickType, List<Action>> copy = new EnumMap<>(ClickType.class);
        display.getActions().forEach((type, values) -> copy.put(type, new ArrayList<>(values)));
        return copy;
    }

    @Override
    public TabCompleteHandler getTabCompleteHandler() {
        return (sender, args) -> {
            if (args.length == 1) return TabCompleteHandler.getPartialMatches(args[0], displayService.getRegisteredDisplayNames());
            if (args.length == 2) return TabCompleteHandler.getPartialMatches(args[1], "list", "add", "remove", "clear");
            if (args.length == 3) return TabCompleteHandler.getPartialMatches(args[2], Arrays.stream(ClickType.values()).map(Enum::name).collect(Collectors.toList()));
            if (args.length == 4 && "add".equalsIgnoreCase(args[1])) {
                return TabCompleteHandler.getPartialMatches(args[3], "MESSAGE:Hello!", "COMMAND:/spawn", "CONSOLE:give {player} diamond", "SOUND:ENTITY_EXPERIENCE_ORB_PICKUP");
            }
            return null;
        };
    }
}
