package com.siberanka.interactiveholograms.nms;

import com.siberanka.interactiveholograms.api.actions.ClickType;
import com.siberanka.interactiveholograms.api.holograms.HologramManager;
import com.siberanka.interactiveholograms.display.interaction.DisplayInteractionService;
import com.siberanka.interactiveholograms.nms.api.event.NmsEntityInteractAction;
import com.siberanka.interactiveholograms.nms.api.event.NmsEntityInteractEvent;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InteractiveHologramsNmsPacketListenerTest {

    @Mock
    private HologramManager hologramManager;
    private InteractiveHologramsNmsPacketListener packetListener;

    @BeforeEach
    void setUp() {
        packetListener = new InteractiveHologramsNmsPacketListener(hologramManager, Runnable::run);
    }

    private static Stream<Arguments> providerInteractionActionsWithRespectiveClickTypes() {
        return Stream.of(
                Arguments.arguments(NmsEntityInteractAction.LEFT_CLICK, ClickType.LEFT),
                Arguments.arguments(NmsEntityInteractAction.RIGHT_CLICK, ClickType.RIGHT),
                Arguments.arguments(NmsEntityInteractAction.SHIFT_LEFT_CLICK, ClickType.SHIFT_LEFT),
                Arguments.arguments(NmsEntityInteractAction.SHIFT_RIGHT_CLICK, ClickType.SHIFT_RIGHT)
        );
    }

    @Test
    void testOnEntityInteract_invalidInteractionAction() {
        Player player = mock(Player.class);
        NmsEntityInteractEvent event = new NmsEntityInteractEvent(player, 1, null);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> packetListener.onEntityInteract(event));

        assertEquals("Unknown action: null", exception.getMessage());
        verify(hologramManager, never()).onClick(any(), anyInt(), any());
    }

    @ParameterizedTest
    @MethodSource("providerInteractionActionsWithRespectiveClickTypes")
    void testOnEntityInteract_mapping(NmsEntityInteractAction action, ClickType clickType) {
        Player player = mock(Player.class);
        NmsEntityInteractEvent event = new NmsEntityInteractEvent(player, 1, action);

        when(hologramManager.hasEntity(player, 1)).thenReturn(true);

        packetListener.onEntityInteract(event);

        assertTrue(event.isHandled());
        verify(hologramManager).onClick(player, 1, clickType);
    }

    @Test
    void testOnEntityInteract_eventHandled() {
        Player player = mock(Player.class);
        NmsEntityInteractEvent event = new NmsEntityInteractEvent(player, 1, NmsEntityInteractAction.LEFT_CLICK);

        when(hologramManager.hasEntity(player, 1)).thenReturn(true);
        when(hologramManager.onClick(any(), anyInt(), any())).thenReturn(true);

        packetListener.onEntityInteract(event);

        assertTrue(event.isHandled());
        verify(hologramManager).onClick(player, 1, ClickType.LEFT);
    }

    @ParameterizedTest
    @MethodSource("providerInteractionActionsWithRespectiveClickTypes")
    void packetDisplayHitboxHandlesEveryClickWithoutLegacyEntityLookup(NmsEntityInteractAction action, ClickType clickType) {
        Player player = mock(Player.class);
        DisplayInteractionService modern = mock(DisplayInteractionService.class);
        packetListener.setDisplayInteractionService(modern);
        when(modern.acceptClick(player, 42, clickType)).thenReturn(true);
        NmsEntityInteractEvent event = new NmsEntityInteractEvent(player, 42, action);

        packetListener.onEntityInteract(event);

        assertTrue(event.isHandled());
        verify(modern).acceptClick(player, 42, clickType);
        verify(hologramManager, never()).hasEntity(any(), anyInt());
        verify(hologramManager, never()).onClick(any(), anyInt(), any());
    }

}
