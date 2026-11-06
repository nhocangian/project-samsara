package com.samsara.entity;

import com.samsara.system.ending.EndingServer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

import java.util.ArrayList;

/**
 * Quả Trứng Nguyên Thủy: khối trứng kim loại cao ~6 block lơ lửng trên bệ đá trong The Womb.
 *  - Đập đúng 60 nhịp/phút (khớp nhật ký Chu Kỳ #0): tiếng tim Warden + vỏ phập phồng (phía client).
 *  - 7 mảnh vỏ bay quanh, tượng trưng cho 7 giấc ngủ.
 *  - Người chơi bước tới gần (TRIGGER_RADIUS) -> bắt đầu đoạn kết (EndingServer).
 * Bất tử, không trọng lực, không va chạm.
 */
public class PrimordialEggEntity extends Entity {
    public static final double TRIGGER_RADIUS = 6.0;
    public static final double HEARTBEAT_RADIUS = 48.0;

    public PrimordialEggEntity(EntityType<? extends PrimordialEggEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    @Override
    protected void initDataTracker() {
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    public boolean canHit() {
        return false;
    }

    @Override
    public boolean shouldRender(double distance) {
        return distance < 256.0 * 256.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.getWorld() instanceof ServerWorld world)) {
            // Bụi hổ phách rơi chậm từ các khe nứt
            if (this.random.nextFloat() < 0.3f) {
                this.getWorld().addParticle(ParticleTypes.FALLING_HONEY,
                        this.getX() + (this.random.nextDouble() - 0.5) * 3.0,
                        this.getY() + 1.0 + this.random.nextDouble() * 4.0,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 3.0, 0, 0, 0);
            }
            return;
        }

        // 60 nhịp / phút
        if (this.age % 20 == 0) {
            world.playSound(null, this.getX(), this.getY() + 3.0, this.getZ(),
                    SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.AMBIENT, 3.0f, 0.7f);
        }

        if (this.age % 5 == 0) {
            for (ServerPlayerEntity player : new ArrayList<>(world.getPlayers())) {
                if (player.isSpectator() || !player.isAlive()) continue;
                double dx = player.getX() - this.getX();
                double dz = player.getZ() - this.getZ();
                if (dx * dx + dz * dz <= TRIGGER_RADIUS * TRIGGER_RADIUS
                        && Math.abs(player.getY() - this.getY()) < 6.0) {
                    EndingServer.tryStart(player, this);
                }
            }
        }
    }
}
