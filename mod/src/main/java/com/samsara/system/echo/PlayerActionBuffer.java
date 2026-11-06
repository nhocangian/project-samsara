package com.samsara.system.echo;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Ring buffer storing player activity snapshots across the last 120 seconds (2400 ticks).
 */
public class PlayerActionBuffer {
    public static final int BUFFER_CAPACITY = 2400; // 120 seconds @ 20 tps
    public static final int ECHO_DELAY_TICKS = 1200; // Exactly 60 seconds ago

    private final Deque<ActionSnapshot> history = new ArrayDeque<>(BUFFER_CAPACITY);

    public void record(ServerPlayerEntity player, long currentTick) {
        if (history.size() >= BUFFER_CAPACITY) {
            history.pollFirst();
        }

        history.addLast(new ActionSnapshot(
                player.getPos(),
                player.getYaw(),
                player.getPitch(),
                player.isSneaking(),
                player.handSwinging,
                currentTick
        ));
    }

    /**
     * Retrieves the snapshot closest to 60 seconds (1200 ticks) in the past.
     */
    public ActionSnapshot getSnapshot60SecondsAgo(long currentTick) {
        long targetTick = currentTick - ECHO_DELAY_TICKS;
        ActionSnapshot closest = null;
        long minDiff = Long.MAX_VALUE;

        for (ActionSnapshot snapshot : history) {
            long diff = Math.abs(snapshot.tickTimestamp() - targetTick);
            if (diff < minDiff) {
                minDiff = diff;
                closest = snapshot;
            }
        }

        // Return snapshot only if within reasonable threshold (within 5 seconds of the 60s target)
        if (closest != null && minDiff <= 100) {
            return closest;
        }
        return null;
    }

    public void clear() {
        history.clear();
    }
}
