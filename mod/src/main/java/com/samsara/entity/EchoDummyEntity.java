package com.samsara.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Arm;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

/**
 * Echo Dummy Entity: A transient visual glitch replicating past player movements.
 * Non-collidable, silent, and dissolves after exactly 3 seconds (60 ticks).
 */
public class EchoDummyEntity extends PathAwareEntity {
    private static final TrackedData<Optional<UUID>> PLAYER_UUID = DataTracker.registerData(EchoDummyEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Boolean> IS_CROUCHING = DataTracker.registerData(EchoDummyEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_SWINGING = DataTracker.registerData(EchoDummyEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_BOWING = DataTracker.registerData(EchoDummyEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> ALWAYS_VISIBLE = DataTracker.registerData(EchoDummyEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    private int ageTicks = 0;
    private boolean permanent = false;
    public static final int DURATION_TICKS = 60; // 3 seconds

    public EchoDummyEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.setSilent(true);
        this.setInvulnerable(true);
    }

    public static DefaultAttributeContainer.Builder createDummyAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(PLAYER_UUID, Optional.empty());
        this.dataTracker.startTracking(IS_CROUCHING, false);
        this.dataTracker.startTracking(IS_SWINGING, false);
        this.dataTracker.startTracking(IS_BOWING, false);
        this.dataTracker.startTracking(ALWAYS_VISIBLE, false);
    }

    public boolean isAlwaysVisible() {
        return this.dataTracker.get(ALWAYS_VISIBLE);
    }

    public void setAlwaysVisible(boolean visible) {
        this.dataTracker.set(ALWAYS_VISIBLE, visible);
    }

    public void setPermanent(boolean permanent) {
        this.permanent = permanent;
    }

    public boolean isPermanent() {
        return this.permanent;
    }

    public void setBowing(boolean bowing) {
        this.dataTracker.set(IS_BOWING, bowing);
    }

    public boolean isBowing() {
        return this.dataTracker.get(IS_BOWING);
    }

    public void setPlayerUuid(UUID uuid) {
        this.dataTracker.set(PLAYER_UUID, Optional.ofNullable(uuid));
    }

    public Optional<UUID> getPlayerUuid() {
        return this.dataTracker.get(PLAYER_UUID);
    }

    public void setCrouchingState(boolean crouching) {
        this.dataTracker.set(IS_CROUCHING, crouching);
        this.setSneaking(crouching);
    }

    public boolean isCrouchingState() {
        return this.dataTracker.get(IS_CROUCHING);
    }

    public void setSwingingState(boolean swinging) {
        this.dataTracker.set(IS_SWINGING, swinging);
    }

    public boolean isSwingingState() {
        return this.dataTracker.get(IS_SWINGING);
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushAway(Entity entity) {
        // Ghost entity; does not interact physically
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        ageTicks++;
        if (this.getWorld().isClient) {
            if (this.isSwingingState() && (this.ageTicks % 10 == 0)) {
                this.swingHand(this.preferredHand);
            }
            return;
        }

        // Bóng ma Nam_1998 sót lại sau khi server tắt giữa chừng -> tan biến
        if (this.ageTicks % 20 == 0 && com.samsara.system.cycle.NamGhostSystem.isOrphanGhost(this)) {
            this.discard();
            return;
        }

        // Tự động sao chép diện mạo người chơi gần nhất nếu chưa có UUID gán sẵn
        if (this.getPlayerUuid().isEmpty()) {
            PlayerEntity nearest = this.getWorld().getClosestPlayer(this, 16.0);
            if (nearest != null) {
                this.setPlayerUuid(nearest.getUuid());
            }
        }

        if (!this.permanent) {
            if (ageTicks >= DURATION_TICKS) {
                dissolve();
            }
        }
    }

    @Override
    public void writeCustomDataToNbt(net.minecraft.nbt.NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("Permanent", this.permanent);
        nbt.putBoolean("Bowing", this.isBowing());
        getPlayerUuid().ifPresent(uuid -> nbt.putUuid("PlayerUuid", uuid));
    }

    @Override
    public void readCustomDataFromNbt(net.minecraft.nbt.NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.permanent = nbt.getBoolean("Permanent");
        this.setBowing(nbt.getBoolean("Bowing"));
        if (nbt.containsUuid("PlayerUuid")) {
            setPlayerUuid(nbt.getUuid("PlayerUuid"));
        }
    }

    private void dissolve() {
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                    ParticleTypes.SMOKE,
                    this.getX(), this.getY() + 0.9, this.getZ(),
                    35,
                    0.25, 0.5, 0.25,
                    0.03
            );
        }
        this.discard();
    }

    @Override
    public Iterable<ItemStack> getArmorItems() {
        return Collections.emptyList();
    }

    @Override
    public ItemStack getEquippedStack(net.minecraft.entity.EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void equipStack(net.minecraft.entity.EquipmentSlot slot, ItemStack stack) {
    }

    @Override
    public Arm getMainArm() {
        return Arm.RIGHT;
    }
}
