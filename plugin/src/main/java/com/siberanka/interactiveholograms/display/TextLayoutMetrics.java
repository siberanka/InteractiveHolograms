package com.siberanka.interactiveholograms.display;

public final class TextLayoutMetrics {

    private final double maximumPageHeight;
    private final int maximumVisibleLineLength;
    private final boolean hasActions;
    private final boolean hasPlaceholders;
    private final boolean hasAnimations;
    private final long layoutRevision;

    public TextLayoutMetrics(double maximumPageHeight, int maximumVisibleLineLength,
                             boolean hasActions, boolean hasPlaceholders, boolean hasAnimations,
                             long layoutRevision) {
        this.maximumPageHeight = maximumPageHeight;
        this.maximumVisibleLineLength = maximumVisibleLineLength;
        this.hasActions = hasActions;
        this.hasPlaceholders = hasPlaceholders;
        this.hasAnimations = hasAnimations;
        this.layoutRevision = layoutRevision;
    }

    public double getMaximumPageHeight() {
        return maximumPageHeight;
    }

    public int getMaximumVisibleLineLength() {
        return maximumVisibleLineLength;
    }

    public boolean hasActions() {
        return hasActions;
    }

    public boolean hasPlaceholders() {
        return hasPlaceholders;
    }

    public boolean hasAnimations() {
        return hasAnimations;
    }

    public long getLayoutRevision() {
        return layoutRevision;
    }
}
