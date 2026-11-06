package com.samsara.system.cycle;

import com.samsara.registry.SamsaraSounds;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stage 3+ (kịch bản Video 6, cảnh 03 "Âm thanh đi ngược"): khi người chơi đứng yên ở Overworld,
 * thỉnh thoảng MỘT âm rất quen (mở rương, cửa gỗ, bước chân trên cỏ, nhặt đồ...) vang lên sau lưng
 * nhưng phát ngược thật, trong khi mọi âm khác vẫn bình thường.
 *
 * Chỉ người đứng yên nghe thấy (gói gửi riêng). Âm tua ngược thật nhờ {@code ReversedSounds} phía client.
 * Lệnh test: {@code /samsara test anomaly reverse}.
 */
public final class ReversedEchoSystem {
    /** Phải đứng yên bao nhiêu giây thì âm mới có thể xuất hiện. */
    private static final int STILL_SECONDS = 3;
    /** Xác suất mỗi giây đứng yên (sau ngưỡng trên): trung bình ~25 giây đứng yên mới nghe một lần. */
    private static final float CHANCE_PER_SECOND = 1.0f / 25.0f;
    /** Khoảng nghỉ tối thiểu giữa hai lần cho một người chơi (tick). */
    private static final int COOLDOWN_TICKS = 20 * 150;
    private static final double STILL_EPSILON_SQ = 0.15 * 0.15;

    private static final Map<UUID, Tracker> TRACKERS = new HashMap<>();

    private static final class Tracker {
        Vec3d lastPos = Vec3d.ZERO;
        int stillSeconds = 0;
        long nextAllowed = 0;
    }

    private ReversedEchoSystem() {}

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> TRACKERS.clear());
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> TRACKERS.remove(handler.getPlayer().getUuid()));
        ServerTickEvents.END_SERVER_TICK.register(ReversedEchoSystem::tick);
    }

    private static void tick(MinecraftServer server) {
        long now = server.getTicks();
        if (now % 20 != 0) return;
        if (EggCycleState.get(server).getStage() < 3) return;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.isSpectator() || player.getWorld().getRegistryKey() != World.OVERWORLD) continue;
            Tracker t = TRACKERS.computeIfAbsent(player.getUuid(), k -> new Tracker());
            Vec3d pos = player.getPos();
            if (pos.squaredDistanceTo(t.lastPos) < STILL_EPSILON_SQ) t.stillSeconds++;
            else t.stillSeconds = 0;
            t.lastPos = pos;

            if (t.stillSeconds < STILL_SECONDS || now < t.nextAllowed) continue;
            if (player.getRandom().nextFloat() >= CHANCE_PER_SECOND) continue;
            play(player);
            t.nextAllowed = now + COOLDOWN_TICKS;
        }
    }

    /** Phát một âm quen thuộc tua ngược, cách 3–5 block sau lưng người chơi, chỉ người đó nghe. */
    public static void play(ServerPlayerEntity player) {
        Random random = player.getRandom();
        Vec3d look = player.getRotationVec(1.0f);
        Vec3d back = new Vec3d(-look.x, 0, -look.z);
        if (back.lengthSquared() < 1.0e-4) back = new Vec3d(0, 0, 1);
        back = back.normalize().multiply(3.0 + random.nextDouble() * 2.0);
        Vec3d at = player.getPos().add(back).add(0, 0.5, 0);
        player.networkHandler.sendPacket(new PlaySoundS2CPacket(
                Registries.SOUND_EVENT.getEntry(SamsaraSounds.REVERSED_FAMILIAR),
                SoundCategory.AMBIENT, at.x, at.y, at.z,
                1.0f, 0.95f + random.nextFloat() * 0.1f, random.nextLong()));
    }
}
