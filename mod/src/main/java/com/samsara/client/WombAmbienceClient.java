package com.samsara.client;

import com.samsara.registry.SamsaraDimensions;
import com.samsara.system.womb.WombPacingSystem;
import com.samsara.system.womb.WombRoute;
import com.samsara.system.womb.chapters.SpireChapter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

/**
 * Âm thanh theo chương của tuyến hành hương (phía client).
 *  - Cả The Womb: tắt nhạc nền vanilla; thỉnh thoảng một tiếng ngân như cá voi rất xa.
 *  - I. Bến Đò: nước vỗ, ván gỗ kẽo kẹt.
 *  - II. Những Cánh Cửa: tiếng cửa gỗ mở / đóng ở đâu đó hai bên tuyến, tiếng thì thầm hang động.
 *  - III. Những Người Khổng Lồ: đá nghiến trầm, tiếng ngân của neo hồi sinh quanh các tượng.
 *  - IV. Tháp Ngược: xích kim loại lạch cạch trên cao.
 *  - Nhịp tim Quả Trứng: nghe được từ rất xa, to dần khi lại gần (gần hẳn thì trứng tự đập, client không đập chồng).
 */
@Environment(EnvType.CLIENT)
public class WombAmbienceClient {
    private static final Random RANDOM = Random.create();
    private static final double HEARTBEAT_RANGE = 220.0;
    private static final double EGG_OWN_RANGE = 44.0;

    private static int ticks = 0;
    private static int nextLocal = 40;
    private static int nextFar = 300;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(WombAmbienceClient::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null || client.isPaused()) return;
        if (client.world.getRegistryKey() != SamsaraDimensions.THE_WOMB_WORLD_KEY) return;
        BlockPos origin = WombRouteClient.getOrigin();
        if (origin == null) return;
        ticks++;

        // Im lặng: không để nhạc piano vanilla phá không khí
        if (ticks % 40 == 0) client.getMusicTracker().stop();

        Vec3d pos = client.player.getPos();
        if (WombRoute.distanceToRoute(origin, pos.x, pos.z) > 60.0) return;
        double progress = WombRoute.progress(origin, pos.x, pos.z);
        int chapter = WombPacingSystem.chapterAt(origin, progress);

        heartbeat(client, origin, pos);

        if (--nextFar <= 0) {
            nextFar = 300 + RANDOM.nextInt(500);
            double a = RANDOM.nextDouble() * Math.PI * 2.0;
            play(client, SoundEvents.ENTITY_ELDER_GUARDIAN_AMBIENT, pos.add(Math.cos(a) * 60, 10, Math.sin(a) * 60), 0.6f, 0.45f);
        }

        if (--nextLocal > 0) return;
        switch (chapter) {
            case 0 -> {
                nextLocal = 60 + RANDOM.nextInt(80);
                if (RANDOM.nextBoolean()) play(client, SoundEvents.BLOCK_WATER_AMBIENT, around(pos, 4), 0.35f, 0.8f);
                else play(client, SoundEvents.BLOCK_WOOD_STEP, around(pos, 3), 0.5f, 0.45f);
            }
            case 1 -> {
                nextLocal = 90 + RANDOM.nextInt(120);
                Vec3d side = sideOfRoute(origin, pos, 14 + RANDOM.nextInt(18));
                SoundEvent door = RANDOM.nextBoolean() ? SoundEvents.BLOCK_WOODEN_DOOR_OPEN : SoundEvents.BLOCK_WOODEN_DOOR_CLOSE;
                if (RANDOM.nextFloat() < 0.25f) play(client, SoundEvents.AMBIENT_CAVE.value(), side, 0.4f, 0.6f);
                else play(client, door, side.add(0, 4 + RANDOM.nextInt(6), 0), 0.45f, 0.5f + RANDOM.nextFloat() * 0.2f);
            }
            case 2 -> {
                nextLocal = 140 + RANDOM.nextInt(140);
                Vec3d side = sideOfRoute(origin, pos, 20);
                if (RANDOM.nextBoolean()) play(client, SoundEvents.BLOCK_GRINDSTONE_USE, side.add(0, 12, 0), 0.4f, 0.3f);
                else play(client, SoundEvents.BLOCK_RESPAWN_ANCHOR_AMBIENT, side.add(0, 6, 0), 0.6f, 0.5f);
            }
            default -> {
                nextLocal = 100 + RANDOM.nextInt(100);
                BlockPos spire = WombRoute.Stop.SPIRE.pos(origin);
                play(client, SoundEvents.BLOCK_CHAIN_PLACE, new Vec3d(spire.getX() + RANDOM.nextInt(13) - 6, 112, spire.getZ() + RANDOM.nextInt(13) - 6), 0.5f, 0.5f);
            }
        }
    }

    /** Nhịp tim trứng nghe từ xa: 1 nhịp/giây, âm lượng tăng dần theo khoảng cách. */
    private static void heartbeat(MinecraftClient client, BlockPos origin, Vec3d pos) {
        if (ticks % 20 != 0) return;
        BlockPos s = WombRoute.Stop.SPIRE.pos(origin);
        Vec3d egg = new Vec3d(s.getX() + 0.5, SpireChapter.POOL_Y + 3, s.getZ() + 0.5);
        double d = pos.distanceTo(egg);
        if (d > HEARTBEAT_RANGE || d < EGG_OWN_RANGE) return;
        float k = (float) (1.0 - (d - EGG_OWN_RANGE) / (HEARTBEAT_RANGE - EGG_OWN_RANGE));
        float volume = 0.08f + 0.6f * k * k;
        client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_WARDEN_HEARTBEAT, 0.7f, volume));
    }

    private static Vec3d around(Vec3d p, double r) {
        return p.add((RANDOM.nextDouble() - 0.5) * 2 * r, 0, (RANDOM.nextDouble() - 0.5) * 2 * r);
    }

    private static Vec3d sideOfRoute(BlockPos origin, Vec3d pos, double lateral) {
        Vec3d t = WombRoute.tangentNear(origin, pos);
        Vec3d n = new Vec3d(-t.z, 0, t.x).multiply(RANDOM.nextBoolean() ? lateral : -lateral);
        return pos.add(n).add(t.multiply(RANDOM.nextInt(20) - 6));
    }

    private static void play(MinecraftClient client, SoundEvent sound, Vec3d at, float volume, float pitch) {
        client.getSoundManager().play(new PositionedSoundInstance(sound, SoundCategory.AMBIENT, volume,
                MathHelper.clamp(pitch, 0.1f, 2.0f), RANDOM, at.x, at.y, at.z));
    }
}
