package com.samsara.system.arg;

import com.samsara.entity.EchoDummyEntity;
import com.samsara.registry.SamsaraDimensions;
import com.samsara.registry.SamsaraEntities;
import com.samsara.registry.SamsaraItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * ARG Feature 3: Con Thuyền Tiền Kiếp (The Phantom Skiff).
 * Thỉnh thoảng xuất hiện ở khoảng cách 60 - 75 block trong làn sương mù của The Womb.
 * Một chiếc thuyền gỗ do bóng hình Echo của người chơi chèo lướt đi trong tĩnh lặng,
 * để lại vệt rẽ nước mờ ảo.
 * Khi người chơi tiếp cận trong bán kính 18 block, thuyền tan biến thành bọt nước vũ trụ,
 * để lại một chiếc rương trôi dạt (Flotsam Chest / Waterlogged Chest) chứa Tàn Thư Trùng Độc.
 */
public class WombPhantomSkiffManager {

    private static final Map<UUID, SkiffData> ACTIVE_SKIFFS = new HashMap<>();
    private static final Random RAND = new Random();
    private static int checkTimer = 0;

    private static class SkiffData {
        final UUID boatUuid;
        final UUID dummyUuid;
        final Vec3d velocity;
        final float yaw;
        int ageTicks;

        SkiffData(UUID boatUuid, UUID dummyUuid, Vec3d velocity, float yaw) {
            this.boatUuid = boatUuid;
            this.dummyUuid = dummyUuid;
            this.velocity = velocity;
            this.yaw = yaw;
            this.ageTicks = 0;
        }
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (!world.getRegistryKey().equals(SamsaraDimensions.THE_WOMB_WORLD_KEY)) {
                return;
            }

