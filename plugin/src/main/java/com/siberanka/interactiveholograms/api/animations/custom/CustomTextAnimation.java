package com.siberanka.interactiveholograms.api.animations.custom;

import com.siberanka.interactiveholograms.api.InteractiveHolograms;
import com.siberanka.interactiveholograms.api.InteractiveHologramsAPI;
import com.siberanka.interactiveholograms.api.animations.TextAnimation;
import com.siberanka.interactiveholograms.api.utils.config.FileConfig;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;

public class CustomTextAnimation extends TextAnimation {

	private static final InteractiveHolograms INTERACTIVE_HOLOGRAMS = InteractiveHologramsAPI.get();
	private final List<String> steps;

	public CustomTextAnimation(@NonNull String name, int speed, int pause, @NonNull List<String> steps) {
		super(name, speed, pause);
		this.steps = steps;
	}

	public CustomTextAnimation(@NonNull String name, int speed, int pause, @NonNull List<String> steps, String... aliases) {
		super(name, speed, pause, aliases);
		this.steps = steps;
	}

	@Override
	public String animate(@NonNull String string, long step, String... args) {
		int currentStepIndex = getCurrentStep(step, steps.size() - 1);
		String currentStep = steps.get(currentStepIndex);
		currentStep = currentStep.replace("{text}", string);
		return currentStep;
	}

	public static CustomTextAnimation fromFile(@NonNull String fileName) {
		FileConfig config = new FileConfig(INTERACTIVE_HOLOGRAMS.getPlugin(), "animations/" + fileName);

		// Parse animation name
		String name;
		if (fileName.toLowerCase().startsWith("animation_") && fileName.length() > "animation_".length()) {
			name = fileName.substring("animation_".length(), fileName.length() - 4);
		} else {
			name = fileName.substring(0, fileName.length() - 4);
		}

		int speed = config.isInt("speed") ? config.getInt("speed") : 1;
		int pause = config.isInt("pause") ? config.getInt("pause") : 1;
		List<String> steps = config.isList("steps") ? config.getStringList("steps") : new ArrayList<>();
		return new CustomTextAnimation(name, speed, pause, steps);
	}

}
