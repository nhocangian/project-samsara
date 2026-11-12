package com.samsara.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

/**
 * Tầng 3 "Trốn tìm": đứa em (model người chơi, skin mặc định) hoặc kẻ ngồi trốn (skin của chính người chơi, ngồi bó gối).
 * Không AI, không gây hại, không bị đẩy. Mắt thường không thấy; chỉ hiện qua tiếng vọng (xem EchoRenderer, EmRenderer).
 * Bước chân của nó vẫn phát tiếng, nên lúc nó chạy thì người chơi thấy nó qua từng nhịp sóng.
 */
public class EmEntity extends PathAwareEntity {
    private static final TrackedData<Boolean> HIDER = DataTracker.registerData(EmEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> CROWD = DataTracker.registerData(EmEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    public EmEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        this.setPersistent();
        this.setInvulnerable(true);
    }

    public static DefaultAttributeContainer.Builder createEmAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(HIDER, false);
        this.dataTracker.startTracking(CROWD, false);
    }

    /** Kẻ ngồi trốn trong tủ: mang skin người chơi, ngồi bó gối. */
    public boolean isHider() {
        return this.dataTracker.get(HIDER);
    }

    public void setHider(boolean hider) {
        this.dataTracker.set(HIDER, hider);
    }

    /** Đám người đứng sau lưng lúc đi ngủ: cũng mang skin người chơi, đứng yên nhìn. */
    public boolean isCrowd() {
        return this.dataTracker.get(CROWD);
    }

    public void setCrowd(boolean crowd) {
        this.dataTracker.set(CROWD, crowd);
    }

    /** Kẻ ngồi trốn và đám đông đều mang skin của người chơi đang chơi. */
    public boolean usesPlayerSkin() {
        return isHider() || isCrowd();
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("Hider", isHider());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        setHider(nbt.getBoolean("Hider"));
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushAway(Entity entity) {
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }
}
