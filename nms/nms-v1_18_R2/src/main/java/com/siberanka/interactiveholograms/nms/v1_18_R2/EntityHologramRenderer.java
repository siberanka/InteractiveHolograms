package com.siberanka.interactiveholograms.nms.v1_18_R2;

import com.siberanka.interactiveholograms.nms.api.NmsHologramPartData;
import com.siberanka.interactiveholograms.nms.api.renderer.NmsEntityHologramRenderer;
import com.siberanka.interactiveholograms.shared.DecentPosition;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

class EntityHologramRenderer implements NmsEntityHologramRenderer {

    private final int entityId;

    EntityHologramRenderer(EntityIdGenerator entityIdGenerator) {
        this.entityId = entityIdGenerator.getFreeEntityId();
    }

    @Override
    public void display(Player player, NmsHologramPartData<EntityType> data) {
        DecentPosition position = data.getPosition();
        EntityType content = data.getContent();
        EntityPacketsBuilder.create()
                .withSpawnEntityLivingOrObject(entityId, content, offsetPosition(position))
                .withEntityMetadata(entityId, EntityMetadataBuilder.create()
                        .withSilent()
                        .withNoGravity()
                        .toWatchableObjects())
                .sendTo(player);
    }

    @Override
    public void updateContent(Player player, NmsHologramPartData<EntityType> data) {
        hide(player);
        display(player, data);
    }

    @Override
    public void move(Player player, NmsHologramPartData<EntityType> data) {
        hide(player);
        display(player, data);
    }

    @Override
    public void hide(Player player) {
        EntityPacketsBuilder.create()
                .withRemoveEntity(entityId)
                .sendTo(player);
    }

    @Override
    public double getHeight(NmsHologramPartData<EntityType> data) {
        return EntityTypeRegistry.getEntityTypeHeight(data.getContent());
    }

    @Override
    public int[] getEntityIds() {
        return new int[]{entityId};
    }

    private DecentPosition offsetPosition(DecentPosition position) {
        return position.subtractY(0.25d);
    }
}
