package com.samsara.system.cycle;

import com.samsara.entity.EchoDummyEntity;
import com.samsara.registry.SamsaraEntities;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bóng ma Nam_1998 (Video 4: "các bóng ma Nam_1998 đứng vây quanh").
 *
 * Từ Stage 2, vào ban đêm ở Overworld, thỉnh thoảng một vòng 6-8 bóng người mang tên "Nam_1998"
 * đứng lặng quanh người chơi, mặt luôn quay về phía người chơi. Chúng là EchoDummyEntity nên
 * CHỈ thấy được qua ống kính máy quay (mắt thường không thấy gì, chỉ có nhiễu EMF trên HUD).
 * Bước lại quá gần, hoặc sau 30 giây, bóng ma tan thành khói.
 * Lệnh test: /samsara test nam_ghosts.
 */
public class NamGhostSystem {
    public static final String GHOST_TAG = "samsara_nam_ghost";
    public static final String GHOST_NAME = "Nam_1998";
    /** UUID "offline" cố định của Nam_1998, để skin mặc định luôn giống nhau. */
    public static final UUID NAM_UUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + GHOST_NAME).getBytes(StandardCharsets.UTF_8));

    private static final int RING_LIFETIME = 600;
    private static final float RING_CHANCE = 1.0f / 12.0f; // mỗi lần kiểm tra (20 giây)
    private static final double VANISH_DISTANCE = 3.0;

    private record Ring(UUID watcher, List<EchoDummyEntity> ghosts, int endTick) {}

    private static final Map<UUID, Ring> RINGS = new HashMap<>();

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> RINGS.clear());
        ServerTickEvents.END_SERVER_TICK.register(NamGhostSystem::tick);
    }

    private static void tick(MinecraftServer server) {
        int now = server.getTicks();
        tickRings(server, now);

        if (now % 400 != 0) return;
        if (EggCycleState.get(server).getStage() < 2) return;
        ServerWorld overworld = server.getOverworld();
        if (!overworld.isNight()) return;
        for (ServerPlayerEntity player : overworld.getPlayers()) {
            if (player.isSpectator() || RINGS.containsKey(player.getUuid())) continue;
            if (overworld.random.nextFloat() < RING_CHANCE) {
                spawnRing(player);
            }
        }
    }

    /** @return số bóng ma vừa xuất hiện */
    public static int spawnRing(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        if (world.getRegistryKey() != World.OVERWORLD) return 0;
        dissolveRing(RINGS.remove(player.getUuid()));

        int count = 6 + world.random.nextInt(3);
        double radius = 9.0 + world.random.nextDouble() * 3.0;
        double offset = world.random.nextDouble() * Math.PI * 2.0;
        List<EchoDummyEntity> ghosts = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            double a = offset + i * (Math.PI * 2.0 / count);
            double x = player.getX() + Math.cos(a) * radius;
            double z = player.getZ() + Math.sin(a) * radius;
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, MathHelper.floor(x), MathHelper.floor(z));
            if (Math.abs(y - player.getY()) > 8) y = player.getBlockY();

            EchoDummyEntity ghost = SamsaraEntities.ECHO_DUMMY.create(world);
            if (ghost == null) continue;
            ghost.refreshPositionAndAngles(x, y, z, 0.0f, 0.0f);
            ghost.setPlayerUuid(NAM_UUID);
            ghost.setPermanent(true);
            ghost.setCustomName(Text.literal(GHOST_NAME));
            ghost.setCustomNameVisible(true);
            ghost.addCommandTag(GHOST_TAG);
            faceTowards(ghost, player.getX(), player.getZ());
            world.spawnEntity(ghost);
            ghosts.add(ghost);
        }
        RINGS.put(player.getUuid(), new Ring(player.getUuid(), ghosts, world.getServer().getTicks() + RING_LIFETIME));
        com.samsara.system.ui.UiHorrorSystem.advancement(player, "nam_ghosts", "samsara.advancement.co_ai_do_dung", net.minecraft.item.Items.PLAYER_HEAD);
        return ghosts.size();
    }

    private static void tickRings(MinecraftServer server, int now) {
        if (RINGS.isEmpty()) return;
        Iterator<Map.Entry<UUID, Ring>> it = RINGS.entrySet().iterator();
        while (it.hasNext()) {
            Ring ring = it.next().getValue();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(ring.watcher());
            if (player == null || now >= ring.endTick() || player.getWorld().getRegistryKey() != World.OVERWORLD) {
                dissolveRing(ring);
                it.remove();
                continue;
            }
            ring.ghosts().removeIf(ghost -> {
                if (ghost.isRemoved()) return true;
                faceTowards(ghost, player.getX(), player.getZ());
                if (ghost.squaredDistanceTo(player) < VANISH_DISTANCE * VANISH_DISTANCE) {
                    dissolve(ghost);
                    return true;
                }
                return false;
            });
            if (ring.ghosts().isEmpty()) it.remove();
        }
    }

    private static void faceTowards(EchoDummyEntity ghost, double tx, double tz) {
        float yaw = (float) (MathHelper.atan2(tz - ghost.getZ(), tx - ghost.getX()) * (180.0 / Math.PI)) - 90.0f;
        ghost.setYaw(yaw);
        ghost.setBodyYaw(yaw);
        ghost.setHeadYaw(yaw);
    }

    private static void dissolveRing(Ring ring) {
        if (ring == null) return;
        for (EchoDummyEntity ghost : ring.ghosts()) dissolve(ghost);
    }

    private static void dissolve(EchoDummyEntity ghost) {
        if (ghost.isRemoved()) return;
        if (ghost.getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.SMOKE, ghost.getX(), ghost.getY() + 0.9, ghost.getZ(), 20, 0.25, 0.5, 0.25, 0.02);
        }
        ghost.discard();
    }

    /** Dọn bóng ma sót lại (vd. server tắt giữa chừng khiến chúng được lưu vào chunk). */
    public static boolean isOrphanGhost(EchoDummyEntity entity) {
        if (!entity.getCommandTags().contains(GHOST_TAG)) return false;
        for (Ring ring : RINGS.values()) {
            if (ring.ghosts().contains(entity)) return false;
        }
        return true;
    }
}
