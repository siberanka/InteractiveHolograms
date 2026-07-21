package com.siberanka.interactiveholograms.display.command;

import com.siberanka.interactiveholograms.Permissions;
import com.siberanka.interactiveholograms.api.commands.CommandHandler;
import com.siberanka.interactiveholograms.api.commands.CommandInfo;
import com.siberanka.interactiveholograms.api.commands.DecentCommand;
import com.siberanka.interactiveholograms.api.commands.TabCompleteHandler;
import com.siberanka.interactiveholograms.display.DisplayService;
import com.siberanka.interactiveholograms.display.config.DecentHologramsImporter;
import org.bukkit.ChatColor;

import java.util.Arrays;

@CommandInfo(usage = "/ih holograms import-decent [relative-path] [--overwrite]",
        description = "Import DecentHolograms YAML files into packet displays.",
        aliases = {"importdecent", "decent-import"}, permissions = {Permissions.COMMAND_DISPLAYS_IMPORT})
final class DecentImportDisplayCommand extends DecentCommand {
    private final DecentHologramsImporter importer; private final DisplayService displays;
    DecentImportDisplayCommand(DecentHologramsImporter importer, DisplayService displays) {
        super("import-decent"); this.importer = importer; this.displays = displays;
    }
    @Override public CommandHandler getCommandHandler() {
        return (sender, args) -> {
            boolean overwrite = Arrays.stream(args).anyMatch("--overwrite"::equalsIgnoreCase);
            String path = Arrays.stream(args).filter(value -> !"--overwrite".equalsIgnoreCase(value)).findFirst().orElse(null);
            try {
                DecentHologramsImporter.ImportResult result = importer.importYaml(path, overwrite);
                displays.reload();
                sender.sendMessage(ChatColor.GREEN + "DecentHolograms import complete: " + result.getImported()
                        + " imported, " + result.getSkipped() + " skipped, " + result.getWarnings() + " warning(s).");
            } catch (Exception exception) {
                sender.sendMessage(ChatColor.RED + "DecentHolograms import failed: " + exception.getMessage());
            }
            return true;
        };
    }
    @Override public TabCompleteHandler getTabCompleteHandler() {
        return (sender, args) -> args.length == 1
                ? TabCompleteHandler.getPartialMatches(args[0], "plugins/DecentHolograms/holograms", "--overwrite")
                : TabCompleteHandler.getPartialMatches(args[args.length - 1], "--overwrite");
    }
}
