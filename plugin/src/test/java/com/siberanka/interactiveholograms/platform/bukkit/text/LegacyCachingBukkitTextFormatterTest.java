package com.siberanka.interactiveholograms.platform.bukkit.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyCachingBukkitTextFormatterTest {

    private final LegacyCachingBukkitTextFormatter formatter = new LegacyCachingBukkitTextFormatter(32);

    @Test
    void rendersMiniMessageNamedColorsWithoutLeakingTags() {
        String result = formatter.format("<aqua>NoxAves</aqua><gray>!");

        assertTrue(result.contains("\u00a7bNoxAves"));
        assertTrue(result.contains("\u00a77!"));
        assertFalse(result.contains("<aqua>"));
    }

    @Test
    void supportsMixedMiniMessageLegacySectionAndRgbFormats() {
        String result = formatter.format("<red>Mini</red> &aAmp \u00a7bSection &#12AB34Hex");

        assertTrue(result.contains("\u00a7cMini"), result.replace('\u00a7', '&'));
        assertTrue(result.contains("\u00a7aAmp"));
        assertTrue(result.contains("\u00a7bSection"));
        assertTrue(result.contains("\u00a7x\u00a71\u00a72\u00a7A\u00a7B\u00a73\u00a74Hex"));
    }

    @Test
    void supportsMiniMessageGradientRainbowAndDecorations() {
        String result = formatter.format("<gradient:#ff0000:#0000ff>Gradient</gradient> <rainbow>Rainbow</rainbow> <bold>Bold</bold>");

        assertFalse(result.contains("<gradient"));
        assertFalse(result.contains("<rainbow"));
        assertTrue(result.contains("\u00a7x"));
        assertTrue(result.contains("\u00a7lBold"));
    }

    @Test
    void retainsHistoricalGradientSyntax() {
        String result = formatter.format("<#ff0000>Old</#0000ff>");

        assertFalse(result.contains("<#"));
        assertTrue(result.contains("\u00a7x"));
        assertTrue(result.endsWith("Old") || result.endsWith("d"));
    }

    @Test
    void convertsAnsiConsoleColorsIncludingPaletteAndTrueColor() {
        String result = formatter.format("\u001B[31mRed\u001B[0m \u001B[38;5;196mPalette \u001B[38;2;18;171;52mRGB\u001B[0m");

        assertTrue(result.contains("\u00a74Red"));
        assertTrue(result.contains("\u00a7r"));
        assertTrue(result.contains("\u00a7x\u00a7F\u00a7F\u00a70\u00a70\u00a70\u00a70Palette"));
        assertTrue(result.contains("\u00a7x\u00a71\u00a72\u00a7A\u00a7B\u00a73\u00a74RGB"));
        assertFalse(result.contains("\u001B["));
    }

    @Test
    void malformedOrExcessiveTagsRemainBoundedAndDoNotThrow() {
        StringBuilder input = new StringBuilder("<not-a-real-tag>visible");
        for (int i = 0; i < 600; i++) input.append('<');

        String result = formatter.format(input.toString());

        assertTrue(result.contains("visible"));
    }
}
