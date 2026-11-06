package com.samsara.system.womb.chapters;

import com.samsara.block.MemoryDoorBlock;
import com.samsara.block.MemoryDoorGiantBlock;
import com.samsara.entity.EchoDummyEntity;
import com.samsara.registry.SamsaraBlocks;
import com.samsara.registry.SamsaraEntities;
import com.samsara.system.cycle.NamGhostSystem;
import com.samsara.system.womb.WombRoute;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LightBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.List;

/**
 * Căn phòng cửa: Bến Đò (chương I) và Những Cánh Cửa (chương II, "mỗi cánh cửa là một kiếp").
 *
 * Toàn bộ nằm trong một căn phòng khổng lồ bằng Vách Hư Không đen tuyền (SaveDoorLayout): người mới tới chỉ thấy
 * bóng tối và một biển cửa giống hệt nhau đứng trơ trọi trên mặt nước nông, thẳng hàng tới tận chỗ tối hẳn. Cửa
 * panel màu phấn hồng / xám xanh / trắng (MemoryDoorBlock), không bậc thang, không biển tên, quay mặt về phía nam.
 * Thỉnh thoảng một cánh hé mở, phía sau chẳng có gì. Ánh sáng mạnh gần tuyến, tắt dần ra xa.
 * Tên save file chỉ hiện qua máy quay, thành nhãn VHS (client/womb/SaveDoorLabels).
 * Lối ra duy nhất: cánh cửa trắng khổng lồ hé mở trong tường bắc (World1, 2009), sau khe cửa là màn sáng; ánh sáng
 * tràn ra thành vệt trên mặt nước. Bước qua mới thấy vũ trụ, Gargantua và Steve 2009.
 * Qua máy quay, vài cánh cửa có bóng Nam_1998 đứng chờ.
 *
 * Phải dựng TRƯỚC DockChapter (phần dọn phòng xoá mọi thứ trên mặt nước, trừ hành lang tuyến).
 */
public final class SaveDoorChapter {
    private SaveDoorChapter() {}

    public static final String GHOST_TAG = "samsara_door_ghost";

    /** Dọn thêm phía bắc tường (cửa kiểu cũ từng dựng tới đây), chừa khu Steve 2009. */
    private static final int LEGACY_CLEAR_NORTH = 120;

    public static void build(ServerWorld world, BlockPos origin) {
        Random random = Random.create(origin.asLong() ^ 0x5A7E_D00DL);
        removeGhosts(world, origin);
        clearRoom(world, origin);
        buildRoom(world, origin);

        BlockState[] colors = {
                SamsaraBlocks.MEMORY_DOOR_PINK.getDefaultState(),
                SamsaraBlocks.MEMORY_DOOR_BLUE.getDefaultState(),
                SamsaraBlocks.MEMORY_DOOR_WHITE.getDefaultState()};

        for (int along = SaveDoorLayout.FIELD_FIRST; along <= SaveDoorLayout.FIELD_LAST; along += SaveDoorLayout.ROW) {
            for (int lateral = SaveDoorLayout.INNER; lateral <= SaveDoorLayout.OUTER; lateral += SaveDoorLayout.COL) {
                for (int side = -1; side <= 1; side += 2) {
                    int dx = side * lateral;
                    if (!SaveDoorLayout.isDoorCell(along, dx)) continue;
                    boolean named = SaveDoorLayout.isNamed(along, dx);
                    // Vài ô trống lẻ tẻ, thưa hơn chút ở mép xa
                    float gap = 0.03f + MathHelper.clamp((lateral - 60) / 35.0f, 0.0f, 1.0f) * 0.25f;
                    if (!named && random.nextFloat() < gap) continue;

                    float c = random.nextFloat();
                    BlockState door = c < 0.45f ? colors[0] : c < 0.8f ? colors[1] : colors[2];
                    float a = random.nextFloat();
                    int ajar = a < 0.01f ? 2 : a < 0.05f ? 1 : 0;
                    placeDoor(world, SaveDoorLayout.doorPos(origin, along, dx), door.with(MemoryDoorBlock.AJAR, ajar));
                }
            }
        }

        // Ánh sáng: sáng gần tuyến, tắt dần ra xa để các hàng cửa chìm vào bóng tối (block ánh sáng vô hình)
        for (int along = SaveDoorLayout.ROOM_SOUTH + 3; along < SaveDoorLayout.ROOM_NORTH - 1; along += 6) {
            for (int dx = -SaveDoorLayout.OUTER; dx <= SaveDoorLayout.OUTER; dx += 6) {
                int level = MathHelper.clamp(13 - Math.max(0, Math.abs(dx) - 10) / 5, 0, 13);
                if (level <= 0) continue;
                BlockPos p = new BlockPos(origin.getX() + dx + 1, WombRoute.SURFACE_Y + 3, origin.getZ() - along - 1);
                if (world.getBlockState(p).isAir()) {
                    world.setBlockState(p, Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, level), 2);
                }
            }
        }

