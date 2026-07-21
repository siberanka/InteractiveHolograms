package com.siberanka.interactiveholograms.nms.v26_2;

import com.siberanka.interactiveholograms.nms.api.NmsHologramPartData;
import com.siberanka.interactiveholograms.nms.api.renderer.NmsTextHologramRenderer;
import com.siberanka.interactiveholograms.shared.DecentPosition;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

class TextHologramRenderer implements NmsTextHologramRenderer {

    private final int armorStandEntityId;

    TextHologramRenderer(EntityIdGenerator entityIdGenerator) {
        this.armorStandEntityId = entityIdGenerator.getFreeEntityId();
    }

    @Override
    public void display(Player player, NmsHologramPartData<String> data) {
        String content = data.getContent();
        DecentPosition position = data.getPosition();
        EntityPacketsBuilder.create()
                .withSpawnEntity(armorStandEntityId, EntityType.ARMOR_STAND, offsetPosition(position))
                .withEntityMetadata(armorStandEntityId, EntityMetadataBuilder.create()
                        .withInvisible()
                        .withNoGravity()
                        .withArmorStandProperties(true, true)
                        .withCustomName(content)
                        .toWatchableObjects())
                .sendTo(player);
    }

    @Override
    public void updateContent(Player player, NmsHologramPartData<String> data) {
        EntityPacketsBuilder.create()
                .withEntityMetadata(armorStandEntityId, EntityMetadataBuilder.create()
                        .withCustomName(data.getContent())
                        .toWatchableObjects())
                .sendTo(player);
    }

    @Override
    public void move(Player player, NmsHologramPartData<String> data) {
        EntityPacketsBuilder.create()
                .withTeleportEntity(armorStandEntityId, offsetPosition(data.getPosition()))
                .sendTo(player);
    }

    @Override
    public void hide(Player player) {
        EntityPacketsBuilder.create()
                .withRemoveEntity(armorStandEntityId)
                .sendTo(player);
    }

    @Override
    public double getHeight(NmsHologramPartData<String> data) {
        return 0.25d;
    }

    @Override
    public int[] getEntityIds() {
        return new int[]{armorStandEntityId};
    }

    private DecentPosition offsetPosition(DecentPosition position) {
        return position.subtractY(0.5);
    }
}
