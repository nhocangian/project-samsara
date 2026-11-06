package com.samsara.client;

import com.samsara.registry.SamsaraDimensions;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.particle.ParticleTypes;

/**
 * Client-Side Water Interaction for Swimming Entities (Người chơi/quái vật bơi lội):
 * Tạo các tia nước bắn và bọt khí khi di chuyển dưới nước.
 * Thuyền do WombWaterWakeRibbonRenderer đảm nhiệm với dải rẽ nước phẳng chuẩn phong cách Wakes.
 */
public class WombClientWaterWakeSystem {

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientWorld world = client.world;
            if (world == null || client.player == null) return;

            boolean isWomb = world.getRegistryKey().equals(SamsaraDimensions.THE_WOMB_WORLD_KEY);

            for (Entity entity : world.getEntities()) {
                if (entity instanceof BoatEntity) continue; // Do WombWaterWakeRibbonRenderer xử lý

                if (entity instanceof LivingEntity living) {
                    processLiving(world, living, isWomb);
                }
            }
        });
    }

    private static void processLiving(ClientWorld world, LivingEntity living, boolean isWomb) {
        double dx = living.getX() - living.prevX;
        double dz = living.getZ() - living.prevZ;
        if (dx * dx + dz * dz < 0.0003) return;

        double y = living.getY();
        if (isWomb) {
            if (y < 60.5 || y > 63.8) return;
        } else {
            if (!living.isTouchingWater()) return;
        }

        double waterY = isWomb ? 62.02 : living.getY() + 0.1;
        world.addParticle(ParticleTypes.SPLASH, living.getX(), waterY, living.getZ(),
                (Math.random() - 0.5) * 0.04, 0.03, (Math.random() - 0.5) * 0.04);
        world.addParticle(ParticleTypes.BUBBLE_POP, living.getX(), waterY, living.getZ(), 0.0, 0.01, 0.0);
    }
}
