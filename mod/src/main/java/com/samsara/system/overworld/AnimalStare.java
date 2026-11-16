package com.samsara.system.overworld;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Con vật nhìn (Overworld, từ Stage 0): thỉnh thoảng cả đàn bò / cừu / lợn / gà quanh người chơi cùng ngừng gặm cỏ,
 * quay đầu về phía người chơi chừng 2-3 giây, rồi ăn tiếp như không có gì. Không tiếng, không chữ. Mỗi người chơi
 * vài phút mới có một lần.
 *
 * <p>Trong lúc nhìn, AI của con vật tắt (đứng yên, không đi lang thang) và đầu được xoay dần về phía người chơi; hết
 * nhịp thì bật AI lại. Con vật đã tắt AI sẵn (do hệ thống khác) thì không đụng tới.
 */
public final class AnimalStare {
    private AnimalStare() {}

    private static final double RANGE = 20.0;
    private static final int MIN_GAP = 3600;
    private static final int EXTRA_GAP = 3600;
    private static final int FIRST_DELAY = 1800;
    /** Độ quay đầu tối đa mỗi tick (độ): quay từ từ như con vật thật, không giật. */
    private static final float TURN_SPEED = 14.0f;

    private record Stare(ServerPlayerEntity player, long until) {}

    private static final Map<MobEntity, Stare> STARING = new HashMap<>();
    private static final Map<UUID, Long> NEXT = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(AnimalStare::tick);
        // Tắt server giữa nhịp nhìn: bật AI lại trước khi lưu, kẻo con vật đứng im vĩnh viễn
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            STARING.keySet().forEach(m -> m.setAiDisabled(false));
            STARING.clear();
        });
    }

    private static void tick(MinecraftServer server) {
        ServerWorld world = server.getWorld(World.OVERWORLD);
        if (world == null) return;
        long now = world.getTime();

        for (Iterator<Map.Entry<MobEntity, Stare>> it = STARING.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<MobEntity, Stare> e = it.next();
            MobEntity mob = e.getKey();
            Stare s = e.getValue();
            if (!mob.isAlive() || mob.isRemoved() || mob.getWorld() != world || s.player.isRemoved() || now >= s.until) {
                mob.setAiDisabled(false);
                it.remove();
                continue;
            }
            face(mob, s.player);
        }

        if (now % 20 != 0 || FilmingState.scripted(server)) return;
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isSpectator()) continue;
            long next = NEXT.computeIfAbsent(player.getUuid(), id -> now + FIRST_DELAY);
            if (now < next) continue;
            if (start(world, player, now, false) > 0) NEXT.put(player.getUuid(), now + MIN_GAP + world.random.nextInt(EXTRA_GAP));
        }
    }

    /** Cho cả đàn quanh người chơi nhìn; cần ít nhất 2 con (trừ khi {@code force}). Trả về số con. */
    private static int start(ServerWorld world, ServerPlayerEntity player, long now, boolean force) {
        List<MobEntity> herd = new ArrayList<>();
        herd.addAll(world.getEntitiesByClass(CowEntity.class, player.getBoundingBox().expand(RANGE), e -> true));
        herd.addAll(world.getEntitiesByClass(SheepEntity.class, player.getBoundingBox().expand(RANGE), e -> true));
        herd.addAll(world.getEntitiesByClass(PigEntity.class, player.getBoundingBox().expand(RANGE), e -> true));
        herd.addAll(world.getEntitiesByClass(ChickenEntity.class, player.getBoundingBox().expand(RANGE), e -> true));
        herd.removeIf(m -> m.isAiDisabled() || m.hasVehicle() || m.isLeashed() || STARING.containsKey(m));
        if (herd.size() < (force ? 1 : 2)) return 0;
        for (MobEntity mob : herd) {
            // Mỗi con dừng lệch nhau vài tick, không đồng loạt như máy
            long until = now + 45 + world.random.nextInt(20);
            mob.getNavigation().stop();
            mob.setAiDisabled(true);
            STARING.put(mob, new Stare(player, until));
        }
        return herd.size();
    }

    private static void face(MobEntity mob, ServerPlayerEntity player) {
        double dx = player.getX() - mob.getX(), dz = player.getZ() - mob.getZ();
        float want = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float head = mob.getHeadYaw() + MathHelper.clamp(MathHelper.wrapDegrees(want - mob.getHeadYaw()), -TURN_SPEED, TURN_SPEED);
        mob.setHeadYaw(head);
        // Thân chỉ xoay theo một nửa: đầu ngoái lại nhìn, như con vật thật
        float body = mob.getBodyYaw() + MathHelper.clamp(MathHelper.wrapDegrees(want - mob.getBodyYaw()) * 0.5f, -TURN_SPEED * 0.5f, TURN_SPEED * 0.5f);
        mob.setBodyYaw(body);
        mob.setYaw(body);
        double dy = player.getEyeY() - mob.getEyeY();
        mob.setPitch((float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
    }

    /** Lệnh test: cả đàn quanh người chơi nhìn ngay. */
    public static int testNow(ServerPlayerEntity player) {
        if (player.getWorld().getRegistryKey() != World.OVERWORLD) return 0;
        return start(player.getServerWorld(), player, player.getServerWorld().getTime(), true);
    }
}
