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

package com.siberanka.interactiveholograms.display;

/**
 * Settings of a Display.
 *
 * @author d0by
 * @see DisplayBase
 * @since 2.10.0
 */
public class DisplaySettings {

    /**
     * Whether the display is enabled or not.
     *
     * <p>If the display is disabled, it won't be rendered.</p>
     */
    private boolean enabled;
    /**
     * The maximum range in blocks at which the display is visible for a player.
     */
    private double displayRange;
    /**
     * The interval in ticks at which the display is updated for players within the update range.
     */
    private int updateInterval;
    /** Viewer selection mode. */
    private DisplayVisibility visibility;
    /** Optional permission override used by PERMISSION_REQUIRED. */
    private String permission;
    /** Whether this hologram is written to disk. */
    private boolean persistent;
    /** Width and height of the automatically generated packet hitbox. */
    private float hitboxWidth;
    private float hitboxHeight;

    public DisplaySettings() {
        this.enabled = true;
        this.displayRange = 256;
        this.updateInterval = 20;
        this.visibility = DisplayVisibility.ALL;
        this.persistent = true;
        this.hitboxWidth = 1.0f;
        this.hitboxHeight = 1.0f;
    }

    public DisplaySettings copy() {
        DisplaySettings copy = new DisplaySettings();
        copy.setEnabled(this.isEnabled());
        copy.setDisplayRange(this.getDisplayRange());
        copy.setUpdateInterval(this.getUpdateInterval());
        copy.setVisibility(this.getVisibility());
        copy.setPermission(this.getPermission());
        copy.setPersistent(this.isPersistent());
        copy.setHitboxWidth(this.getHitboxWidth());
        copy.setHitboxHeight(this.getHitboxHeight());
        return copy;
    }

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
        this.visibility = visibility == null ? DisplayVisibility.ALL : visibility;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission == null || permission.trim().isEmpty() ? null : permission.trim();
    }

    public boolean isPersistent() {
        return persistent;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    public float getHitboxWidth() {
        return hitboxWidth;
    }

    public void setHitboxWidth(float hitboxWidth) {
        this.hitboxWidth = clampHitbox(hitboxWidth);
    }

    public float getHitboxHeight() {
        return hitboxHeight;
    }

    public void setHitboxHeight(float hitboxHeight) {
        this.hitboxHeight = clampHitbox(hitboxHeight);
    }

    private float clampHitbox(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return 1.0f;
        }
        return Math.max(0.1f, Math.min(16.0f, value));
    }
}
