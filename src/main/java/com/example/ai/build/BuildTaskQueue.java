package com.example.ai.build;

import com.example.config.AiBuilderConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayDeque;
import java.util.Queue;

public final class BuildTaskQueue {

    private static final Queue<Runnable> TASKS = new ArrayDeque<>();
    private static int tickCounter = 0;
    private static Runnable emptyCallback;
    private static MinecraftServer server;

    private BuildTaskQueue() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(activeServer -> {
            server = activeServer;
            tick();
        });
    }

    public static void enqueue(Runnable task) {
        if (task == null) {
            return;
        }

        int delay = AiBuilderConfig.get().ticksPerBlock;
        if (delay <= 1) {
            task.run();
            notifyIfIdle();
            return;
        }

        TASKS.add(task);
    }

    public static void clear() {
        TASKS.clear();
        tickCounter = 0;
        emptyCallback = null;
    }

    public static int pendingCount() {
        return TASKS.size();
    }

    public static void whenEmpty(Runnable callback) {
        emptyCallback = callback;
        notifyIfIdle();
    }

    private static void tick() {
        if (TASKS.isEmpty()) {
            tickCounter = 0;
            notifyIfIdle();
            return;
        }

        tickCounter++;
        if (tickCounter < AiBuilderConfig.get().ticksPerBlock) {
            return;
        }

        tickCounter = 0;
        Runnable task = TASKS.poll();
        if (task != null) {
            task.run();
        }

        notifyIfIdle();
    }

    private static void notifyIfIdle() {
        if (!TASKS.isEmpty() || emptyCallback == null || server == null) {
            return;
        }

        Runnable callback = emptyCallback;
        emptyCallback = null;
        server.execute(callback);
    }
}
