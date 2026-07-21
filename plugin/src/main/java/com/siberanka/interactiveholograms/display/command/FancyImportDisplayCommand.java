package com.siberanka.interactiveholograms.display.command;

import com.siberanka.interactiveholograms.Permissions;
import com.siberanka.interactiveholograms.api.commands.CommandHandler;
import com.siberanka.interactiveholograms.api.commands.CommandInfo;
import com.siberanka.interactiveholograms.api.commands.DecentCommand;
import com.siberanka.interactiveholograms.api.commands.TabCompleteHandler;
import com.siberanka.interactiveholograms.display.DisplayService;
import com.siberanka.interactiveholograms.display.config.FancyHologramsImporter;
import org.bukkit.ChatColor;

import java.util.Arrays;

@CommandInfo(
        usage = "/ih holograms import-fancy [relative-path] [--overwrite]",
        description = "Import packet displays from FancyHolograms YAML.",
        aliases = {"importfancy", "fancy-import"},
        permissions = {Permissions.COMMAND_DISPLAYS_IMPORT}
)
final class FancyImportDisplayCommand extends DecentCommand {

    private final FancyHologramsImporter importer;
    private final DisplayService displayService;

    FancyImportDisplayCommand(FancyHologramsImporter importer, DisplayService displayService) {
        super("import-fancy");
        this.importer = importer;
        this.displayService = displayService;
    }

    @Override
    public CommandHandler getCommandHandler() {
        return (sender, args) -> {
            boolean overwrite = Arrays.stream(args).anyMatch("--overwrite"::equalsIgnoreCase);
            String path = Arrays.stream(args)
                    .filter(argument -> !"--overwrite".equalsIgnoreCase(argument))
                    .findFirst().orElse(null);
            try {
                FancyHologramsImporter.ImportResult result = importer.importYaml(path, overwrite);
                displayService.reload();
                sender.sendMessage(ChatColor.GREEN + "FancyHolograms import complete: "
                        + result.getImported() + " imported, " + result.getSkipped() + " skipped.");
            } catch (Exception exception) {
                sender.sendMessage(ChatColor.RED + "FancyHolograms import failed: " + exception.getMessage());
            }
            return true;
        };
    }

    @Override
    public TabCompleteHandler getTabCompleteHandler() {
        return (sender, args) -> args.length == 1
                ? TabCompleteHandler.getPartialMatches(args[0], "plugins/FancyHolograms/holograms.yml", "--overwrite")
                : TabCompleteHandler.getPartialMatches(args[args.length - 1], "--overwrite");
    }
}
