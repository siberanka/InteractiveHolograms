package com.siberanka.interactiveholograms.display.interaction;

import com.siberanka.interactiveholograms.api.actions.Action;
import com.siberanka.interactiveholograms.api.actions.ClickType;
import com.siberanka.interactiveholograms.display.BlockDisplay;
import com.siberanka.interactiveholograms.display.DisplayBase;
import com.siberanka.interactiveholograms.display.DisplaySettings;
import com.siberanka.interactiveholograms.display.ItemDisplay;
import com.siberanka.interactiveholograms.display.TextDisplay;
import com.siberanka.interactiveholograms.platform.api.data.DecentLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DisplayInteractionServiceTest {
    private static Stream<DisplayBase> packetVisualKinds() {
        DecentLocation location = new DecentLocation("world", 0, 64, 0, 0, 0);
        TextDisplay text = new TextDisplay("text", location, new DisplaySettings());
        ItemDisplay item = new ItemDisplay("item", location, new DisplaySettings());
        BlockDisplay block = new BlockDisplay("block", location, new DisplaySettings());
        ItemDisplay betterModel = new ItemDisplay("bettermodel", location, new DisplaySettings());
        betterModel.setModelProvider("BETTERMODEL"); betterModel.setModel("knight");
        ItemDisplay mythicMob = new ItemDisplay("mythicmob", location, new DisplaySettings());
        mythicMob.setModelProvider("MYTHICMOBS"); mythicMob.setModel("SkeletalKnight");
        ItemDisplay modelEngine = new ItemDisplay("modelengine", location, new DisplaySettings());
        modelEngine.setModelProvider("MODELENGINE"); modelEngine.setModel("dragon");
        return Stream.of(text, item, block, betterModel, mythicMob, modelEngine);
    }

    @ParameterizedTest(name = "packet click hitbox: {0}")
    @MethodSource("packetVisualKinds")
    void actionsEnableHitboxForEveryVisualKind(DisplayBase display) {
        Action action = mock(Action.class);
        display.setActions(Collections.singletonMap(ClickType.RIGHT,
                Collections.singletonList(action)));
        assertTrue(DisplayInteractionService.requiresPacketHitbox(display));
        Player player = mock(Player.class);
        when(action.execute(player)).thenReturn(true);
        DisplayInteractionService.executeActions(display, player, ClickType.RIGHT);
        verify(action).execute(player);
    }

    @Test
    void disabledOrActionlessDisplayDoesNotCreateHitbox() {
        DisplaySettings settings = new DisplaySettings();
        TextDisplay display = new TextDisplay("text", new DecentLocation("world", 0, 64, 0, 0, 0), settings);
        assertFalse(DisplayInteractionService.requiresPacketHitbox(display));
        display.setActions(Collections.singletonMap(ClickType.LEFT,
                Collections.singletonList(mock(Action.class))));
        settings.setEnabled(false);
        assertFalse(DisplayInteractionService.requiresPacketHitbox(display));
    }
}
