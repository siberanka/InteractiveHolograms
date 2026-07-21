package com.siberanka.interactiveholograms.nms;

import com.siberanka.interactiveholograms.api.actions.ClickType;
import com.siberanka.interactiveholograms.api.holograms.HologramManager;
import com.siberanka.interactiveholograms.display.interaction.DisplayInteractionService;
import com.siberanka.interactiveholograms.nms.api.NmsPacketListener;
import com.siberanka.interactiveholograms.nms.api.event.NmsEntityInteractAction;
import com.siberanka.interactiveholograms.nms.api.event.NmsEntityInteractEvent;
import com.siberanka.interactiveholograms.api.InteractiveHologramsAPI;
import org.bukkit.Bukkit;
import java.util.function.Consumer;

/**
 * This packet listener is responsible for handling player interactions with holograms.
 *
 * @author d0by
 * @since 2.9.0
 */
public class InteractiveHologramsNmsPacketListener implements NmsPacketListener {

    private final HologramManager hologramManager;
    private final Consumer<Runnable> mainThreadExecutor;
    private volatile DisplayInteractionService displayInteractionService;

    public InteractiveHologramsNmsPacketListener(HologramManager hologramManager) {
        this(hologramManager, task -> Bukkit.getScheduler().runTask(InteractiveHologramsAPI.get().getPlugin(), task));
    }

    InteractiveHologramsNmsPacketListener(HologramManager hologramManager, Consumer<Runnable> mainThreadExecutor) {
        this.hologramManager = hologramManager;
        this.mainThreadExecutor = mainThreadExecutor;
    }

    public void setDisplayInteractionService(DisplayInteractionService displayInteractionService) {
        this.displayInteractionService = displayInteractionService;
    }

    @Override
    public void onEntityInteract(NmsEntityInteractEvent event) {
        ClickType clickType = mapEntityInteractActionToClickType(event.getAction());
        DisplayInteractionService modern = displayInteractionService;
        boolean processed = modern != null && modern.acceptClick(event.getPlayer(), event.getEntityId(), clickType);
        if (!processed && hologramManager.hasEntity(event.getPlayer(), event.getEntityId())) {
            processed = true;
            mainThreadExecutor.accept(() -> hologramManager.onClick(event.getPlayer(), event.getEntityId(), clickType));
        }
        if (processed) {
            event.setHandled(true);
        }
    }

    private ClickType mapEntityInteractActionToClickType(NmsEntityInteractAction action) {
        if (action != null) {
            switch (action) {
                case LEFT_CLICK:
                    return ClickType.LEFT;
                case RIGHT_CLICK:
                    return ClickType.RIGHT;
                case SHIFT_LEFT_CLICK:
                    return ClickType.SHIFT_LEFT;
                case SHIFT_RIGHT_CLICK:
                    return ClickType.SHIFT_RIGHT;
            }
        }
        throw new IllegalArgumentException("Unknown action: " + action);
    }

}
