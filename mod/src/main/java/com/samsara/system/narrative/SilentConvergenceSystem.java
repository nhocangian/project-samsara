package com.samsara.system.narrative;

import com.samsara.registry.SamsaraDimensions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Tính năng 4: Cuộc Diễu Hành Trầm Lặng (The Silent Convergence) - Giai đoạn 3.
 * Khi người chơi bước vào chiều không gian The Womb hoặc phòng tế lễ:
 * Toàn bộ quái vật thù địch lập tức đình chiến hoàn toàn.
 * Chúng tụ họp lại thành một vòng tròn yên lặng như những mảnh linh hồn đến chứng kiến sự thức tỉnh của chính mình.
 */
public class SilentConvergenceSystem {

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(SilentConvergenceSystem::tick);
    }

    private static void tick(MinecraftServer server) {
        if (server.getTicks() % 5 != 0) return; // Chạy mỗi 5 ticks

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            boolean inTheWomb = player.getServerWorld().getRegistryKey().equals(SamsaraDimensions.THE_WOMB_WORLD_KEY);
            boolean inRitualDepth = player.getServerWorld().getRegistryKey().getValue().getPath().equals("overworld") && player.getY() <= -50;

            if (inTheWomb || inRitualDepth) {
                Box box = new Box(player.getBlockPos()).expand(32.0);
                List<HostileEntity> nearbyHostiles = player.getServerWorld().getEntitiesByClass(HostileEntity.class, box, h -> true);

                for (HostileEntity mob : nearbyHostiles) {
                    // 1. Đình chiến tuyệt đối: Xóa bỏ mục tiêu tấn công
                    mob.setTarget(null);

                    // 2. Nếu là Skeleton: không giương cung
                    if (mob instanceof SkeletonEntity skeleton) {
                        skeleton.setAttacking(false);
                    }

                    // 3. Nếu là Enderman: không phẫn nộ, đứng yên
                    if (mob instanceof EndermanEntity enderman) {
                        enderman.setProvoked();
                        enderman.setTarget(null);
                    }

                    // 4. Di chuyển chậm rãi thành vòng tròn chứng kiến
                    double dist = mob.distanceTo(player);
                    if (dist > 6.0) {
                        // Tiếp cận chậm rãi về cự ly 6 block
                        mob.getNavigation().startMovingTo(player.getX(), player.getY(), player.getZ(), 0.55);
                    } else if (dist < 4.0) {
                        // Lùi nhẹ để giữ khoảng cách tôn nghiêm
                        Vec3d away = mob.getPos().subtract(player.getPos()).normalize().multiply(5.0);
                        mob.getNavigation().startMovingTo(player.getX() + away.x, player.getY(), player.getZ() + away.z, 0.45);
                    } else {
                        // Đứng yên nhìn thẳng vào người chơi
                        mob.getNavigation().stop();
                        mob.getLookControl().lookAt(player, 30.0f, 30.0f);
                    }
                }
            }
        }
    }
}
