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

package com.siberanka.interactiveholograms.display.config;

import com.siberanka.interactiveholograms.display.BlockDisplay;
import com.siberanka.interactiveholograms.display.DisplayBase;
import com.siberanka.interactiveholograms.display.DisplaySettings;
import com.siberanka.interactiveholograms.display.ItemDisplay;
import com.siberanka.interactiveholograms.display.TextDisplay;
import com.siberanka.interactiveholograms.display.TextDisplayLine;
import com.siberanka.interactiveholograms.display.TextDisplayPage;
import com.siberanka.interactiveholograms.display.attribute.AttributeConfigMapper;
import com.siberanka.interactiveholograms.display.attribute.DisplayAttribute;
import com.siberanka.interactiveholograms.display.attribute.value.AttributeValue;
import com.siberanka.interactiveholograms.display.config.dto.ConfigAttribute;
import com.siberanka.interactiveholograms.display.config.dto.ConfigDecentLocation;
import com.siberanka.interactiveholograms.display.config.dto.ConfigDisplay;
import com.siberanka.interactiveholograms.display.config.dto.ConfigDisplaySettings;
import com.siberanka.interactiveholograms.display.config.dto.ConfigTextPage;
import com.siberanka.interactiveholograms.display.config.dto.ConfigTextLine;
import com.siberanka.interactiveholograms.display.config.dto.ConfigHitbox;
import com.siberanka.interactiveholograms.api.actions.Action;
import com.siberanka.interactiveholograms.api.actions.ClickType;
import com.siberanka.interactiveholograms.api.utils.Log;
import com.siberanka.interactiveholograms.platform.api.capability.PlatformMaterialService;
import com.siberanka.interactiveholograms.platform.api.data.DecentLocation;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Objects;
import java.util.stream.Collectors;

public class DisplayConfigMapper {

    private final AttributeConfigMapper attributeConfigMapper;
    private final PlatformMaterialService materialService;

    public DisplayConfigMapper(AttributeConfigMapper attributeConfigMapper, PlatformMaterialService materialService) {
        this.attributeConfigMapper = attributeConfigMapper;
        this.materialService = materialService;
    }

    public DisplayBase toDomain(ConfigDisplay dto) {
        DecentLocation location = locationToDomain(dto.getLocation());
        DisplaySettings settings = settingsToDomain(dto.getSettings());
        DisplayBase display;
        switch (dto.getType()) {
            case TEXT:
                display = textDisplayToDomain(dto, location, settings);
                break;
            case ITEM:
                display = itemDisplayToDomain(dto, location, settings);
                break;
            case BLOCK:
                display = blockDisplayToDomain(dto, location, settings);
                break;
            default:
                throw new DisplayConfigException("Unknown display type: " + dto.getType());
        }
        attributeConfigMapper.attributesToDomain(display, dto.getAttributes());
        display.setActions(actionsToDomain(dto.getActions(), dto.getName()));
        display.setModelProvider(dto.getModelProvider());
        display.setModel(dto.getModel());
        display.setAnimation(dto.getAnimation());
        return display;
    }

    private BlockDisplay blockDisplayToDomain(ConfigDisplay dto, DecentLocation location, DisplaySettings settings) {
        String blockMaterial = dto.getBlock();
        if (blockMaterial == null) {
            throw new DisplayConfigException("Block display must have a material");
        }
        if (!materialService.isBlock(blockMaterial)) {
            throw new DisplayConfigException("Found invalid block type '" + blockMaterial + "'.");
        }
        BlockDisplay blockDisplay = new BlockDisplay(dto.getName(), location, settings);
        blockDisplay.setMaterial(blockMaterial);
        return blockDisplay;
    }

    private ItemDisplay itemDisplayToDomain(ConfigDisplay dto, DecentLocation location, DisplaySettings settings) {
        String itemMaterial = dto.getItem();
        if (itemMaterial == null) {
            throw new DisplayConfigException("Item display must have an item");
        }
        if (!materialService.isItem(itemMaterial)) {
            throw new DisplayConfigException("Found invalid item type '" + itemMaterial + "'.");
        }
        ItemDisplay itemDisplay = new ItemDisplay(dto.getName(), location, settings);
        itemDisplay.setMaterial(itemMaterial);
        return itemDisplay;
    }

    private TextDisplay textDisplayToDomain(ConfigDisplay dto, DecentLocation location, DisplaySettings settings) {
        List<ConfigTextPage> pages = dto.getPages();
        if (pages == null || pages.isEmpty()) {
            throw new DisplayConfigException("Text display must have at least one page");
        }
        TextDisplay textDisplay = new TextDisplay(dto.getName(), location, settings);
        List<TextDisplayPage> domainPages = new ArrayList<>();
        for (ConfigTextPage page : pages) {
            if (page == null || page.getLines() == null || page.getLines().isEmpty()) {
                continue;
            }
            List<TextDisplayLine> lines = page.getLines().stream()
                    .filter(Objects::nonNull)
                    .map(line -> new TextDisplayLine(line.getContent(), line.getHeight()))
                    .collect(Collectors.toList());
            if (!lines.isEmpty()) {
                domainPages.add(new TextDisplayPage(lines, actionsToDomain(page.getActions(), dto.getName())));
            }
        }
        if (domainPages.isEmpty()) {
            throw new DisplayConfigException("Text display must have at least one non-empty page");
        }
        textDisplay.setPages(domainPages);
        return textDisplay;
    }

    private DecentLocation locationToDomain(ConfigDecentLocation dto) {
        return new DecentLocation(dto.getWorld(), dto.getX(), dto.getY(), dto.getZ(), dto.getYaw(), dto.getPitch());
    }

