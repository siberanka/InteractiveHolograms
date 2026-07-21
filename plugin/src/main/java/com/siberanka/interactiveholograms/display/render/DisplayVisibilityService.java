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

package com.siberanka.interactiveholograms.display.render;

import com.siberanka.interactiveholograms.display.DisplayBase;
import com.siberanka.interactiveholograms.display.DisplayVisibility;
import com.siberanka.interactiveholograms.platform.api.data.DecentLocation;
import com.siberanka.interactiveholograms.platform.api.player.PlatformPlayer;

public class DisplayVisibilityService {

    public boolean shouldBeShownToPlayer(DisplayBase display, PlatformPlayer player) {
        return isDisplayEnabled(display)
                && isVisibleToPlayer(display, player)
                && isPlayerWithinDisplayRange(display, player);
    }

    private boolean isVisibleToPlayer(DisplayBase display, PlatformPlayer player) {
        DisplayVisibility visibility = display.getSettings().getVisibility();
        if (visibility == DisplayVisibility.MANUAL) {
            return display.isManualViewer(player.getUniqueId());
        }
        if (visibility == DisplayVisibility.PERMISSION_REQUIRED) {
            String permission = display.getSettings().getPermission();
            if (permission == null) {
                permission = "interactiveholograms.hologram." + display.getName() + ".view";
            }
            return player.hasPermission(permission);
        }
        return true;
    }

    private boolean isDisplayEnabled(DisplayBase display) {
        return display.getSettings().isEnabled();
    }

    private boolean isPlayerWithinDisplayRange(DisplayBase display, PlatformPlayer player) {
        double displayRange = display.getSettings().getDisplayRange();
        DecentLocation displayLocation = display.getLocation();
        DecentLocation playerLocation = player.getLocation();
        return displayLocation.isSameWorld(playerLocation)
                && displayLocation.distanceSquared(playerLocation) <= displayRange * displayRange;
    }
}
