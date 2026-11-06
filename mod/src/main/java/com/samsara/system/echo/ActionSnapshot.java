package com.samsara.system.echo;

import net.minecraft.util.math.Vec3d;

/**
 * Snapshot of a player's spatial state at a specific server game tick.
 */
public record ActionSnapshot(
        Vec3d position,
        float yaw,
        float pitch,
        boolean isCrouching,
        boolean isSwinging,
        long tickTimestamp
) {}
