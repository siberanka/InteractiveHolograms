package com.siberanka.interactiveholograms.display.config.dto;

import com.siberanka.interactiveholograms.display.TextDisplayLine;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Required;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public class ConfigTextLine {

    @Setting
    @Required
    private String content = "";

    @Setting
    private double height = TextDisplayLine.DEFAULT_HEIGHT;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }
}
