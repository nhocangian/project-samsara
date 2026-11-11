package com.samsara.system.womb;

import com.samsara.entity.TheWandererEntity;
import com.samsara.registry.SamsaraDimensions;
import com.samsara.registry.SamsaraEntities;
import com.samsara.registry.SamsaraSounds;
import com.samsara.system.cycle.EggCycleState;
import com.samsara.system.ending.EndingServer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Wanderer bám đuôi trên tuyến hành hương (kiểu "Weeping Angel").
 *
 *  - Bắt đầu khi người chơi đi qua Bến Đò; dừng khi tới chân Tháp Singularity hoặc rời The Womb.
 *  - Nó đứng trên tuyến, phía sau lưng người chơi. Mắt thường không thấy (chỉ thấy qua máy quay).
 *  - Nếu KHÔNG bị nhìn qua máy quay, cứ vài giây nó áp sát thêm một quãng, kèm tiếng lội nước sau lưng.
 *    Ngoảnh lại nhìn nó qua ống kính thì nó đứng sững.
 *  - Chạm tới người chơi: không chết, nhưng bị nuốt vào cơn glitch và ném lùi về chặng trước;
 *    nó tan biến rồi lại bắt đầu từ xa.
 * Lệnh test: /samsara test wanderer_chase [start|stop].
 */
public class WanderChaseSystem {
    /** Gói C2S: client báo đang ngắm hẳn qua máy quay hay không (boolean). */
    public static final Identifier AIM_PACKET = new Identifier("samsara", "camcorder_aim");

    private static final double START_DISTANCE = 36.0;
    private static final double STEP = 3.0;
    private static final int STEP_INTERVAL = 50;
    private static final double TOUCH_DISTANCE = 2.2;
    private static final double CHASE_BEGIN = 26.0;   // quãng đường dọc tuyến sau Bến Đò
    private static final double CHASE_END_MARGIN = 34.0; // dừng trước chân tháp

    private static class Chase {
        TheWandererEntity entity;
        double behind = START_DISTANCE;
        int nextStepTick;
    }

