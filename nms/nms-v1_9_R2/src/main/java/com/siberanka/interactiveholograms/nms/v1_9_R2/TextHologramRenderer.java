package com.siberanka.interactiveholograms.nms.v1_9_R2;

import com.siberanka.interactiveholograms.nms.api.NmsHologramPartData;
import com.siberanka.interactiveholograms.nms.api.renderer.NmsTextHologramRenderer;
import com.siberanka.interactiveholograms.shared.DecentPosition;
import net.minecraft.server.v1_9_R2.DataWatcher;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

class TextHologramRenderer implements NmsTextHologramRenderer {

    private final int armorStandEntityId;
    private final DataWatcher dataWatcher;

    TextHologramRenderer(EntityIdGenerator entityIdGenerator) {
        this.armorStandEntityId = entityIdGenerator.getFreeEntityId();
        this.dataWatcher = DataWatcherBuilder.create()
                .withInvisible()
                .withArmorStandProperties(true, true)
                .toDataWatcher();
    }

    @Override
    public void display(Player player, NmsHologramPartData<String> data) {
        String content = data.getContent();
        DecentPosition position = data.getPosition();
        EntityPacketsBuilder.create()
                .withSpawnEntityLiving(armorStandEntityId, EntityType.ARMOR_STAND, offsetPosition(position), dataWatcher)
                .withEntityMetadata(armorStandEntityId, EntityMetadataBuilder.create()
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