            tick(world);
        });
    }

    private static void tick(ServerWorld world) {
        checkTimer++;

        // 1. DUYỆT CÁC CON THUYỀN MA ĐANG HOẠT ĐỘNG
        Iterator<Map.Entry<UUID, SkiffData>> iterator = ACTIVE_SKIFFS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, SkiffData> entry = iterator.next();
            SkiffData data = entry.getValue();
            data.ageTicks++;

            BoatEntity boat = (BoatEntity) world.getEntity(data.boatUuid);
            EchoDummyEntity dummy = (EchoDummyEntity) world.getEntity(data.dummyUuid);

            if (boat == null || !boat.isAlive()) {
                if (dummy != null) dummy.discard();
                iterator.remove();
                continue;
            }

            // Giữ thuyền di chuyển lướt nhẹ trên mặt nước và vung mái chèo, để vật lý Y tự nhiên của thuyền nổi trên mặt nước
            boat.setVelocity(data.velocity.x, boat.getVelocity().y, data.velocity.z);
            boat.setYaw(data.yaw);
            boat.setPaddleMovings(true, true);
            boat.velocityModified = true;

            // Đảm bảo dummy luôn ngồi vững trên thuyền
            if (dummy != null && dummy.isAlive() && !dummy.hasVehicle()) {
                dummy.startRiding(boat, true);
            }

            // Tìm người chơi gần nhất
            PlayerEntity nearestPlayer = world.getClosestPlayer(boat, 150.0);
            if (nearestPlayer == null) {
                boat.discard();
                if (dummy != null) dummy.discard();
                iterator.remove();
                continue;
            }

            double dist = boat.distanceTo(nearestPlayer);

            // BẮT GẶP THUYỀN MA: Khi người chơi tiến sát lại gần trong bán kính 18 block!
            if (dist < 18.0) {
                triggerDissolution(world, boat, nearestPlayer);
                if (dummy != null) dummy.discard();
                boat.discard();
                iterator.remove();
                continue;
            }

            // Hết thời gian sống (70 giây) hoặc người chơi đi quá xa (> 130 block)
            if (data.ageTicks > 1400 || dist > 130.0) {
                boat.discard();
                if (dummy != null) dummy.discard();
                iterator.remove();
            }
        }

        // 2. ĐỊNH KỲ THỬ TRIỆU HỒI THUYỀN MA (Mỗi 10 giây kiểm tra một lần)
        if (checkTimer % 200 == 0 && ACTIVE_SKIFFS.isEmpty()) {
            List<ServerPlayerEntity> players = world.getPlayers();
            if (players.isEmpty()) return;

            // 40% cơ hội xuất hiện cho người chơi ngẫu nhiên
            if (RAND.nextFloat() < 0.40f) {
                ServerPlayerEntity targetPlayer = players.get(RAND.nextInt(players.size()));
                spawnPhantomSkiff(world, targetPlayer);
            }
        }
    }

    public static void spawnTestSkiff(ServerWorld world, ServerPlayerEntity player) {
        Vec3d look = player.getRotationVec(1.0f);
        double sx = player.getX() + look.x * 12.0;
        double sz = player.getZ() + look.z * 12.0;
        double sy = 61.45;

        // Vận tốc lướt ngang nhẹ trước tầm mắt
        double vx = -look.z * 0.10;
        double vz = look.x * 0.10;
        Vec3d velocity = new Vec3d(vx, 0.0, vz);
        float yaw = (float) Math.toDegrees(Math.atan2(-vx, vz));

        BoatEntity boat = new BoatEntity(world, sx, sy, sz);
        boat.setVariant(BoatEntity.Type.DARK_OAK);
        boat.setYaw(yaw);
        boat.setVelocity(velocity);
        boat.addCommandTag("samsara_phantom_skiff");
        world.spawnEntity(boat);

        EchoDummyEntity dummy = new EchoDummyEntity(SamsaraEntities.ECHO_DUMMY, world);
        dummy.setPosition(sx, sy, sz);
        dummy.setPermanent(true);
        dummy.setAlwaysVisible(true);
        dummy.setPlayerUuid(player.getUuid());
        world.spawnEntity(dummy);
        dummy.startRiding(boat, true);

        ACTIVE_SKIFFS.put(boat.getUuid(), new SkiffData(boat.getUuid(), dummy.getUuid(), velocity, yaw));
    }

    private static void spawnPhantomSkiff(ServerWorld world, ServerPlayerEntity player) {
        double angle = RAND.nextDouble() * Math.PI * 2.0;
        double distance = 60.0 + RAND.nextDouble() * 15.0; // 60 - 75 block trong sương mù

        double sx = player.getX() + Math.cos(angle) * distance;
        double sz = player.getZ() + Math.sin(angle) * distance;

        // Kiểm tra vị trí spawn có phải là nước cạn không
        BlockPos waterCheck = new BlockPos((int) sx, 61, (int) sz);
        if (!world.getBlockState(waterCheck).isOf(Blocks.WATER)) {
            return;
        }

        // Độ cao mớn nước nổi tự nhiên của thuyền trên mặt biển Y=61 là Y=61.45
        double sy = 61.45;

        // Vận tốc lướt ngang qua tầm nhìn của người chơi (vận tốc ~ 0.12 block/tick)
        double moveAngle = angle + (Math.PI * 0.45) + (RAND.nextDouble() * 0.1);
        double vx = Math.cos(moveAngle) * 0.12;
        double vz = Math.sin(moveAngle) * 0.12;
        Vec3d velocity = new Vec3d(vx, 0.0, vz);
        float yaw = (float) Math.toDegrees(Math.atan2(-vx, vz));

        // 1. Tạo thuyền gỗ sồi đen ở độ cao nổi chuẩn xác
        BoatEntity boat = new BoatEntity(world, sx, sy, sz);
        boat.setVariant(BoatEntity.Type.DARK_OAK);
        boat.setYaw(yaw);
        boat.setVelocity(velocity);
        boat.addCommandTag("samsara_phantom_skiff");
        world.spawnEntity(boat);

        // 2. Tạo bóng Echo Dummy ngồi trực tiếp lên thuyền
        EchoDummyEntity dummy = new EchoDummyEntity(SamsaraEntities.ECHO_DUMMY, world);
        dummy.setPosition(sx, sy, sz);
        dummy.setPermanent(true);
        dummy.setAlwaysVisible(true); // Hiển thị hình bóng mờ ảo ngay cả khi không dùng máy quay
        dummy.setPlayerUuid(player.getUuid());
        world.spawnEntity(dummy);

        dummy.startRiding(boat, true);

        ACTIVE_SKIFFS.put(boat.getUuid(), new SkiffData(boat.getUuid(), dummy.getUuid(), velocity, yaw));
    }

    private static void triggerDissolution(ServerWorld world, BoatEntity boat, PlayerEntity player) {
        BlockPos boatPos = boat.getBlockPos();

        // 1. Hiệu ứng tan biến huyền bí
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, boat.getX(), boat.getY() + 0.5, boat.getZ(), 45, 0.6, 0.4, 0.6, 0.06);
        world.spawnParticles(ParticleTypes.FLASH, boat.getX(), boat.getY() + 0.8, boat.getZ(), 2, 0.1, 0.1, 0.1, 0.0);
        world.spawnParticles(ParticleTypes.END_ROD, boat.getX(), boat.getY() + 0.5, boat.getZ(), 20, 0.4, 0.4, 0.4, 0.05);

        world.playSound(null, boatPos, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.AMBIENT, 1.2f, 0.45f);
        world.playSound(null, boatPos, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundCategory.AMBIENT, 0.8f, 1.6f);

        // 2. Đặt rương gỗ trôi dạt (Flotsam Chest / Waterlogged Chest)
        BlockPos cratePos = new BlockPos(boatPos.getX(), 61, boatPos.getZ());
        world.setBlockState(cratePos, Blocks.CHEST.getDefaultState().with(ChestBlock.WATERLOGGED, true), 3);

        BlockEntity be = world.getBlockEntity(cratePos);
        if (be instanceof ChestBlockEntity chest) {
            chest.setCustomName(Text.translatable("samsara.womb_phantom_skiff_manager.chiec_ruong_tien_kiep"));
            chest.setStack(11, createEchoCompass(world, player));
            chest.setStack(13, PalimpsestJournalItem.createDecodedBook());
            chest.setStack(15, new ItemStack(SamsaraItems.CAMCORDER_BATTERY, 2));
            chest.markDirty();
        }

        // 3. Thông báo cho người chơi
        player.sendMessage(Text.translatable("samsara.womb_phantom_skiff_manager.con_thuyen_tien_kiep"), true);
    }

    /** La Bàn Tiền Kiếp: luôn chỉ về Quả Trứng ở cuối tuyến hành hương (hoặc Bến Đò nếu tuyến chưa có). */
    public static ItemStack createEchoCompass(ServerWorld world, PlayerEntity player) {
        com.samsara.system.cycle.EggCycleState state = com.samsara.system.cycle.EggCycleState.get(world.getServer());
        BlockPos target = state.getEggAltarPos() != null ? state.getEggAltarPos()
                : (state.getRouteOrigin() != null ? state.getRouteOrigin() : com.samsara.system.womb.WombTravel.ARRIVAL);
        int convX = target.getX();
        int convZ = target.getZ();

        ItemStack compass = new ItemStack(Items.COMPASS);
        NbtCompound nbt = compass.getOrCreateNbt();
        nbt.putBoolean("LodestoneTracked", true);
        NbtCompound posNbt = new NbtCompound();
        posNbt.putInt("X", convX);
        posNbt.putInt("Y", target.getY());
        posNbt.putInt("Z", convZ);
        nbt.put("LodestonePos", posNbt);
        nbt.putString("LodestoneDimension", SamsaraDimensions.THE_WOMB_ID.toString());

        compass.setCustomName(Text.translatable("samsara.womb_phantom_skiff_manager.la_ban_tien_kiep"));

        NbtCompound display = compass.getOrCreateSubNbt("display");
        NbtList lore = new NbtList();
        lore.add(NbtString.of(Text.Serializer.toJson(Text.translatable("samsara.womb_phantom_skiff_manager.kim_la_ban_luon"))));
        lore.add(NbtString.of(Text.Serializer.toJson(Text.translatable("samsara.womb_phantom_skiff_manager.no_da_doi_cau"))));
        lore.add(NbtString.of(Text.Serializer.toJson(Text.translatable("samsara.womb_phantom_skiff.target", convX, convZ))));
        display.put("Lore", lore);

        return compass;
    }
}
