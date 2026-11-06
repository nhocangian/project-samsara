package com.samsara.entity;

import com.samsara.registry.SamsaraEntities;
import com.samsara.system.womb.WombTravel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Vết Rách Không Gian (Dimensional Rift Entity) theo phong cách Minecraft Legends / Dungeons.
 *  - Hình dáng phẳng zíc zắc hình học với đường viền phát sáng rực rỡ (Luminous White Neon Outline).
 *  - Có chiều sâu 3D thực thụ: khung viền 3D dày 0.36 block và 8 khối lập phương vệ tinh 3D lơ lửng quanh viền.
 *  - Không có va chạm vật lý (noClip), cho phép người chơi bước xuyên thẳng qua.
 *  - Tương tác: Bước vào trong bán kính cổng hoặc nhấp chuột phải để dịch chuyển.
 */
public class DimensionalRiftEntity extends Entity {
    public static int defaultWombStyle = 3; // 1: Silexars, 2: Glowmarcher, 3: Kishimisu (Mặc định)
    private static final TrackedData<String> RIFT_TYPE = DataTracker.registerData(DimensionalRiftEntity.class, TrackedDataHandlerRegistry.STRING);

    public DimensionalRiftEntity(EntityType<? extends DimensionalRiftEntity> type, World world) {
        super(type, world);
        this.noClip = true;
    }

    public DimensionalRiftEntity(World world, double x, double y, double z, float yaw, String riftType) {
        this(SamsaraEntities.DIMENSIONAL_RIFT, world);
        this.setPosition(x, y, z);
        this.setYaw(yaw);
        this.setRiftType(riftType);
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(RIFT_TYPE, "womb_" + defaultWombStyle);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("RiftType")) {
            setRiftType(nbt.getString("RiftType"));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putString("RiftType", getRiftType());
    }

    public String getRiftType() {
        return this.dataTracker.get(RIFT_TYPE);
    }

    public void setRiftType(String type) {
        this.dataTracker.set(RIFT_TYPE, type);
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    public boolean canHit() {
        return true; // Cho phép nhấp chuột tương tác
    }

    @Override
    public void tick() {
        super.tick();

        // 1. Server-side: quét người chơi bước vào phạm vi của vết rách
        if (!this.getWorld().isClient) {
            // Vết rách Elysian còn sót trong thế giới cũ: tan biến
            if (!getRiftType().toLowerCase().startsWith("womb")) {
                this.discard();
                return;
            }
            ServerWorld world = (ServerWorld) this.getWorld();
            long now = System.currentTimeMillis();

            // QUAN TRỌNG: Clone danh sách người chơi để triệt tiêu hoàn toàn ConcurrentModificationException
            List<ServerPlayerEntity> currentPlayers = new ArrayList<>(world.getPlayers());
            List<ServerPlayerEntity> toTeleport = new ArrayList<>();

            for (ServerPlayerEntity player : currentPlayers) {
                if (!player.isAlive()) continue;

                double dx = player.getX() - this.getX();
                double dy = player.getY() - this.getY();
                double dz = player.getZ() - this.getZ();
                // Toạ độ cục bộ theo mặt phẳng cổng (cùng phép xoay với DimensionalRiftRenderer:
                // RotationAxis.POSITIVE_Y.rotationDegrees(-yaw)). Lòng cổng rộng ±1.22, cao 0.35..4.85 block.
                // Chỉ dịch chuyển khi người chơi thật sự BƯỚC VÀO lòng cổng, không phải chỉ đứng gần.
                double theta = Math.toRadians(-this.getYaw());
                double along = dx * Math.cos(theta) - dz * Math.sin(theta);  // dọc theo bề ngang cổng
                double depth = dx * Math.sin(theta) + dz * Math.cos(theta);  // xuyên qua mặt cổng

                if (Math.abs(along) <= 1.1 && Math.abs(depth) <= 0.45 && dy >= -0.5 && dy <= 4.0) {
                    if (now - WombTravel.getCooldown(player.getUuid()) > 6000L) {
                        WombTravel.setCooldown(player.getUuid());
                        toTeleport.add(player);
                    }
                }
            }

            // Dịch chuyển ngoài vòng lặp an toàn
            for (ServerPlayerEntity p : toTeleport) {
                triggerTeleport(p);
            }

            // Âm thanh ngân vang định kỳ
            if (this.age % 60 == 0) {
                world.playSound(null, this.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.AMBIENT, 0.9f, 1.2f);
                world.playSound(null, this.getBlockPos(), SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.AMBIENT, 0.7f, 1.4f);
            }
        } else {
            // 2. Client: Chỉ thỉnh thoảng sinh hạt bụi tinh tú rất nhỏ quanh rìa ngoài cùng, tuyệt đối không che tâm ngắm
            if (this.random.nextFloat() < 0.15f) {
                double side = this.random.nextBoolean() ? 1.0 : -1.0;
                double ox = side * (2.2 + this.random.nextDouble() * 2.2);
                double oy = 0.5 + this.random.nextDouble() * 4.5;
                double oz = (this.random.nextDouble() - 0.5) * 0.4;

                float yawRad = (float) Math.toRadians(-this.getYaw());
                double rx = ox * Math.cos(yawRad) - oz * Math.sin(yawRad);
                double rz = ox * Math.sin(yawRad) + oz * Math.cos(yawRad);

                this.getWorld().addParticle(ParticleTypes.END_ROD, this.getX() + rx, this.getY() + oy, this.getZ() + rz, 0, 0.01, 0);
            }
        }
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (!this.getWorld().isClient && player instanceof ServerPlayerEntity serverPlayer) {
            long now = System.currentTimeMillis();
            if (now - WombTravel.getCooldown(serverPlayer.getUuid()) > 6000L) {
                WombTravel.setCooldown(serverPlayer.getUuid());
                triggerTeleport(serverPlayer);
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.PASS;
    }

    private void triggerTeleport(ServerPlayerEntity player) {
        String type = getRiftType();
        // Elysian Highlands đã bị gỡ: mọi vết rách đều dẫn về The Womb
        WombTravel.teleportToTheWomb(player);
    }
}
