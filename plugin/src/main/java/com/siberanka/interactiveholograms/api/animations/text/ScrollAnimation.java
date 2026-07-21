package com.siberanka.interactiveholograms.api.animations.text;

import com.siberanka.interactiveholograms.api.animations.TextAnimation;
import com.siberanka.interactiveholograms.api.utils.color.StripColorUtil;
import lombok.NonNull;

public class ScrollAnimation extends TextAnimation {

    public ScrollAnimation() {
        super("scroll", 3, 0);
    }

    @Override
    public String animate(@NonNull String string, long step, String... args) {
        String specialColors = StripColorUtil.extractSpecialColorsFormatting(string);
        String stripped = StripColorUtil.stripLegacyColorCodes(string);

        int length = stripped.length();
        int size = length / 3 * 2;
        int currentStep = getCurrentStep(step, length);
        int index2 = currentStep + size;
        if (index2 > length) {
            return specialColors + stripped.substring(currentStep) + " " + specialColors + stripped.substring(0, index2 - length);
        }
        return specialColors + stripped.substring(currentStep, index2);
    }
}
