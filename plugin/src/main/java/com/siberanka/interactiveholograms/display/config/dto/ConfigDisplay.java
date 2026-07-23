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

package com.siberanka.interactiveholograms.display.config.dto;

import com.siberanka.interactiveholograms.platform.api.data.display.DisplayType;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Required;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigSerializable
public class ConfigDisplay {
    @Setting("schema-version")
    private int schemaVersion = 4;
    private transient String name; // Name is extracted from file name.
    @Setting
    @Required
    private DisplayType type;
    @Setting
    @Required
    private ConfigDecentLocation location;
    @Setting
    private ConfigDisplaySettings settings = new ConfigDisplaySettings();
    @Setting
    private Map<String, ConfigAttribute> attributes = new HashMap<>();
    @Setting
    private List<ConfigTextPage> pages;
    @Setting
    private String item;
    @Setting
    private String block;
    @Setting
    private Map<String, List<String>> actions = new HashMap<>();
    @Setting("model_provider")
    private String modelProvider;
    @Setting
    private String model;
    @Setting
    private String animation;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DisplayType getType() {
        return type;
    }

    public void setType(DisplayType type) {
        this.type = type;
    }

    public ConfigDecentLocation getLocation() {
        return location;
    }

    public void setLocation(ConfigDecentLocation location) {
        this.location = location;
    }

    public ConfigDisplaySettings getSettings() {
        return settings;
    }

    public void setSettings(ConfigDisplaySettings settings) {
        this.settings = settings;
    }

    public Map<String, ConfigAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, ConfigAttribute> attributes) {
        this.attributes = attributes;
    }

    public List<ConfigTextPage> getPages() {
        return pages;
    }

    public void setPages(List<ConfigTextPage> pages) {
        this.pages = pages;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getBlock() {
        return block;
    }

    public void setBlock(String block) {
        this.block = block;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public Map<String, List<String>> getActions() {
        return actions;
    }

    public void setActions(Map<String, List<String>> actions) {
        this.actions = actions == null ? new HashMap<>() : actions;
    }

    public String getModelProvider() { return modelProvider; }
    public void setModelProvider(String modelProvider) { this.modelProvider = modelProvider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getAnimation() { return animation; }
    public void setAnimation(String animation) { this.animation = animation; }
}
