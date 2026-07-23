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

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Required;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigSerializable
public class ConfigTextPage {
    @Setting
    @Required
    private List<ConfigTextLine> lines = new ArrayList<>();

    @Setting
    private Map<String, List<String>> actions = new HashMap<>();

    public List<ConfigTextLine> getLines() {
        return lines;
    }

    public void setLines(List<ConfigTextLine> lines) {
        this.lines = lines;
    }

    public Map<String, List<String>> getActions() {
        return actions;
    }

    public void setActions(Map<String, List<String>> actions) {
        this.actions = actions == null ? new HashMap<>() : actions;
    }
}
