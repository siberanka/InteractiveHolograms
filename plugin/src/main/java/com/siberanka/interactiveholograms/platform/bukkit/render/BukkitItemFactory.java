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

package com.siberanka.interactiveholograms.platform.bukkit.render;

import com.siberanka.interactiveholograms.api.utils.items.DecentMaterial;
import com.siberanka.interactiveholograms.api.utils.items.ItemBuilder;
import com.siberanka.interactiveholograms.platform.api.data.DecentColor;
import com.siberanka.interactiveholograms.platform.api.data.ItemDescriptor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.lang.reflect.Method;

public class BukkitItemFactory {

    public ItemStack createItemStack(ItemDescriptor descriptor) {
        ItemStack customItem = resolveCustomItem(descriptor.getMaterial());
        if (customItem != null) {
            ItemBuilder customBuilder = new ItemBuilder(customItem);
            applyEnchantGlint(descriptor, customBuilder);
            applyCustomModelData(descriptor, customBuilder);
            applySkullTexture(descriptor, customItem.getType(), customBuilder);
            applyLeatherColor(descriptor, customItem.getType(), customBuilder);
            return customBuilder.toItemStack();
        }
        Material material = resolveMaterial(descriptor.getMaterial());
        if (material == null) {
            material = Material.STONE;
        }
        ItemBuilder itemBuilder = new ItemBuilder(material);
        applyEnchantGlint(descriptor, itemBuilder);
        applyCustomModelData(descriptor, itemBuilder);
        applySkullTexture(descriptor, material, itemBuilder);
        applyLeatherColor(descriptor, material, itemBuilder);
        return itemBuilder.toItemStack();
    }

    /**
     * Uses CraftEngine's stable public lookup API without linking it at build
     * time. This keeps CraftEngine optional and preserves plugin class-loader
     * compatibility across supported server versions.
     */
    private static ItemStack resolveCustomItem(String itemId) {
        if (itemId == null || itemId.startsWith("minecraft:") || itemId.indexOf(':') < 1) {
            return null;
        }
        try {
            Class<?> api = Class.forName("net.momirealms.craftengine.bukkit.api.CraftEngineItems");
            Method byId = api.getMethod("byId", String.class);
            Object definition = byId.invoke(null, itemId);
            if (definition == null) {
                return null;
            }
            Object stack = definition.getClass().getMethod("buildBukkitItem").invoke(definition);
            return stack instanceof ItemStack ? ((ItemStack) stack).clone() : null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private static Material resolveMaterial(String materialName) {
        int colonIndex = materialName.indexOf(':');
        if (colonIndex != -1) {
            materialName = materialName.substring(colonIndex + 1);
        }
        return Material.getMaterial(materialName.toUpperCase(Locale.ENGLISH));
    }

    private static void applyEnchantGlint(ItemDescriptor descriptor, ItemBuilder itemBuilder) {
        if (descriptor.isEnchanted()) {
            itemBuilder.withUnsafeEnchantment(Enchantment.values()[0], 1);
        }
    }

    private static void applyCustomModelData(ItemDescriptor descriptor, ItemBuilder itemBuilder) {
        // TODO
    }

    private static void applySkullTexture(ItemDescriptor descriptor, Material material, ItemBuilder itemBuilder) {
        String skullTexture = descriptor.getSkullTexture();
        if (skullTexture != null && DecentMaterial.isSkull(material)) {
            if (skullTexture.length() <= 16) {
                itemBuilder.withSkullOwner(skullTexture);
            } else {
                itemBuilder.withSkullTexture(skullTexture);
            }
        }
    }

    private static void applyLeatherColor(ItemDescriptor descriptor, Material material, ItemBuilder itemBuilder) {
        DecentColor color = descriptor.getLeatherColor();
        if (color != null && DecentMaterial.isLeatherArmor(material)) {
            itemBuilder.withLeatherArmorColor(mapDecentColorToBukkitColor(color));
        }
    }

    private static Color mapDecentColorToBukkitColor(DecentColor decentColor) {
        return Color.fromRGB(decentColor.getRed(), decentColor.getGreen(), decentColor.getBlue());
    }
}
