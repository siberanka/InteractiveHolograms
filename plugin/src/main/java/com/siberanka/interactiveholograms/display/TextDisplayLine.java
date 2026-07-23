package com.siberanka.interactiveholograms.display;

import java.util.Objects;

/** One text page row and its vertical spacing, measured in blocks. */
public final class TextDisplayLine {

    public static final double DEFAULT_HEIGHT = 0.3d;

    private String content;
    private double height;

    public TextDisplayLine(String content, double height) {
        this.content = Objects.requireNonNull(content, "content cannot be null");
        this.height = normalizeHeight(height);
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = Objects.requireNonNull(content, "content cannot be null");
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = normalizeHeight(height);
    }

    public TextDisplayLine copy() {
        return new TextDisplayLine(content, height);
    }

    private static double normalizeHeight(double value) {
        return Double.isFinite(value) ? Math.max(0.01d, Math.min(16.0d, value)) : DEFAULT_HEIGHT;
    }
}
