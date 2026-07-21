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

import com.siberanka.interactiveholograms.display.DisplayVisibility;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public class ConfigDisplaySettings {
    @Setting
    private boolean enabled = true;
    @Setting("display-range")
    private double displayRange = 256;
    @Setting("update-interval")
    private int updateInterval = 20;
    @Setting
    private DisplayVisibility visibility = DisplayVisibility.ALL;
    @Setting
    private String permission;
    @Setting
    private boolean persistent = true;
    @Setting
    private ConfigHitbox hitbox = new ConfigHitbox();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public double getDisplayRange() {
        return displayRange;
    }

    public void setDisplayRange(double displayRange) {
        this.displayRange = displayRange;
    }

    public int getUpdateInterval() {
        return updateInterval;
    }

    public void setUpdateInterval(int updateInterval) {
        this.updateInterval = updateInterval;
    }

    public DisplayVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(DisplayVisibility visibility) {
        this.visibility = visibility;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    public ConfigHitbox getHitbox() {
        return hitbox;
    }

    public void setHitbox(ConfigHitbox hitbox) {
        this.hitbox = hitbox;
    }
}
