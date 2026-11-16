package com.samsara.system.additions;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Bước chân dội lại (Overworld): đang đi bộ thì thỉnh thoảng nghe ba tiếng bước chân của chính mình lặp lại trễ nửa nhịp,
 * vang từ 6-8 khối phía sau lưng, đúng âm của khối đang giẫm (cỏ, sỏi, đá...). Người chơi dừng lại thì "nó" vẫn bước thêm
 * một hai bước rồi mới im. Chỉ người đó nghe; hiếm (mỗi người ít nhất {@link #COOLDOWN} tick một lần).
 */
public final class PhantomAudioEchoSystem {
    private PhantomAudioEchoSystem() {}

    private static final int COOLDOWN = 3600;
    /** Xác suất mỗi giây đang đi bộ, sau khi hết thời gian nghỉ. */
    private static final float CHANCE = 1.0f / 90.0f;
    private static final int STEPS = 3;
    private static final int FIRST_DELAY = 10;
    private static final int STEP_GAP = 8;

    private record Echo(long start, Vec3d back, SoundEvent sound) {}

    private static final Map<UUID, Long> NEXT_ALLOWED = new HashMap<>();
    private static final Map<UUID, Echo> PLAYING = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(PhantomAudioEchoSystem::tick);
    }

    private static void tick(MinecraftServer server) {
        long now = server.getTicks();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID id = player.getUuid();
            Echo echo = PLAYING.get(id);
            if (echo != null) {
                long t = now - echo.start;
                if (t >= 0 && t % STEP_GAP == 0) play(player, echo);
                if (t >= (long) (STEPS - 1) * STEP_GAP) PLAYING.remove(id);
                continue;
            }
            if (now % 20 != 0 || player.getWorld().getRegistryKey() != World.OVERWORLD || com.samsara.system.overworld.FilmingState.scripted(server)) continue;
            if (player.isSpectator() || player.isSneaking() || !player.isOnGround()) continue;
            if (player.getVelocity().horizontalLengthSquared() < 0.002) continue;
            if (now < NEXT_ALLOWED.getOrDefault(id, (long) COOLDOWN)) continue;
            if (player.getRandom().nextFloat() >= CHANCE) continue;
            start(player, now);
        }
    }

    private static void start(ServerPlayerEntity player, long now) {
        BlockPos below = player.getBlockPos().down();
        BlockState ground = player.getWorld().getBlockState(below);
        if (ground.isAir()) return;
        Vec3d facing = player.getRotationVec(1.0f).multiply(1, 0, 1).normalize();
        Vec3d back = facing.multiply(-(6.0 + player.getRandom().nextDouble() * 2.0));
        PLAYING.put(player.getUuid(), new Echo(now + FIRST_DELAY, back, ground.getSoundGroup().getStepSound()));
        NEXT_ALLOWED.put(player.getUuid(), now + COOLDOWN);
    }

    private static void play(ServerPlayerEntity player, Echo echo) {
        Vec3d p = player.getPos().add(echo.back);
        player.networkHandler.sendPacket(new PlaySoundS2CPacket(Registries.SOUND_EVENT.getEntry(echo.sound),
                SoundCategory.PLAYERS, p.x, p.y, p.z, 0.45f, 0.95f + player.getRandom().nextFloat() * 0.1f,
                player.getRandom().nextLong()));
    }

    /** Lệnh test: phát ngay một lượt (bỏ qua thời gian nghỉ và xác suất). */
    public static void testNow(ServerPlayerEntity player) {
        start(player, player.getServer().getTicks());
    }
}
