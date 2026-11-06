package com.samsara.entity;

import com.samsara.registry.SamsaraSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * The Wanderer: Karmic Reversal Manifestation.
 * Appears at the player's periphery when sin debt accumulates.
 * Stationary, invulnerable, reacts fatally only when perceived directly.
 */
public class TheWandererEntity extends HostileEntity {
    private static final TrackedData<Boolean> HANDS_RAISED = DataTracker.registerData(TheWandererEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    private int gazeTicks = 0;
    /** Do WanderChaseSystem điều khiển (không lưu: sau khi tải lại thế giới sẽ tự tan biến). */
    private boolean stalker = false;

    public void setStalker(boolean stalker) {
        this.stalker = stalker;
    }

    public boolean isStalker() {
        return stalker;
    }

    /** Lệnh test: đứng yên làm mẫu để ngắm model (không phản đòn, tự biến mất sau 2 phút). */
    private boolean pinned = false;

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }
    private int lifetimeTicks = 0;
    public static final int MAX_LIFETIME = 1200; // 60 seconds

    public TheWandererEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(false);
        this.experiencePoints = 0;
    }

    public static DefaultAttributeContainer.Builder createWandererAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 100.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(HANDS_RAISED, false);
    }

    public boolean areHandsRaised() {
        return this.dataTracker.get(HANDS_RAISED);
    }

    public void setHandsRaised(boolean raised) {
        this.dataTracker.set(HANDS_RAISED, raised);
    }

    @Override
    protected void initGoals() {
        // Only look at the nearest player; never navigate or attack directly
        this.goalSelector.add(1, new LookAtEntityGoal(this, PlayerEntity.class, 32.0f, 1.0f));
    }

    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        return true;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        // Completely immune to all forms of damage
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getWorld().isClient) {
            // Subtle void particles around feet
            if (this.random.nextFloat() < 0.25f) {
                this.getWorld().addParticle(
                        ParticleTypes.SMOKE,
                        this.getX() + (this.random.nextDouble() - 0.5) * 0.5,
                        this.getY() + 0.1,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 0.5,
                        0.0, 0.01, 0.0
                );
            }
            return;
        }

        if (pinned) {
            if (this.age > 2400) this.discard();
            return;
        }

        // Kẻ bám đuôi trên tuyến hành hương: WanderChaseSystem điều khiển, bỏ qua tuổi thọ & phản đòn khi bị nhìn
        if (stalker) {
            if (this.age % 20 == 0 && !com.samsara.system.womb.WanderChaseSystem.isTracked(this)) this.discard();
            return;
        }

        lifetimeTicks++;
        if (lifetimeTicks > MAX_LIFETIME) {
            triggerDisappearance();
            return;
        }

        // Check line of sight and crosshair gaze from nearby players
        PlayerEntity nearestPlayer = this.getWorld().getClosestPlayer(this, 32.0);
        if (nearestPlayer instanceof ServerPlayerEntity player) {
            if (isPlayerLookingDirectlyAt(player)) {
                // Kích hoạt hiệu ứng Reality Glitch & Subliminal trên shader
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.DARKNESS,
                        35, // 1.75s per gaze tick
                        0,
                        false,
                        false,
                        false
                ));

                gazeTicks++;
                if (gazeTicks >= 4) { // Gaze sustained for 200ms
                    triggerKarmicRetribution(player);
                }
            } else {
                gazeTicks = Math.max(0, gazeTicks - 1);
            }
        }
    }

    /**
     * Calculates whether the player's crosshair is aimed directly at this entity.
     */
    private boolean isPlayerLookingDirectlyAt(PlayerEntity player) {
        Vec3d viewVec = player.getRotationVec(1.0f).normalize();
        Vec3d targetVec = this.getEyePos().subtract(player.getEyePos()).normalize();

        double dot = viewVec.dotProduct(targetVec);
        // dot > 0.985 corresponds to approximately within ~10 degrees of crosshair center
        if (dot > 0.985) {
            return player.canSee(this);
        }
        return false;
    }

    private void triggerKarmicRetribution(ServerPlayerEntity player) {
        this.setHandsRaised(true);

        if (this.getWorld() instanceof ServerWorld serverWorld) {
            // Play reversed cave sound
            serverWorld.playSound(
                    null,
                    this.getX(), this.getY(), this.getZ(),
                    SamsaraSounds.CAVE_REVERSED,
                    SoundCategory.HOSTILE,
                    1.2f,
                    0.6f
            );

            // Flash of void particles
            serverWorld.spawnParticles(
                    ParticleTypes.REVERSE_PORTAL,
                    this.getX(), this.getY() + 1.0, this.getZ(),
                    80,
                    0.4, 0.8, 0.4,
                    0.15
            );
            serverWorld.spawnParticles(
                    ParticleTypes.SQUID_INK,
                    this.getX(), this.getY() + 1.0, this.getZ(),
                    30,
                    0.3, 0.6, 0.3,
                    0.05
            );

            // Unblockable 1 heart (2.0f) damage
            player.damage(player.getDamageSources().magic(), 2.0f);

            // Cơn chấn động thực tại dữ dội (Heavy 2.5s Reality Glitch shockwave)
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.DARKNESS,
                    50,
                    0,
                    false,
                    false,
                    false
            ));
        }

        this.discard();
    }

    private void triggerDisappearance() {
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                    ParticleTypes.SMOKE,
                    this.getX(), this.getY() + 1.0, this.getZ(),
                    20,
                    0.3, 0.5, 0.3,
                    0.02
            );
        }
        this.discard();
    }
}
