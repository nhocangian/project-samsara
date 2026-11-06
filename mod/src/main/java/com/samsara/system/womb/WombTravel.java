package com.samsara.system.womb;

import com.samsara.registry.SamsaraDimensions;
import com.samsara.system.cycle.EggCycleState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Đi vào / ra khỏi The Womb.
 * Mọi lối vào (vết rách dưới hầm nhà Nam, lệnh /samsara womb) đều đưa người chơi tới Bến Đò - đầu tuyến hành hương.
 * Lần đầu tiên tuyến được dựng tại ARRIVAL; sau đó luôn quay về gốc tuyến đã lưu.
 */
public final class WombTravel {
    private WombTravel() {}

    /** Nơi bước vào The Womb lần đầu (gốc tuyến hành hương). */
    public static final BlockPos ARRIVAL = new BlockPos(0, WombRoute.SURFACE_Y, 0);

    /** Chống dịch chuyển lặp liên tục khi đứng trong vết rách. */
    private static final Map<UUID, Long> TELEPORT_COOLDOWN = new HashMap<>();

    public static long getCooldown(UUID uuid) {
        return TELEPORT_COOLDOWN.getOrDefault(uuid, 0L);
    }

    public static void setCooldown(UUID uuid) {
        TELEPORT_COOLDOWN.put(uuid, System.currentTimeMillis());
    }

    public static void teleportToTheWomb(ServerPlayerEntity player) {
        ServerWorld womb = player.server.getWorld(SamsaraDimensions.THE_WOMB_WORLD_KEY);
        if (womb == null) return;
        setCooldown(player.getUuid());

        BlockPos origin = EggCycleState.get(player.server).getRouteOrigin();
        BlockPos dock = origin != null ? origin : ARRIVAL;
        // Đứng trên Bến Đò, nhìn về phía Quả Trứng (bắc)
        player.teleport(womb, dock.getX() + 0.5, WombRoute.SURFACE_Y, dock.getZ() + 0.5, 180.0f, 0.0f);
        WombRouteManager.ensureRoute(womb, dock);
        womb.playSound(null, dock, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundCategory.PLAYERS, 1.5f, 1.2f);
        player.sendMessage(Text.translatable("samsara.womb_travel.bien_guong_the_womb"), true);
    }

    public static void teleportToOverworld(ServerPlayerEntity player) {
        ServerWorld overworld = player.server.getOverworld();
        BlockPos spawn = overworld.getSpawnPos();
        player.teleport(overworld, spawn.getX() + 0.5, spawn.getY() + 1.0, spawn.getZ() + 0.5, player.getYaw(), player.getPitch());
        player.sendMessage(Text.translatable("samsara.womb_travel.dich_chuyen_tro_ve"), true);
    }
}
