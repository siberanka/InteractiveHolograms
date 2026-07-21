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

package com.siberanka.interactiveholograms.platform.bukkit.placeholder;

import com.siberanka.interactiveholograms.api.utils.PAPI;
import com.siberanka.interactiveholograms.platform.api.placeholder.PlaceholderContext;
import com.siberanka.interactiveholograms.platform.api.placeholder.PlaceholderProvider;
import com.siberanka.interactiveholograms.platform.bukkit.player.BukkitPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BukkitPlaceholderApiProvider implements PlaceholderProvider {

    @Override
    public @NotNull String replace(@NotNull String input, @NotNull PlaceholderContext ctx) {
        Player player = ((BukkitPlayer) ctx.getPlayer()).getBukkitPlayer();
        return PAPI.setPlaceholders(player, input);
    }

    @Override
    public boolean containsPlaceholders(@NotNull String input) {
        return PAPI.containsPlaceholders(input);
    }
}
