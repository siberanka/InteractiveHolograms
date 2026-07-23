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
import java.util.Arrays;
import java.util.UUID;
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

    @Test
    void pageActionsEnableHitboxAndNavigatePerViewer() {
        TextDisplay display = new TextDisplay("pages",
                new DecentLocation("world", 0, 64, 0, 0, 0), new DisplaySettings());
        com.siberanka.interactiveholograms.display.TextDisplayPage first =
                new com.siberanka.interactiveholograms.display.TextDisplayPage(
                        Collections.singletonList(new com.siberanka.interactiveholograms.display.TextDisplayLine(
                                "first", 0.5d)),
                        Collections.singletonMap(ClickType.LEFT,
                                Collections.singletonList(new Action("NEXT_PAGE"))));
        com.siberanka.interactiveholograms.display.TextDisplayPage second =
                new com.siberanka.interactiveholograms.display.TextDisplayPage(
                        Collections.singletonList(new com.siberanka.interactiveholograms.display.TextDisplayLine(
                                "second", 0.3d)),
                        Collections.singletonMap(ClickType.LEFT,
                                Collections.singletonList(new Action("PAGE:1"))));
        display.setPages(Arrays.asList(first, second));
        UUID viewerId = UUID.randomUUID();
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(viewerId);

        assertTrue(DisplayInteractionService.requiresPacketHitbox(display));
        assertTrue(DisplayInteractionService.executeActions(display, player, ClickType.LEFT));
        assertTrue(display.getPageIndex(viewerId) == 1);
        assertTrue(DisplayInteractionService.executeActions(display, player, ClickType.LEFT));
        assertTrue(display.getPageIndex(viewerId) == 0);
    }

    @Test
    void textHitboxCoversLongestLineAndTallestPageWithinPacketLimit() {
        TextDisplay display = new TextDisplay("bounds",
                new DecentLocation("world", 0, 64, 0, 0, 0), new DisplaySettings());
        display.setPages(Collections.singletonList(
                new com.siberanka.interactiveholograms.display.TextDisplayPage(Arrays.asList(
                        new com.siberanka.interactiveholograms.display.TextDisplayLine(
                                "This line is deliberately much wider than one block", 0.5d),
                        new com.siberanka.interactiveholograms.display.TextDisplayLine("second", 2.0d)
                ), Collections.emptyMap())));

        DisplayInteractionService.HitboxLayout layout =
                DisplayInteractionService.HitboxLayout.forDisplay(display);

        assertTrue(layout.getWidth() >= 4.0d);
        assertTrue(layout.getHeight() >= 2.5d);
        assertTrue(layout.size() > 1);
        assertTrue(layout.size() <= 64);
    }
}
