package com.siberanka.interactiveholograms.api.utils.scheduler;

import com.siberanka.interactiveholograms.api.InteractiveHolograms;
import com.siberanka.interactiveholograms.api.InteractiveHologramsAPI;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.CompletableFuture;

@UtilityClass
public class S {

    private static final InteractiveHolograms INTERACTIVE_HOLOGRAMS = InteractiveHologramsAPI.get();

    public static void stopTask(int id) {
        Bukkit.getScheduler().cancelTask(id);
    }

    public static void sync(Runnable runnable) {
        Bukkit.getScheduler().runTask(INTERACTIVE_HOLOGRAMS.getPlugin(), runnable);
    }

    public static BukkitTask sync(Runnable runnable, long delay) {
        return Bukkit.getScheduler().runTaskLater(INTERACTIVE_HOLOGRAMS.getPlugin(), runnable, delay);
    }

    public static BukkitTask syncTask(Runnable runnable, long interval) {
        return Bukkit.getScheduler().runTaskTimer(INTERACTIVE_HOLOGRAMS.getPlugin(), runnable, 0, interval);
    }

    public static void async(Runnable runnable) {
        try {
            Bukkit.getScheduler().runTaskAsynchronously(INTERACTIVE_HOLOGRAMS.getPlugin(), runnable);
        } catch (IllegalPluginAccessException e) {
            CompletableFuture.runAsync(runnable);
        }
    }

    public static void async(Runnable runnable, long delay) {
        try {
            Bukkit.getScheduler().runTaskLaterAsynchronously(INTERACTIVE_HOLOGRAMS.getPlugin(), runnable, delay);
        } catch (IllegalPluginAccessException e) {
            CompletableFuture.runAsync(runnable);
        }
    }

    public static BukkitTask asyncTask(Runnable runnable, long interval) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(INTERACTIVE_HOLOGRAMS.getPlugin(), runnable, 0, interval);
    }

    public static BukkitTask asyncTask(Runnable runnable, long interval, long delay) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(INTERACTIVE_HOLOGRAMS.getPlugin(), runnable, delay, interval);
    }

}
