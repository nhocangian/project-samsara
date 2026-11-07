package com.samsara.system.womb.chapters;

import com.samsara.block.DockPropBlock;
import com.samsara.block.NamWallpaperBlock;
import com.samsara.registry.SamsaraBlocks;
import com.samsara.system.womb.WombRoute;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CandleBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

/**
 * Chương I - Bến Đò, "nơi bạn thức dậy" (nơi người chơi bước vào The Womb, ngay sau giấc ngủ thứ 7).
 * Bản 07/11/2026: phòng ngủ của Nam đang tan ra trên mặt nước, giữa biển cửa.
 *  - Gốc bến là một mảnh sàn phòng ngủ mép vỡ, một góc đã chìm: chiếc giường đỏ như vừa có người nằm, tủ đầu giường có nến,
 *    bàn học với máy tính của Nam (màn hình là ảnh chụp mặt trời có chấm đen), ghế, kệ sách, tấm thảm. Tường chỉ còn
 *    vài mảng thấp (giấy dán tường, một mảng còn treo ảnh hai người, một mảng còn vết nắng của tấm poster đã gỡ).
 *  - Giữa phòng và bến gỗ là cánh cửa phòng ngủ (cửa gỗ vân sam như nhà Nam) đứng trơ trọi trong khung, đã mở: cánh cửa
 *    đầu tiên. Cả tuyến khép lại bằng cánh cửa cuối tự tay mở ở chương IV.
 *  - Dấu chân ướt đi từ cạnh giường, qua cửa, dọc bến, tới chỗ thuyền.
 *  - Bến gỗ mục dài, cột đèn lồng ấm hai bên; thuyền gỗ sồi tối buộc ở đầu bến (WombCurrentSystem giữ luôn có thuyền).
 *  - Các mảng tường khác và đồ đạc trong phòng trôi lơ lửng, xoay chậm về phía cánh cửa trắng (DockDrift, thực thể hiển thị).
 * Người chơi xuất hiện ở gốc tuyến (0, 0), ngay trong phòng, nhìn về bắc. Bến nằm trong khoảng trống quanh Bến Đò của
 * biển cửa (SaveDoorLayout: |dx| <= 8, along -12..12); cửa có tên gần nhất ở dx ±6 từ along 0 về bắc.
 */
public final class DockChapter {
    private DockChapter() {}

    /** Mặt sàn (đỉnh ván) = mặt nước, chân người chơi đứng ở y = DECK_Y + 1. */
    private static final int DECK_Y = WombRoute.SURFACE_Y - 1;
    private static final int FLOOR = DECK_Y + 1;          // tầng đặt đồ đạc
    private static final int DOOR_Z = -2;                  // cánh cửa phòng ngủ
    private static final int NORTH_END = -16;              // đầu bến (chỗ buộc thuyền)

    /** Chỗ buộc thuyền (đầu bắc của bến). */
    public static Vec3d mooring(BlockPos origin) {
        return new Vec3d(origin.getX() + 0.5, WombRoute.SURFACE_Y - 0.4, origin.getZ() + NORTH_END - 2.5);
    }

    public static void build(ServerWorld world, BlockPos origin) {
        Random random = Random.create(origin.asLong() ^ 0xD0C4L);
        BlockPos o = origin.withY(0);
        clearAbove(world, o);
        buildRoomFloor(world, o, random);
        buildFurniture(world, o);
        buildWallRemnants(world, o, random);
        buildBedroomDoor(world, o);
        buildPier(world, o, random);
        buildFootprints(world, o);
        buildCodePatches(world, o);
        DockDrift.build(world, origin);   // mảng tường + đồ đạc trôi lơ lửng (thực thể hiển thị)
        ensureBoat(world, origin);
    }

    // ==========================================
    // PHÒNG NGỦ
    // ==========================================

