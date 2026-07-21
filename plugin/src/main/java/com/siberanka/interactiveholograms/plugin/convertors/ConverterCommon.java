package com.siberanka.interactiveholograms.plugin.convertors;

import com.siberanka.interactiveholograms.api.InteractiveHolograms;
import com.siberanka.interactiveholograms.api.actions.Action;
import com.siberanka.interactiveholograms.api.actions.ActionType;
import com.siberanka.interactiveholograms.api.actions.ClickType;
import com.siberanka.interactiveholograms.api.holograms.Hologram;
import com.siberanka.interactiveholograms.api.holograms.HologramLine;
import com.siberanka.interactiveholograms.api.holograms.HologramPage;
import com.siberanka.interactiveholograms.api.utils.Log;
import lombok.experimental.UtilityClass;
import org.bukkit.Location;

import java.io.File;
import java.util.List;

@UtilityClass
public final class ConverterCommon {
    
    public static void createHologram(ConvertorResult convertorResult, String name, Location location, List<String> lines, InteractiveHolograms plugin) {
        if (plugin.getHologramManager().containsHologram(name)) {
            Log.warn("A hologram with name '%s' already exists, skipping...", name);
            convertorResult.addSkipped();
            return ;
        }
        Hologram hologram = new Hologram(name, location);
        HologramPage page = hologram.getPage(0);
        plugin.getHologramManager().registerHologram(hologram);
        lines.forEach(line -> page.addLine(new HologramLine(page, page.getNextLineLocation(), line)));
        hologram.save();
        convertorResult.addSuccess();
    }
    
    public static void createHologramPages(ConvertorResult convertorResult, String name, Location location, List<List<String>> pages, InteractiveHolograms plugin) {
        if (plugin.getHologramManager().containsHologram(name)) {
            Log.warn("A hologram with name '%s' already exists, skipping...", name);
            convertorResult.addSkipped();
            return;
        }
        
        Hologram hologram = new Hologram(name, location);
        for (int i = 0; i < pages.size(); i++) {
            if (i != 0) {
                hologram.addPage();
            }
            
            HologramPage page = hologram.getPage(i);
            List<String> lines = pages.get(i);
            lines.forEach(line -> page.addLine(new HologramLine(page, page.getNextLineLocation(), line)));
            
            page.addAction(ClickType.LEFT, new Action(ActionType.PREV_PAGE, hologram.getName()));
            page.addAction(ClickType.RIGHT, new Action(ActionType.NEXT_PAGE, hologram.getName()));
        }
        
        plugin.getHologramManager().registerHologram(hologram);
        hologram.save();
        
        convertorResult.addSuccess();
    }
    
    public static boolean notValidFile(final File file, final String fileName) {
        return file == null || !file.exists() || file.isDirectory() || !file.getName().equals(fileName);
    }

}
