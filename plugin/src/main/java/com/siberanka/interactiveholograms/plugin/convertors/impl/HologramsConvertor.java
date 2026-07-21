package com.siberanka.interactiveholograms.plugin.convertors.impl;

import com.siberanka.interactiveholograms.api.InteractiveHolograms;
import com.siberanka.interactiveholograms.api.InteractiveHologramsAPI;
import com.siberanka.interactiveholograms.api.convertor.IConvertor;
import com.siberanka.interactiveholograms.api.utils.Log;
import com.siberanka.interactiveholograms.api.utils.config.FileConfig;
import com.siberanka.interactiveholograms.api.utils.location.LocationUtils;
import com.siberanka.interactiveholograms.plugin.convertors.ConverterCommon;
import com.siberanka.interactiveholograms.plugin.convertors.ConvertorResult;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class HologramsConvertor implements IConvertor {

    private static final InteractiveHolograms PLUGIN = InteractiveHologramsAPI.get();

    @Override
    public ConvertorResult convert() {
        return convert(new File(PLUGIN.getDataFolder().getParent() + "/Holograms/", "holograms.yml"));
    }

    @Override
    public ConvertorResult convert(final File file) {
        Log.info("Converting Holograms holograms...");
        if (ConverterCommon.notValidFile(file, "holograms.yml")) {
            Log.warn("Invalid file! Need 'holograms.yml'");
            return ConvertorResult.createFailed();
        }

        FileConfig config = new FileConfig(PLUGIN.getPlugin(), file);
        ConvertorResult convertorResult = new ConvertorResult();

        ConfigurationSection hologramsSection = config.getConfigurationSection("holograms");
        if (hologramsSection != null) {
            for (String id : hologramsSection.getKeys(false)) {
                Location location = LocationUtils.asLocation(hologramsSection.getString(id + ".location"), ";");
                if (location == null) {
                    Log.warn("Cannot convert '%s'! Invalid location.", id);
                    convertorResult.addFailed();
                    continue;
                }

                List<String> lines = prepareLines(hologramsSection.getStringList(id + ".lines"));
                ConverterCommon.createHologram(convertorResult, id, location, lines, PLUGIN);
            }
        }
        return convertorResult;
    }

    @Override
    public List<String> prepareLines(List<String> lines) {
        return lines.stream().map(line -> {
            if (line.toUpperCase().startsWith("ITEM:")) {
                return "#ICON" + line.substring(4);
            }
            return line;
        }).collect(Collectors.toList());
    }

}