    private static void clearAbove(ServerWorld world, BlockPos o) {
        // Đồ của bản trước nằm ngang mặt nước (giường chìm, ván bến cũ ngoài mảnh sàn mới): trả lại nước
        for (int x = -7; x <= 7; x++) {
            for (int z = NORTH_END - 3; z <= 10; z++) {
                BlockState st = world.getBlockState(o.add(x, DECK_Y, z));
                if (st.getBlock() instanceof BedBlock || st.isOf(Blocks.BARREL)) {
                    world.setBlockState(o.add(x, DECK_Y, z), Blocks.WATER.getDefaultState(), 2);
                }
            }
        }
        // Mảnh đất cỏ Overworld lơ lửng của bản đầu và mảng tường dựng bằng block của bản trước (giờ là thực thể
        // hiển thị trong DockDrift): dọn cả vùng trời quanh bến
        java.util.Set<net.minecraft.block.Block> old = java.util.Set.of(Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.STONE,
                Blocks.POPPY, Blocks.DANDELION, Blocks.GRASS, Blocks.OAK_SAPLING, Blocks.TORCH, Blocks.CRAFTING_TABLE,
                Blocks.GLASS_PANE, SamsaraBlocks.NAM_WALLPAPER);
        for (int x = -40; x <= 40; x++) {
            for (int z = -70; z <= 40; z++) {
                for (int y = FLOOR + 4; y <= FLOOR + 30; y++) {
                    if (old.contains(world.getBlockState(o.add(x, y, z)).getBlock())) {
                        world.setBlockState(o.add(x, y, z), Blocks.AIR.getDefaultState(), 2);
                    }
                }
            }
        }
        // Thuyền trống của bản trước có thể nằm kẹt trên mặt bến mới
        world.getEntitiesByClass(BoatEntity.class, new Box(o.getX() - 4, DECK_Y - 1, o.getZ() + NORTH_END + 1,
                o.getX() + 5, DECK_Y + 4, o.getZ() + 11), b -> !b.hasPassengers()).forEach(BoatEntity::discard);
        // |x| <= 5: cửa có tên của biển cửa đứng ở dx ±6; giữ block ánh sáng của căn phòng
        for (int x = -5; x <= 5; x++) {
            for (int z = NORTH_END - 1; z <= 10; z++) {
                for (int y = FLOOR; y <= FLOOR + 6; y++) {
                    if (!world.getBlockState(o.add(x, y, z)).isOf(Blocks.LIGHT)) {
                        world.setBlockState(o.add(x, y, z), Blocks.AIR.getDefaultState(), 2);
                    }
                }
            }
        }
    }

    /** Mảnh sàn ván sồi mép vỡ nham nhở; góc đông nam đã chìm một block dưới mặt nước. */
    private static void buildRoomFloor(ServerWorld world, BlockPos o, Random random) {
        BlockState plank = Blocks.OAK_PLANKS.getDefaultState();
        for (int x = -6; x <= 6; x++) {
            for (int z = -1; z <= 9; z++) {
                double nx = x / 5.6, nz = (z - 4) / 5.2;
                double d = nx * nx + nz * nz;
                if (d > 1.0 - random.nextDouble() * 0.22) continue;
                boolean sunk = x + (z - 4) > 5;
                world.setBlockState(o.add(x, sunk ? DECK_Y - 1 : DECK_Y, z), plank, 2);
                if (sunk) world.setBlockState(o.add(x, DECK_Y, z), Blocks.WATER.getDefaultState(), 2);
            }
        }
        // Sàn luôn liền dưới đồ đạc và lối từ giường ra cửa
        for (int z = -1; z <= 6; z++) {
            for (int x = -4; x <= 4; x++) {
                if (x + (z - 4) <= 5) world.setBlockState(o.add(x, DECK_Y, z), plank, 2);
            }
        }
    }