        buildGiantDoor(world, origin);

        // Bóng Nam_1998 đứng chờ trước vài cánh cửa có tên (chỉ thấy qua máy quay)
        List<SaveDoorLayout.NamedDoor> named = SaveDoorLayout.named(origin);
        for (int i = 3; i < named.size(); i += 4) spawnGhost(world, named.get(i).pos());
    }

    private static void placeDoor(ServerWorld world, BlockPos base, BlockState door) {
        door = door.with(MemoryDoorBlock.FACING, Direction.SOUTH);
        world.setBlockState(base, door.with(MemoryDoorBlock.HALF, DoubleBlockHalf.LOWER), 2);
        world.setBlockState(base.up(), door.with(MemoryDoorBlock.HALF, DoubleBlockHalf.UPPER), 2);
    }

    /** Tường bốn phía (từ mặt nước tới trần) và trần bằng Vách Hư Không; chừa lỗ cho cánh cửa khổng lồ ở tường bắc. */
    private static void buildRoom(ServerWorld world, BlockPos origin) {
        BlockState veil = SamsaraBlocks.WOMB_VEIL.getDefaultState();
        BlockPos.Mutable m = new BlockPos.Mutable();
        int water = WombRoute.SURFACE_Y - 1;
        int ceiling = WombRoute.SURFACE_Y + SaveDoorLayout.ROOM_HEIGHT;
        int hw = SaveDoorLayout.ROOM_HALF_WIDTH;
        int zSouth = origin.getZ() - SaveDoorLayout.ROOM_SOUTH;
        int zNorth = origin.getZ() - SaveDoorLayout.ROOM_NORTH;
        int x0 = origin.getX() - hw;
        int x1 = origin.getX() + hw;

        for (int y = water; y <= ceiling; y++) {
            for (int x = x0; x <= x1; x++) {
                world.setBlockState(m.set(x, y, zSouth), veil, 2);
                boolean gate = Math.abs(x - origin.getX()) <= SaveDoorLayout.GATE_HALF_WIDTH
                        && y < WombRoute.SURFACE_Y + SaveDoorLayout.GATE_HEIGHT;
                if (!gate) world.setBlockState(m.set(x, y, zNorth), veil, 2);
            }
            for (int z = zNorth; z <= zSouth; z++) {
                world.setBlockState(m.set(x0, y, z), veil, 2);
                world.setBlockState(m.set(x1, y, z), veil, 2);
            }
        }
        for (int x = x0; x <= x1; x++) {
            for (int z = zNorth; z <= zSouth; z++) world.setBlockState(m.set(x, ceiling, z), veil, 2);
        }
    }

    /** Cánh cửa trắng khổng lồ + vệt sáng dưới mặt nước kéo về phía người chơi tới. */
    private static void buildGiantDoor(ServerWorld world, BlockPos origin) {
        BlockPos base = SaveDoorLayout.giant(origin);
        world.setBlockState(base, SamsaraBlocks.MEMORY_DOOR_GIANT.getDefaultState()
                .with(MemoryDoorGiantBlock.FACING, Direction.SOUTH), 3);

        // Vệt phản chiếu: đèn biển ở đáy (y = 60) dưới lớp nước nông, rộng 3 ở chân cửa rồi thu lại còn 1
        BlockState lamp = Blocks.SEA_LANTERN.getDefaultState();
        int floor = WombRoute.SURFACE_Y - 2;
        for (int dz = 1; dz <= 26; dz++) {
            int half = dz < 10 ? 1 : 0;
            for (int dx = -half; dx <= half; dx++) {
                if (dz > 15 && (dz % 3 == 0)) continue; // đuôi vệt đứt quãng như ánh sáng vỡ trên sóng
                world.setBlockState(base.add(dx, floor - base.getY(), dz), lamp, 2);
            }
        }

        // Ánh sáng tràn vào phòng từ khe cửa
        BlockState bright = Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, 15);
        for (int y = 2; y <= 12; y += 5) {
            for (int dx = -2; dx <= 2; dx += 2) world.setBlockState(base.add(dx, y, 1), bright, 2);
        }
    }

    private static void spawnGhost(ServerWorld world, BlockPos doorBase) {
        EchoDummyEntity ghost = SamsaraEntities.ECHO_DUMMY.create(world);
        if (ghost == null) return;
        float yaw = Direction.SOUTH.asRotation();
        ghost.refreshPositionAndAngles(doorBase.getX() + 0.5, doorBase.getY(), doorBase.getZ() + 1.6, yaw, 0.0f);
        ghost.setHeadYaw(yaw);
        ghost.setBodyYaw(yaw);
        ghost.setNoGravity(true);
        ghost.setPlayerUuid(NamGhostSystem.NAM_UUID);
        ghost.setPermanent(true);
        ghost.addCommandTag(GHOST_TAG);
        world.spawnEntity(ghost);
    }

    /**
     * Dọn sạch mọi thứ trên mặt nước trong phòng (trừ hành lang tuyến, WombRouteManager đã lo), vá nước nông và đáy.
     * Dọn thêm một dải phía bắc tường tới {@link #LEGACY_CLEAR_NORTH} (cửa kiểu cũ), chừa khu Steve 2009.
     */
    private static void clearRoom(ServerWorld world, BlockPos origin) {
        BlockState air = Blocks.AIR.getDefaultState();
        BlockState water = Blocks.WATER.getDefaultState();
        BlockState floor = Blocks.BLACKSTONE.getDefaultState();
        BlockPos.Mutable m = new BlockPos.Mutable();
        int surface = WombRoute.SURFACE_Y;
        int hw = SaveDoorLayout.ROOM_HALF_WIDTH;
        for (int along = SaveDoorLayout.ROOM_SOUTH; along <= LEGACY_CLEAR_NORTH; along++) {
            int z = origin.getZ() - along;
            boolean outside = along > SaveDoorLayout.ROOM_NORTH;
            int height = outside ? 34 : SaveDoorLayout.ROOM_HEIGHT + 1;
            for (int dx = -hw; dx <= hw; dx++) {
                if (Math.abs(dx) <= 4) continue;
                if (outside && (Math.abs(dx) > 66 || (along >= 96 && dx >= 6 && dx <= 34))) continue;
                int x = origin.getX() + dx;
                for (int y = surface; y < surface + height; y++) {
                    if (!world.getBlockState(m.set(x, y, z)).isAir()) world.setBlockState(m, air, 2);
                }
                if (!world.getFluidState(m.set(x, surface - 1, z)).isStill()) world.setBlockState(m, water, 2);
                if (world.getBlockState(m.set(x, surface - 2, z)).isAir() || !world.getFluidState(m).isEmpty()) {
                    world.setBlockState(m, floor, 2);
                }
            }
        }
        // Hành lang tuyến trong phòng: chỉ dọn phần trên cao (WombRouteManager dọn tới 80 block; trần ở 40 nên đủ)
    }

    /** Điểm trên tuyến cách Bến Đò {@code distance} block (đo dọc tuyến). */
    public static Vec3d pointAlong(BlockPos origin, double distance) {
        return WombRoute.pointAlong(origin, distance);
    }

    private static void removeGhosts(ServerWorld world, BlockPos origin) {
        int hw = SaveDoorLayout.ROOM_HALF_WIDTH + 4;
        Box area = new Box(origin.getX() - hw, WombRoute.SURFACE_Y - 4, origin.getZ() - LEGACY_CLEAR_NORTH,
                origin.getX() + hw, WombRoute.SURFACE_Y + 44, origin.getZ() - SaveDoorLayout.ROOM_SOUTH + 4);
        world.getEntitiesByClass(EchoDummyEntity.class, area, e -> e.getCommandTags().contains(GHOST_TAG))
                .forEach(EchoDummyEntity::discard);
    }
}