    private static final Map<UUID, Chase> CHASES = new HashMap<>();
    private static final Set<UUID> AIMING = new HashSet<>();
    private static final Set<UUID> DISABLED = new HashSet<>();

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(AIM_PACKET, (server, player, handler, buf, sender) -> {
            boolean aiming = buf.readBoolean();
            server.execute(() -> {
                if (aiming) AIMING.add(player.getUuid());
                else AIMING.remove(player.getUuid());
            });
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.getPlayer().getUuid();
            AIMING.remove(id);
            stop(id);
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            CHASES.clear();
            AIMING.clear();
            DISABLED.clear();
        });
        ServerTickEvents.END_SERVER_TICK.register(WanderChaseSystem::tick);
    }

    /** Người chơi đang áp máy quay lên mắt (client báo qua AIM_PACKET). */
    public static boolean isAiming(UUID player) {
        return AIMING.contains(player);
    }

    public static boolean isTracked(TheWandererEntity entity) {
        for (Chase c : CHASES.values()) {
            if (c.entity == entity) return true;
        }
        return false;
    }

    /** Lệnh test: bật / tắt cuộc đuổi cho một người chơi. */
    public static void setEnabled(ServerPlayerEntity player, boolean enabled) {
        if (enabled) DISABLED.remove(player.getUuid());
        else {
            DISABLED.add(player.getUuid());
            stop(player.getUuid());
        }
    }

    /** Lệnh test: bắt đầu ngay, ở gần hơn bình thường. */
    public static boolean startNow(ServerPlayerEntity player) {
        DISABLED.remove(player.getUuid());
        Chase chase = CHASES.computeIfAbsent(player.getUuid(), id -> new Chase());
        chase.behind = 20.0;
        chase.nextStepTick = player.getServer().getTicks() + STEP_INTERVAL;
        return place(player, chase);
    }

    private static void stop(UUID id) {
        Chase c = CHASES.remove(id);
        if (c != null && c.entity != null && !c.entity.isRemoved()) c.entity.discard();
    }

    private static void tick(MinecraftServer server) {
        ServerWorld womb = server.getWorld(SamsaraDimensions.THE_WOMB_WORLD_KEY);
        BlockPos origin = EggCycleState.get(server).getRouteOrigin();
        int now = server.getTicks();

        // Dọn những cuộc đuổi không còn hợp lệ
        Iterator<Map.Entry<UUID, Chase>> it = CHASES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Chase> e = it.next();
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(e.getKey());
            if (p == null || womb == null || origin == null || p.getWorld() != womb || !onChaseStretch(origin, p)
                    || EndingServer.isRunning(p)) {
                if (e.getValue().entity != null && !e.getValue().entity.isRemoved()) e.getValue().entity.discard();
                it.remove();
            }
        }
        if (womb == null || origin == null || now % 5 != 0) return;

        for (ServerPlayerEntity player : womb.getPlayers()) {
            if (player.isSpectator() || player.isCreative() && !CHASES.containsKey(player.getUuid())) continue;
            if (DISABLED.contains(player.getUuid()) || !onChaseStretch(origin, player)) continue;
            Chase chase = CHASES.computeIfAbsent(player.getUuid(), id -> {
                Chase c = new Chase();
                c.nextStepTick = now + STEP_INTERVAL;
                return c;
            });
            if (chase.entity == null || chase.entity.isRemoved()) {
                if (!place(player, chase)) continue;
            }

            boolean watched = isWatching(player, chase.entity);
            if (watched) {
                // Bị nhìn qua ống kính: đứng sững, chỉ hoãn bước tiếp theo
                chase.nextStepTick = now + STEP_INTERVAL;
                faceTowards(chase.entity, player);
                continue;
            }
            if (now >= chase.nextStepTick) {
                chase.behind = Math.max(0.0, chase.behind - STEP);
                chase.nextStepTick = now + STEP_INTERVAL;
                place(player, chase);
                womb.playSound(null, chase.entity.getX(), chase.entity.getY(), chase.entity.getZ(),
                        SoundEvents.ENTITY_PLAYER_SPLASH, SoundCategory.HOSTILE, 0.35f, 0.55f);
            }
            if (chase.entity.squaredDistanceTo(player) < TOUCH_DISTANCE * TOUCH_DISTANCE || chase.behind <= 0.5) {
                caught(player, chase, origin);
            }
        }
    }

    private static boolean onChaseStretch(BlockPos origin, ServerPlayerEntity player) {
        if (WombRoute.distanceToRoute(origin, player.getX(), player.getZ()) > 40.0) return false;
        double progress = WombRoute.progress(origin, player.getX(), player.getZ());
        double end = WombRoute.length(origin) - CHASE_END_MARGIN;
        return progress > CHASE_BEGIN && progress < end;
    }

    /** Đặt nó trên tuyến, phía sau người chơi {@code chase.behind} block. */
    private static boolean place(ServerPlayerEntity player, Chase chase) {
        ServerWorld world = player.getServerWorld();
        BlockPos origin = EggCycleState.get(world.getServer()).getRouteOrigin();
        if (origin == null) return false;
        double progress = WombRoute.progress(origin, player.getX(), player.getZ());
        Vec3d p = WombRoute.pointAlong(origin, Math.max(0.0, progress - chase.behind));
        // Khi đã sát người chơi thì đứng ngay sau lưng thay vì trên đường giữa
        if (chase.behind < 6.0) {
            Vec3d back = player.getRotationVec(1.0f).multiply(-1, 0, -1);
            if (back.lengthSquared() > 1.0E-4) p = player.getPos().add(back.normalize().multiply(Math.max(1.6, chase.behind)));
        }
        double y = WombRoute.SURFACE_Y - 1;
        if (chase.entity == null || chase.entity.isRemoved()) {
            TheWandererEntity w = SamsaraEntities.THE_WANDERER.create(world);
            if (w == null) return false;
            w.setStalker(true);
            w.refreshPositionAndAngles(p.x, y, p.z, 0.0f, 0.0f);
            world.spawnEntity(w);
            chase.entity = w;
        } else {
            chase.entity.requestTeleport(p.x, y, p.z);
        }
        faceTowards(chase.entity, player);
        return true;
    }

    /** Người chơi có đang ngắm qua máy quay VÀ hướng nhìn rơi vào nó không. */
    private static boolean isWatching(ServerPlayerEntity player, TheWandererEntity w) {
        if (!AIMING.contains(player.getUuid())) return false;
        Vec3d look = player.getRotationVec(1.0f).normalize();
        Vec3d to = w.getPos().add(0, 1.2, 0).subtract(player.getEyePos());
        double dist = to.length();
        if (dist < 1.0E-3) return true;
        return look.dotProduct(to.multiply(1.0 / dist)) > 0.8;
    }

    private static void faceTowards(TheWandererEntity w, ServerPlayerEntity p) {
        float yaw = (float) (MathHelper.atan2(p.getZ() - w.getZ(), p.getX() - w.getX()) * (180.0 / Math.PI)) - 90.0f;
        w.setYaw(yaw);
        w.setHeadYaw(yaw);
        w.setBodyYaw(yaw);
    }

    /** Bị chạm: glitch nặng, ném lùi về chặng trước, nó bắt đầu lại từ xa. */
    private static void caught(ServerPlayerEntity player, Chase chase, BlockPos origin) {
        ServerWorld world = player.getServerWorld();
        world.spawnParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SamsaraSounds.CAVE_REVERSED, SoundCategory.HOSTILE, 1.4f, 0.5f);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 80, 0, false, false, false));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 120, 0, false, false, false));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 30, 0, false, false, false));

        // Chặng trước quãng đường hiện tại
        double progress = WombRoute.progress(origin, player.getX(), player.getZ());
        WombRoute.Stop back = WombRoute.Stop.DOCK;
        for (WombRoute.Stop s : WombRoute.Stop.values()) {
            if (WombRoute.progressOf(origin, s) < progress - 8.0) back = s;
        }
        BlockPos target = back.pos(origin);
        Vec3d ahead = WombRoute.pointAlong(origin, WombRoute.progressOf(origin, back) + 4.0);
        if (player.hasVehicle()) player.stopRiding();
        float yaw = (float) (MathHelper.atan2(ahead.z - target.getZ(), ahead.x - target.getX()) * (180.0 / Math.PI)) - 90.0f;
        player.teleport(world, target.getX() + 0.5, WombRoute.SURFACE_Y, target.getZ() + 0.5, yaw, 0.0f);
        player.sendMessage(Text.translatable("samsara.wander_chase_system.no_da_o_ngay"), true);
        com.samsara.system.ui.UiHorrorSystem.advancement(player, "wanderer_caught", "samsara.advancement.dung_roi_mat", net.minecraft.item.Items.ENDER_EYE);

        if (chase.entity != null && !chase.entity.isRemoved()) chase.entity.discard();
        chase.entity = null;
        chase.behind = START_DISTANCE;
        chase.nextStepTick = world.getServer().getTicks() + STEP_INTERVAL * 3;
    }
}