    private static void buildFurniture(ServerWorld world, BlockPos o) {
        // Giường đỏ, đầu giường về bắc (như vừa có người nằm dậy đi ra cửa)
        BlockState bed = Blocks.RED_BED.getDefaultState().with(BedBlock.FACING, Direction.NORTH);
        world.setBlockState(o.add(-3, FLOOR, 2), bed.with(BedBlock.PART, BedPart.HEAD), 2);
        world.setBlockState(o.add(-3, FLOOR, 3), bed.with(BedBlock.PART, BedPart.FOOT), 2);
        // Tủ đầu giường + nến
        world.setBlockState(o.add(-4, FLOOR, 2), Blocks.SPRUCE_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.TOP), 2);
        world.setBlockState(o.add(-4, FLOOR + 1, 2), Blocks.WHITE_CANDLE.getDefaultState()
                .with(CandleBlock.CANDLES, 1).with(CandleBlock.LIT, true), 2);
        // Thảm nâu giữa phòng
        for (int x = 0; x <= 2; x++) {
            for (int z = 3; z <= 5; z++) world.setBlockState(o.add(x, FLOOR, z), Blocks.BROWN_CARPET.getDefaultState(), 2);
        }
        // Bàn học + máy tính quay vào phòng + ghế
        BlockState desk = Blocks.SPRUCE_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.TOP);
        world.setBlockState(o.add(3, FLOOR, 0), desk, 2);
        world.setBlockState(o.add(3, FLOOR, 1), desk, 2);
        world.setBlockState(o.add(3, FLOOR + 1, 0), SamsaraBlocks.NAM_COMPUTER.getDefaultState()
                .with(DockPropBlock.FACING, Direction.WEST), 2);
        world.setBlockState(o.add(2, FLOOR, 0), Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.WEST), 2);
        // Kệ sách
        world.setBlockState(o.add(4, FLOOR, 3), Blocks.BOOKSHELF.getDefaultState(), 2);
        world.setBlockState(o.add(4, FLOOR + 1, 3), Blocks.BOOKSHELF.getDefaultState(), 2);
        world.setBlockState(o.add(4, FLOOR, 4), Blocks.BOOKSHELF.getDefaultState(), 2);
    }

    /** Tường chỉ còn vài mảng thấp, mặt giấy dán tường quay vào trong phòng. */
    private static void buildWallRemnants(ServerWorld world, BlockPos o, Random random) {
        BlockState wall = SamsaraBlocks.NAM_WALLPAPER.getDefaultState();
        // Tường nam (mặt trong quay về bắc), có vết poster
        for (int x = -4; x <= 2; x++) {
            int h = 1 + random.nextInt(3);
            for (int y = 0; y < h; y++) {
                int v = x == -1 && y == 1 ? 2 : 0;
                world.setBlockState(o.add(x, FLOOR + y, 8), wall.with(NamWallpaperBlock.FACING, Direction.NORTH)
                        .with(NamWallpaperBlock.VARIANT, v), 2);
            }
        }
        // Tường tây (mặt trong quay về đông), có ảnh treo
        for (int z = 1; z <= 6; z++) {
            int h = z == 4 ? 3 : 1 + random.nextInt(2);
            for (int y = 0; y < h; y++) {
                int v = z == 4 && y == 1 ? 1 : 0;
                world.setBlockState(o.add(-5, FLOOR + y, z), wall.with(NamWallpaperBlock.FACING, Direction.EAST)
                        .with(NamWallpaperBlock.VARIANT, v), 2);
            }
        }
        // Một mảng tường đông sau kệ sách (mặt trong quay về tây)
        for (int z = 2; z <= 4; z++) {
            world.setBlockState(o.add(5, DECK_Y, z), Blocks.OAK_PLANKS.getDefaultState(), 2);
            world.setBlockState(o.add(5, FLOOR, z), wall.with(NamWallpaperBlock.FACING, Direction.WEST), 2);
        }
    }

    /** Cánh cửa phòng ngủ đứng trơ trọi trong khung gỗ vân sam, đã mở ra phía bến. */
    private static void buildBedroomDoor(ServerWorld world, BlockPos o) {
        BlockState post = Blocks.STRIPPED_SPRUCE_LOG.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Y);
        BlockState lintel = Blocks.STRIPPED_SPRUCE_LOG.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.X);
        for (int y = 0; y < 3; y++) {
            world.setBlockState(o.add(-1, FLOOR + y, DOOR_Z), post, 2);
            world.setBlockState(o.add(1, FLOOR + y, DOOR_Z), post, 2);
        }
        for (int x = -1; x <= 1; x++) world.setBlockState(o.add(x, FLOOR + 2, DOOR_Z), lintel, 2);
        // Vài viên tường còn bám hai bên khung
        BlockState wall = SamsaraBlocks.NAM_WALLPAPER.getDefaultState().with(NamWallpaperBlock.FACING, Direction.SOUTH);
        world.setBlockState(o.add(-2, FLOOR, DOOR_Z), wall, 2);
        world.setBlockState(o.add(2, FLOOR, DOOR_Z), wall, 2);
        world.setBlockState(o.add(2, FLOOR + 1, DOOR_Z), wall, 2);

        BlockState door = Blocks.SPRUCE_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.NORTH)
                .with(DoorBlock.OPEN, true).with(DoorBlock.HINGE, DoorHinge.LEFT);
        world.setBlockState(o.add(0, FLOOR, DOOR_Z), door.with(DoorBlock.HALF, DoubleBlockHalf.LOWER), 2);
        world.setBlockState(o.add(0, FLOOR + 1, DOOR_Z), door.with(DoorBlock.HALF, DoubleBlockHalf.UPPER), 2);
        for (int x = -1; x <= 1; x++) world.setBlockState(o.add(x, DECK_Y, DOOR_Z), Blocks.SPRUCE_PLANKS.getDefaultState(), 2);
    }

    // ==========================================
    // BẾN GỖ
    // ==========================================

    private static void buildPier(ServerWorld world, BlockPos o, Random random) {
        BlockState[] boards = {Blocks.SPRUCE_PLANKS.getDefaultState(), Blocks.SPRUCE_PLANKS.getDefaultState(),
                Blocks.OAK_PLANKS.getDefaultState(), Blocks.DARK_OAK_PLANKS.getDefaultState()};
        BlockState rail = Blocks.STRIPPED_SPRUCE_LOG.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Z);
        BlockState pile = Blocks.SPRUCE_LOG.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Y);
        BlockState post = Blocks.SPRUCE_FENCE.getDefaultState();
        BlockState lantern = Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, false);
        for (int z = DOOR_Z - 1; z >= NORTH_END; z--) {
            for (int x = -2; x <= 2; x++) {
                // Ván mục: thỉnh thoảng thiếu một tấm ở mép (không thiếu ở lối đi giữa)
                if (Math.abs(x) == 1 && random.nextFloat() < 0.08f) continue;
                world.setBlockState(o.add(x, DECK_Y, z), Math.abs(x) == 2 ? rail : boards[random.nextInt(boards.length)], 2);
            }
            if ((z - NORTH_END) % 4 == 0) {
                for (int x : new int[]{-2, 2}) {
                    for (int y = DECK_Y - 2; y < DECK_Y; y++) world.setBlockState(o.add(x, y, z), pile, 2);
                    world.setBlockState(o.add(x, FLOOR, z), post, 2);
                    world.setBlockState(o.add(x, FLOOR + 1, z), post, 2);
                    world.setBlockState(o.add(x, FLOOR + 2, z), lantern, 2);
                }
            }
        }
        // Cọc buộc thuyền ở đầu bến
        world.setBlockState(o.add(0, FLOOR, NORTH_END), post, 2);
    }

    /** Dấu chân ướt từ cạnh giường, qua cửa, dọc bến, tới chỗ thuyền. */
    private static void buildFootprints(ServerWorld world, BlockPos o) {
        BlockState prints = SamsaraBlocks.WET_FOOTPRINTS.getDefaultState().with(DockPropBlock.FACING, Direction.NORTH);
        int[][] steps = {{-2, 3}, {-2, 1}, {-1, 0}, {0, -1}};
        for (int[] s : steps) world.setBlockState(o.add(s[0], FLOOR, s[1]), prints, 2);
        for (int z = DOOR_Z - 1; z > NORTH_END; z -= 2) {
            BlockPos p = o.add(0, FLOOR, z);
            if (world.getBlockState(p.down()).isOpaque()) world.setBlockState(p, prints, 2);
        }
    }

    /**
     * Vài chỗ căn phòng đã rã thành mã (Khối Mã, các khối cạnh nhau tự nối liền): một mảng sàn ở mép vỡ phía đông nam,
     * một khoảng tường nam, góc tường tây. Chỉ thay những block đang có (sàn mép vỡ, tường cao ngẫu nhiên).
     */
    private static void buildCodePatches(ServerWorld world, BlockPos o) {
        BlockState code = SamsaraBlocks.WOMB_CODE.getDefaultState();
        int[][] cells = {
                // sàn (y = DECK_Y)
                {3, DECK_Y, 5}, {4, DECK_Y, 5}, {2, DECK_Y, 6}, {3, DECK_Y, 6}, {2, DECK_Y, 7}, {1, DECK_Y, 7},
                // tường nam (z 8): một khoảng tường đã thành mã
                {0, FLOOR, 8}, {1, FLOOR, 8}, {2, FLOOR, 8}, {1, FLOOR + 1, 8}, {2, FLOOR + 1, 8}, {1, FLOOR + 2, 8},
                // góc tường tây
                {-5, FLOOR, 6}, {-5, FLOOR + 1, 6}, {-5, FLOOR, 5},
        };
        for (int[] c : cells) {
            BlockPos p = o.add(c[0], c[1], c[2]);
            BlockState st = world.getBlockState(p);
            boolean wallTop = c[1] > FLOOR && world.getBlockState(p.down()).isOf(SamsaraBlocks.WOMB_CODE);
            if (st.isAir() && !wallTop) continue;             // không treo mã giữa không trung
            if (st.isOf(Blocks.WATER)) continue;
            if (world.getBlockState(p.up()).isOf(Blocks.BROWN_CARPET)) continue;
            world.setBlockState(p, com.samsara.block.WombCodeBlock.at(code, p), 2);
        }
        // Viền mã tan vào block bên cạnh: tính sau khi cả mảng đã đặt
        for (int[] c : cells) com.samsara.block.WombCodeBlock.refresh(world, o.add(c[0], c[1], c[2]));
    }

    /** Thuyền gỗ sồi tối buộc ở đầu bến; không tạo thêm nếu đã có thuyền gần đó. */
    public static void ensureBoat(ServerWorld world, BlockPos origin) {
        Vec3d m = mooring(origin);
        Box area = new Box(m, m).expand(10.0, 4.0, 10.0);
        if (!world.getEntitiesByClass(BoatEntity.class, area, e -> true).isEmpty()) return;
        BoatEntity boat = new BoatEntity(world, m.x, m.y, m.z);
        boat.setVariant(BoatEntity.Type.DARK_OAK);
        boat.setYaw(180.0f);
        world.spawnEntity(boat);
    }
}
