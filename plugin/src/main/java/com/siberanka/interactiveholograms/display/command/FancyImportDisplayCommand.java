package com.siberanka.interactiveholograms.display.command;

import com.siberanka.interactiveholograms.Permissions;
import com.siberanka.interactiveholograms.api.commands.CommandHandler;
import com.siberanka.interactiveholograms.api.commands.CommandInfo;
import com.siberanka.interactiveholograms.api.commands.DecentCommand;
import com.siberanka.interactiveholograms.api.commands.TabCompleteHandler;
import com.siberanka.interactiveholograms.api.utils.scheduler.S;
import com.siberanka.interactiveholograms.display.DisplayService;
import com.siberanka.interactiveholograms.display.config.HologramImportService;
import com.siberanka.interactiveholograms.display.config.HologramImportSource;
import org.bukkit.ChatColor;

import java.util.Arrays;

@CommandInfo(
        usage = "/ih holograms import-fancy [relative-path] [--overwrite]",
        description = "Import packet displays from FancyHolograms YAML.",
        aliases = {"importfancy", "fancy-import"},
        permissions = {Permissions.COMMAND_DISPLAYS_IMPORT}
)
final class FancyImportDisplayCommand extends DecentCommand {

    private final HologramImportService importer;
    private final DisplayService displayService;

    FancyImportDisplayCommand(HologramImportService importer, DisplayService displayService) {
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
            sender.sendMessage(ChatColor.YELLOW + "FancyHolograms import started...");
            S.async(() -> {
                try {
                    HologramImportService.ImportResult result = importer.importFrom(
                            HologramImportSource.FANCY_HOLOGRAMS, path, overwrite);
                    S.sync(() -> {
                        displayService.reload();
                        sender.sendMessage(ChatColor.GREEN + "FancyHolograms import complete: "
                                + result.getImported() + " imported, " + result.getSkipped() + " skipped.");
                    });
                } catch (Exception exception) {
                    S.sync(() -> sender.sendMessage(ChatColor.RED
                            + "FancyHolograms import failed: " + exception.getMessage()));
                }
            });
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