    private DisplaySettings settingsToDomain(ConfigDisplaySettings dto) {
        if (dto == null) {
            return new DisplaySettings();
        }
        DisplaySettings settings = new DisplaySettings();
        settings.setEnabled(dto.isEnabled());
        settings.setDisplayRange(dto.getDisplayRange());
        settings.setUpdateInterval(dto.getUpdateInterval());
        settings.setVisibility(dto.getVisibility());
        settings.setPermission(dto.getPermission());
        settings.setPersistent(dto.isPersistent());
        ConfigHitbox hitbox = dto.getHitbox();
        if (hitbox != null) {
            settings.setHitboxWidth(hitbox.getWidth());
            settings.setHitboxHeight(hitbox.getHeight());
        }
        return settings;
    }

    public ConfigDisplay toDto(DisplayBase domain) {
        ConfigDisplay dto = new ConfigDisplay();
        dto.setName(domain.getName());
        dto.setType(domain.getType());
        dto.setLocation(locationToDto(domain.getLocation()));
        dto.setSettings(settingsToDto(domain.getSettings()));
        dto.setAttributes(attributesToDto(domain.getAttributes()));
        dto.setActions(actionsToDto(domain));
        dto.setModelProvider(domain.getModelProvider());
        dto.setModel(domain.getModel());
        dto.setAnimation(domain.getAnimation());
        switch (domain.getType()) {
            case TEXT:
                dto.setPages(pagesToDto((TextDisplay) domain));
                break;
            case ITEM:
                dto.setItem(((ItemDisplay) domain).getMaterial());
                break;
            case BLOCK:
                dto.setBlock(((BlockDisplay) domain).getMaterial());
                break;
        }
        return dto;
    }

    private List<ConfigTextPage> pagesToDto(TextDisplay domain) {
        return domain.getPages().stream().map(this::pageToDto).collect(Collectors.toList());
    }

    private ConfigTextPage pageToDto(TextDisplayPage page) {
        ConfigTextPage pageDto = new ConfigTextPage();
        pageDto.setLines(page.getLines().stream().map(this::lineToDto).collect(Collectors.toList()));
        pageDto.setActions(actionsToDto(page.getActions()));
        return pageDto;
    }

    private ConfigTextLine lineToDto(TextDisplayLine line) {
        ConfigTextLine dto = new ConfigTextLine();
        dto.setContent(line.getContent());
        dto.setHeight(line.getHeight());
        return dto;
    }

    private ConfigDecentLocation locationToDto(DecentLocation location) {
        ConfigDecentLocation dto = new ConfigDecentLocation();
        dto.setWorld(location.getWorldName());
        dto.setX(location.getX());
        dto.setY(location.getY());
        dto.setZ(location.getZ());
        dto.setYaw(location.getYaw());
        dto.setPitch(location.getPitch());
        return dto;
    }

    private ConfigDisplaySettings settingsToDto(DisplaySettings settings) {
        ConfigDisplaySettings dto = new ConfigDisplaySettings();
        dto.setEnabled(settings.isEnabled());
        dto.setDisplayRange(settings.getDisplayRange());
        dto.setUpdateInterval(settings.getUpdateInterval());
        dto.setVisibility(settings.getVisibility());
        dto.setPermission(settings.getPermission());
        dto.setPersistent(settings.isPersistent());
        ConfigHitbox hitbox = new ConfigHitbox();
        hitbox.setWidth(settings.getHitboxWidth());
        hitbox.setHeight(settings.getHitboxHeight());
        dto.setHitbox(hitbox);
        return dto;
    }

    private Map<ClickType, List<Action>> actionsToDomain(Map<String, List<String>> configured, String displayName) {
        Map<ClickType, List<Action>> result = new EnumMap<>(ClickType.class);
        if (configured == null) {
            return result;
        }
        configured.forEach((key, values) -> {
            ClickType clickType = ClickType.fromString(key);
            if (clickType == null || values == null) {
                Log.warn("Ignoring invalid click action group '%s' in hologram '%s'.", key, displayName);
                return;
            }
            List<Action> actions = new ArrayList<>();
            for (String value : values) {
                try {
                    actions.add(new Action(value));
                } catch (IllegalArgumentException exception) {
                    Log.warn("Ignoring invalid action '%s' in hologram '%s'.", value, displayName);
                }
            }
            if (!actions.isEmpty()) {
                result.put(clickType, actions);
            }
        });
        return result;
    }

    private Map<String, List<String>> actionsToDto(DisplayBase domain) {
        return actionsToDto(domain.getActions());
    }

    private Map<String, List<String>> actionsToDto(Map<ClickType, List<Action>> configured) {
        Map<String, List<String>> result = new java.util.LinkedHashMap<>();
        configured.forEach((clickType, actions) -> result.put(
                clickType.name(), actions.stream().map(Action::toString).collect(Collectors.toList())
        ));
        return result;
    }

    private Map<String, ConfigAttribute> attributesToDto(Collection<DisplayAttribute<?>> attributes) {
        return attributes.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        attribute -> attribute.getKey().getName(),
                        this::attributeToDto
                ));
    }

    private ConfigAttribute attributeToDto(DisplayAttribute<?> attribute) {
        ConfigAttribute dto = new ConfigAttribute();
        AttributeValue<?> attributeValue = attribute.getValue();
        if (attributeValue != null) {
            dto.setValueType(attributeValue.getTypeKey());
        }
        dto.setValue(attributeValue);
        return dto;
    }
}
