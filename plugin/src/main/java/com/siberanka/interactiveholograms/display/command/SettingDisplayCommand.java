package com.siberanka.interactiveholograms.display.command;

import com.siberanka.interactiveholograms.Permissions;
import com.siberanka.interactiveholograms.api.commands.CommandHandler;
import com.siberanka.interactiveholograms.api.commands.CommandInfo;
import com.siberanka.interactiveholograms.api.commands.DecentCommand;
import com.siberanka.interactiveholograms.api.commands.TabCompleteHandler;
import com.siberanka.interactiveholograms.display.DisplayBase;
import com.siberanka.interactiveholograms.display.DisplayService;
import com.siberanka.interactiveholograms.display.DisplaySettings;
import com.siberanka.interactiveholograms.display.DisplayVisibility;
import com.siberanka.interactiveholograms.plugin.Validator;
import org.bukkit.ChatColor;

@CommandInfo(
        usage = "/ih holograms setting <name> <visibility|permission|persistent> [value]",
        description = "Get or change advanced hologram settings.",
        permissions = {Permissions.COMMAND_DISPLAYS_SETTING},
        minArgs = 2
)
final class SettingDisplayCommand extends DecentCommand {

    private final DisplayService displayService;

    SettingDisplayCommand(DisplayService displayService) {
        super("setting");
        this.displayService = displayService;
    }

    @Override
    public CommandHandler getCommandHandler() {
        return (sender, args) -> {
            Validator.validateArgsCount(2, args);
            DisplayBase display = Validator.getDisplay(displayService, args[0]);
            DisplaySettings settings = display.getSettings();
            String key = args[1].toLowerCase(java.util.Locale.ROOT);
            if (args.length == 2) {
                sender.sendMessage(ChatColor.YELLOW + key + ChatColor.GRAY + " = " + value(settings, key));
                return true;
            }
            switch (key) {
                case "visibility":
                    DisplayVisibility parsed = DisplayVisibility.parse(args[2]);
                    if (!parsed.name().equalsIgnoreCase(args[2]) && !"permission".equalsIgnoreCase(args[2])) {
                        sender.sendMessage(ChatColor.RED + "Visibility must be ALL, MANUAL or PERMISSION_REQUIRED.");
                        return true;
                    }
                    settings.setVisibility(parsed);
                    displayService.updateDisplayVisibility(display);
                    break;
                case "permission":
                    settings.setPermission("none".equalsIgnoreCase(args[2]) ? null : args[2]);
                    displayService.updateDisplayVisibility(display);
                    break;
                case "persistent":
                    if (!"true".equalsIgnoreCase(args[2]) && !"false".equalsIgnoreCase(args[2])) {
                        sender.sendMessage(ChatColor.RED + "Persistent must be true or false.");
                        return true;
                    }
                    settings.setPersistent(Boolean.parseBoolean(args[2]));
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "Unknown setting. Use visibility, permission or persistent.");
                    return true;
            }
            displayService.saveDisplay(display);
            sender.sendMessage(ChatColor.GREEN + "Setting updated: " + key + " = " + value(settings, key));
            return true;
        };
    }

    private String value(DisplaySettings settings, String key) {
        switch (key) {
            case "visibility": return settings.getVisibility().name();
            case "permission": return settings.getPermission() == null ? "default" : settings.getPermission();
            case "persistent": return String.valueOf(settings.isPersistent());
            default: return "unknown";
        }
    }

    @Override
    public TabCompleteHandler getTabCompleteHandler() {
        return (sender, args) -> {
            if (args.length == 1) return TabCompleteHandler.getPartialMatches(args[0], displayService.getRegisteredDisplayNames());
            if (args.length == 2) return TabCompleteHandler.getPartialMatches(args[1], "visibility", "permission", "persistent");
            if (args.length == 3) {
                DisplayBase display = displayService.getDisplay(args[0]);
                if (display == null) return null;
                DisplaySettings settings = display.getSettings();
                if ("visibility".equalsIgnoreCase(args[1])) {
                    return TabCompleteHandler.getPartialMatchesWithCurrent(args[2],
                            settings.getVisibility().name(), "ALL", "MANUAL", "PERMISSION_REQUIRED");
                }
                if ("permission".equalsIgnoreCase(args[1])) {
                    String current = settings.getPermission() == null ? "none" : settings.getPermission();
                    return TabCompleteHandler.getPartialMatchesWithCurrent(args[2], current, "none");
                }
                if ("persistent".equalsIgnoreCase(args[1])) {
                    return TabCompleteHandler.getPartialMatchesWithCurrent(args[2],
                            String.valueOf(settings.isPersistent()), "true", "false");
                }
            }
            return null;
        };
    }
}
