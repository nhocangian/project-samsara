package com.samsara.client;

import com.samsara.mixin.MusicTrackerAccessor;
import com.samsara.system.celestial.SolarFractureManager;
import com.samsara.system.cycle.StageAnomalySystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Dị tượng phía CLIENT theo giai đoạn (bổ sung cho StageAnomalySystem phía server).
 *
 *  Stage 1+ : nhạc nền Vanilla thỉnh thoảng bị "giật" như băng từ kẹt đầu đọc.
 *  Stage 2+ : nước ở Overworld đổi sang màu xám bạc.
 *  Mọi stage: báo cho server biết người chơi có đang ngắm qua máy quay với IR bật không
 *             (để cây trồng lớn dưới ánh hồng ngoại ở Stage 2+).
 *
 * Lệnh test: /samsara test anomaly music (server gửi gói MUSIC_STUTTER_PACKET).
 */
@Environment(EnvType.CLIENT)
public class StageAnomalyClient {
    /** Màu nước xám bạc của Stage 2+ (mặt nước / sương mù dưới nước). */
    public static final int SILVER_WATER = 0x8E979E;
    public static final int SILVER_WATER_FOG = 0x2E3338;

    /** Xác suất bắt đầu một cơn giật mỗi tick khi nhạc đang phát (~1 lần / 3 phút). */
    private static final float STUTTER_CHANCE = 1.0f / 3600.0f;
    /** Mẫu giật: số tick tắt tiếng / có tiếng xen kẽ, bắt đầu bằng tắt tiếng. */
    private static final int[] STUTTER_PATTERN = {3, 4, 2, 6, 4};

    private static final Random RANDOM = Random.create();

    private static int lastStage = 0;
    private static boolean lastIrObserving = false;

    private static int stutterStep = -1;
    private static int stutterTicksLeft = 0;
    private static boolean paused = false;

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(StageAnomalySystem.MUSIC_STUTTER_PACKET,
                (client, handler, buf, responseSender) -> client.execute(StageAnomalyClient::startStutter));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            endStutter(client);
            lastIrObserving = false;
            lastStage = 0;
        }));

        ClientTickEvents.END_CLIENT_TICK.register(StageAnomalyClient::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        int stage = SolarFractureManager.clientStage;

        // Nước đổi màu khi qua ngưỡng Stage 2 -> xoá cache màu biome và dựng lại chunk
        if ((stage >= 2) != (lastStage >= 2)) {
            client.world.reloadColor();
            client.worldRenderer.reload();
        }
        lastStage = stage;

        // Báo trạng thái ngắm IR lên server khi thay đổi
        boolean irObserving = CamcorderClientManager.isFullyAimed()
                && CamcorderClientManager.isNightVision()
                && CamcorderClientManager.getBatteryLevel() > 0.0f;
        if (irObserving != lastIrObserving) {
            lastIrObserving = irObserving;
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBoolean(irObserving);
            ClientPlayNetworking.send(StageAnomalySystem.IR_OBSERVE_PACKET, buf);
        }

        tickMusicStutter(client, stage);
    }

    // ==========================================
    // NHẠC NỀN BỊ GIẬT (STAGE 1+)
    // ==========================================

    private static void tickMusicStutter(MinecraftClient client, int stage) {
        if (stutterStep < 0) {
            if (stage >= 1 && client.world.getRegistryKey() == World.OVERWORLD
                    && isMusicPlaying(client) && RANDOM.nextFloat() < STUTTER_CHANCE) {
                startStutter();
            }
            return;
        }

        if (--stutterTicksLeft > 0) return;
        stutterStep++;
        if (stutterStep >= STUTTER_PATTERN.length) {
            endStutter(client);
            return;
        }
        applyStutterStep(client);
    }

    public static void startStutter() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (stutterStep >= 0) return;
        stutterStep = 0;
        applyStutterStep(client);
    }

    /** Bước chẵn: tắt tiếng; bước lẻ: phát lại. */
    private static void applyStutterStep(MinecraftClient client) {
        stutterTicksLeft = STUTTER_PATTERN[stutterStep];
        boolean shouldPause = stutterStep % 2 == 0;
        if (shouldPause && !paused) {
            client.getSoundManager().pauseAll();
            paused = true;
        } else if (!shouldPause && paused) {
            client.getSoundManager().resumeAll();
            paused = false;
        }
    }

    private static void endStutter(MinecraftClient client) {
        if (paused) {
            client.getSoundManager().resumeAll();
            paused = false;
        }
        stutterStep = -1;
        stutterTicksLeft = 0;
    }

    private static boolean isMusicPlaying(MinecraftClient client) {
        SoundInstance current = ((MusicTrackerAccessor) client.getMusicTracker()).samsara$getCurrent();
        return current != null && client.getSoundManager().isPlaying(current);
    }

    // ==========================================
    // NƯỚC XÁM BẠC (STAGE 2+)
    // ==========================================

    /**
     * Gọi từ BiomeWaterColorMixin (có thể từ luồng dựng chunk của Sodium).
     * Đổi màu ngay tại Biome.getWaterColor nên vanilla, Sodium và Iris đều đọc cùng một màu.
     */
    public static boolean shouldSilverWater() {
        if (SolarFractureManager.clientStage < 2) return false;
        MinecraftClient client = MinecraftClient.getInstance();
        return client.world != null && client.world.getRegistryKey() == World.OVERWORLD;
    }
}
