package com.samsara.system.cycle;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Quái ngập ngừng (Overworld, từ Stage 1): thỉnh thoảng một con quái đang đuổi người chơi khựng lại chừng nửa giây,
 * đứng nhìn thẳng vào người chơi, rồi lao vào tiếp như không có gì. Không âm thanh, không hạt, không chữ: người chơi
 * chỉ thấy nó "đứng hình" một nhịp và tự hỏi có phải game giật không.
 */
public final class HostileHesitationSystem {
    private HostileHesitationSystem() {}

    private static final int MIN_TICKS = 8;
    private static final int EXTRA_TICKS = 8;
    /** Mỗi con chỉ khựng một lần trong chừng này tick. */
    private static final int COOLDOWN_TICKS = 600;
    /** Xác suất mỗi lượt kiểm tra (0,5 s) cho một con đang nhắm người chơi. */
    private static final float CHANCE = 0.06f;

    private static final Map<HostileEntity, Long> HESITATING = new WeakHashMap<>();
    private static final Map<HostileEntity, ServerPlayerEntity> STARING_AT = new WeakHashMap<>();
    private static final Map<HostileEntity, Long> COOLDOWN = new WeakHashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(HostileHesitationSystem::tick);
    }

    private static void tick(MinecraftServer server) {
        long now = server.getTicks();
        for (Iterator<Map.Entry<HostileEntity, Long>> it = HESITATING.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<HostileEntity, Long> e = it.next();
            HostileEntity mob = e.getKey();
            ServerPlayerEntity player = STARING_AT.get(mob);
            if (!mob.isAlive() || player == null || now >= e.getValue()) {
                // Hết nhịp: nhắm lại người chơi như cũ
                if (mob.isAlive() && player != null && player.isAlive()) mob.setTarget(player);
                COOLDOWN.put(mob, now + COOLDOWN_TICKS);
                STARING_AT.remove(mob);
                it.remove();
                continue;
            }
            mob.setTarget(null);
            mob.getNavigation().stop();
            if (mob instanceof SkeletonEntity skeleton) skeleton.setAttacking(false);
            mob.getLookControl().lookAt(player, 180.0f, 180.0f);
        }

        if (now % 10 != 0 || com.samsara.system.overworld.FilmingState.scripted(server)) return;
        if (EggCycleState.get(server).getStage() < 1) return;
        ServerWorld world = server.getWorld(World.OVERWORLD);
        if (world == null) return;
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isSpectator() || player.isCreative()) continue;
            Box range = player.getBoundingBox().expand(12.0);
            for (HostileEntity mob : world.getEntitiesByClass(HostileEntity.class, range, h -> h.getTarget() == player)) {
                if (HESITATING.containsKey(mob) || now < COOLDOWN.getOrDefault(mob, 0L)) continue;
                if (mob.squaredDistanceTo(player) < 9.0) continue;   // sát mặt rồi thì thôi, khỏi thành "cứu" người chơi
                if (world.random.nextFloat() >= CHANCE) continue;
                HESITATING.put(mob, now + MIN_TICKS + world.random.nextInt(EXTRA_TICKS + 1));
                STARING_AT.put(mob, player);
            }
        }
    }

    /** Lệnh test: mọi con quái quanh người chơi khựng lại ngay (bỏ qua stage và xác suất). */
    public static int testNow(ServerPlayerEntity player) {
        long now = player.getServer().getTicks();
        int n = 0;
        for (HostileEntity mob : player.getServerWorld().getEntitiesByClass(HostileEntity.class, player.getBoundingBox().expand(16.0), h -> true)) {
            HESITATING.put(mob, now + 40);
            STARING_AT.put(mob, player);
            n++;
        }
        return n;
    }
}
