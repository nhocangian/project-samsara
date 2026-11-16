package com.samsara.system.overworld;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

/** Tiện ích chung cho các hiện tượng dưới hang động Overworld. */
final class Caves {
    private Caves() {}

    /** Đang ở dưới lòng đất Overworld: dưới Y 50 và không thấy trời. */
    static boolean underground(ServerPlayerEntity player) {
        if (player.isSpectator() || player.getWorld().getRegistryKey() != World.OVERWORLD) return false;
        return player.getY() < 50 && !player.getWorld().isSkyVisible(player.getBlockPos().up());
    }
}
