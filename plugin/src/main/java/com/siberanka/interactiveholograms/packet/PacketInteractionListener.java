package com.siberanka.interactiveholograms.packet;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.siberanka.interactiveholograms.api.actions.ClickType;
import com.siberanka.interactiveholograms.display.interaction.DisplayInteractionService;
import org.bukkit.entity.Player;

public class PacketInteractionListener extends PacketListenerAbstract {

    private final DisplayInteractionService interactionService;

    public PacketInteractionListener(DisplayInteractionService interactionService) {
        super(PacketListenerPriority.HIGH);
        this.interactionService = interactionService;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            try {
                WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
                if (wrapper.getHand() == InteractionHand.OFF_HAND) {
                    return;
                }
                int entityId = wrapper.getEntityId();
                Object playerObj = event.getPlayer();
                if (playerObj instanceof Player) {
                    Player player = (Player) playerObj;
                    ClickType clickType = mapInteractAction(wrapper.getAction(), player.isSneaking());
                    if (clickType != null && interactionService != null) {
                        boolean handled = interactionService.acceptClick(player, entityId, clickType);
                        if (handled) {
                            event.setCancelled(true);
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private ClickType mapInteractAction(WrapperPlayClientInteractEntity.InteractAction action, boolean sneaking) {
        if (action == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
            return sneaking ? ClickType.SHIFT_LEFT : ClickType.LEFT;
        } else if (action == WrapperPlayClientInteractEntity.InteractAction.INTERACT ||
                   action == WrapperPlayClientInteractEntity.InteractAction.INTERACT_AT) {
            return sneaking ? ClickType.SHIFT_RIGHT : ClickType.RIGHT;
        }
        return null;
    }
}
