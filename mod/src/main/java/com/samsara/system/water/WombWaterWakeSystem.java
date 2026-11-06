package com.samsara.system.water;

import com.samsara.registry.SamsaraDimensions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

/**
 * Server-Side Water Wake System:
 * Đồng bộ vệt rẽ nước cho các người chơi khác trong thế giới The Womb.
 */
public class WombWaterWakeSystem {

    private static final DustParticleEffect WAKE_FOAM =
            new DustParticleEffect(new Vector3f(0.92f, 0.96f, 1.0f), 1.25f);

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (!world.getRegistryKey().equals(SamsaraDimensions.THE_WOMB_WORLD_KEY)) {
                return;
            }

            for (Entity entity : world.iterateEntities()) {
                if (!(entity instanceof BoatEntity || entity instanceof LivingEntity)) {
                    continue;
                }

                double y = entity.getY();
                if (y < 60.5 || y > 63.8) {
                    continue;
                }

                Vec3d vel = entity.getVelocity();
                double speedSq = vel.x * vel.x + vel.z * vel.z;
                if (speedSq < 0.0008) {
                    continue;
                }

                double speed = Math.sqrt(speedSq);
                Vec3d dir = new Vec3d(vel.x / speed, 0, vel.z / speed);
                Vec3d side = new Vec3d(-dir.z, 0, dir.x);

                if (entity instanceof BoatEntity) {
                    double width = 0.80;
                    double length = 1.10;

                    // Vệt rẽ sóng trắng 2 mạn
                    world.spawnParticles(WAKE_FOAM,
                            entity.getX() - dir.x * 0.4 + side.x * width, 62.03, entity.getZ() - dir.z * 0.4 + side.z * width,
                            1, 0, 0, 0, 0);

                    world.spawnParticles(WAKE_FOAM,
                            entity.getX() - dir.x * 0.4 - side.x * width, 62.03, entity.getZ() - dir.z * 0.4 - side.z * width,
                            1, 0, 0, 0, 0);

                    // Đuôi thuyền
                    world.spawnParticles(WAKE_FOAM,
                            entity.getX() - dir.x * length, 62.03, entity.getZ() - dir.z * length,
                            1, 0, 0, 0, 0);
                } else {
                    world.spawnParticles(ParticleTypes.SPLASH,
                            entity.getX(), 62.08, entity.getZ(),
                            1, 0.02, 0.0, 0.02, 0.01);
                }
            }
        });
    }
}
