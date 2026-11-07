package com.samsara.system.womb;

import com.samsara.entity.PrimordialEggEntity;
import com.samsara.system.cycle.EggCycleState;
import com.samsara.system.ending.EggAltarManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Dựng tuyến hành hương (WombRoute) trong The Womb, một lần cho cả thế giới:
 *  - Dọn hành lang rộng 7 block dọc tuyến: xoá mọi thứ trên mặt nước, giữ nước nông ở y = 61
 *    (phế tích sinh trước đó bị xẻ một đường như bị dòng chảy cắt qua).
 *  - Phao đèn linh hồn cắm hai bên, mỗi 24 block một cây, so le trái phải: lối đi duy nhất trong bóng tối.
 *  - Bệ Quả Trứng tạm đặt ở cuối tuyến (chặng SPIRE) cho tới khi có Tháp Singularity (3b-4).
 * WombRuinManager không sinh phế tích mới trong RUIN_CLEARANCE block quanh tuyến.
 * Lệnh test: /samsara test womb_route [here|rebuild|tp <chặng>].
 */
public final class WombRouteManager {
    private WombRouteManager() {}

    /** Phế tích ngẫu nhiên không được sinh gần tuyến hơn khoảng này. */
    public static final double RUIN_CLEARANCE = 55.0;
    /** Bán kính quanh Tháp không sinh phế tích (phế tích lớn như bộ xương sườn chìa rất xa khỏi tâm). */
    public static final double SPIRE_RUIN_CLEARANCE = 150.0;
    private static final int CORRIDOR_HALF_WIDTH = 3;
    private static final int CLEAR_HEIGHT = 80;
    private static final double BUOY_SPACING = 24.0;
    private static final int BUOY_OFFSET = 4;

    /** Gọi khi người chơi vừa tới The Womb tại {@code arrival}. */
    public static void ensureRoute(ServerWorld womb, BlockPos arrival) {
        EggCycleState state = EggCycleState.get(womb.getServer());
        if (state.getRouteOrigin() != null) return;

        BlockPos origin = new BlockPos(arrival.getX(), WombRoute.SURFACE_Y, arrival.getZ());
        BlockPos legacyAltar = state.getEggAltarPos();
        if (legacyAltar != null) {
            // Thế giới đã có bệ trứng kiểu cũ (96 block phía bắc nơi đến): lấy lại nơi đến cũ làm gốc tuyến
            origin = legacyAltar.add(0, 0, EggAltarManager.DISTANCE_NORTH).withY(WombRoute.SURFACE_Y);
            removeEggs(womb, legacyAltar);
        }
        state.setRouteOrigin(origin);
        build(womb, origin);
    }

    /** Dựng (hoặc dựng lại) toàn bộ tuyến từ {@code origin}. */
    public static void build(ServerWorld world, BlockPos origin) {
        // Lấy mẫu mỗi 3 block: hành lang rộng 7 nên các ô dọn vẫn liền nhau
        List<Vec3d> path = WombRoute.sample(origin, 3.0);
        loadChunksAlong(world, path);
        clearBeyondRoom(world, origin);
        clearCorridor(world, path);
        placeBuoys(world, origin);

        // Kể chuyện bằng hình ảnh dọc tuyến
        // Căn phòng cửa trước (nó dọn sạch phòng), rồi mới tới Bến Đò bên trong
        com.samsara.system.womb.chapters.SaveDoorChapter.build(world, origin);
        com.samsara.system.womb.chapters.DockChapter.build(world, origin);
        com.samsara.system.womb.chapters.TitanChapter.build(world, origin);

        // Cuối tuyến: cầu thang xoắn, cánh cửa cuối, hồ gương lơ lửng với Quả Trứng, tháp ngược
        com.samsara.system.womb.chapters.SpireChapter.build(world, origin);

        // Client cần gốc tuyến cho dòng chảy & thuyền bóng ma
        WombCurrentSystem.syncAll(world.getServer());
    }

    /** Nửa bề ngang và chiều cao của dải dọn phía sau căn phòng cửa. */
    private static final int BEYOND_HALF_WIDTH = 42;
    /** Dải lát lại đáy (rộng hơn dải dọn, để phủ hết vùng nhìn thấy quanh khổng thần và tượng quỳ). */
    private static final int FLOOR_HALF_WIDTH = 90;
    /** Đáy biển The Womb: một loại đá duy nhất (khớp surface_rule trong worldgen/noise_settings/the_womb.json). */
    public static final BlockState FLOOR = Blocks.POLISHED_BLACKSTONE.getDefaultState();
    private static final int BEYOND_HEIGHT = 70;
    /** Vị trí cũ (trước 07/11/2026, các chặng lùi 50 block) của Hố Nổ và Tháp, tính theo gốc tuyến. */
    private static final int LEGACY_CRATER_DX = 8, LEGACY_CRATER_DZ = -200, LEGACY_CRATER_RADIUS = 15;
    private static final int LEGACY_SPIRE_DZ = -400, LEGACY_SPIRE_RADIUS = 40, LEGACY_SPIRE_TOP = 216;

    /**
     * Dọn sạch dải hai bên tuyến từ sau tường căn phòng cửa tới cuối tuyến (khổng thần, tinh thể, tháp của lần dựng
     * trước, kể cả ở vị trí cũ trước khi các chặng bị lùi), vá mặt nước và đáy; lấp lại lòng Hố Nổ cũ và dọn cột
     * Tháp cũ tới tận đỉnh. Giữ phao đèn. Mọi chương sau đó dựng lại lên vùng sạch.
     */
    private static void clearBeyondRoom(ServerWorld world, BlockPos origin) {
        BlockState air = Blocks.AIR.getDefaultState();
        BlockState water = Blocks.WATER.getDefaultState();
        BlockState floor = FLOOR;
        BlockPos.Mutable m = new BlockPos.Mutable();
        int surface = WombRoute.SURFACE_Y;
        double length = WombRoute.length(origin);
        java.util.Set<Long> done = new java.util.HashSet<>();
        for (double along = com.samsara.system.womb.chapters.SaveDoorLayout.ROOM_NORTH + 1; along <= length; along += 1.0) {
            Vec3d p = WombRoute.pointAlong(origin, along);
            Vec3d t = WombRoute.tangentNear(origin, p);
            Vec3d n = new Vec3d(-t.z, 0, t.x);
            for (int lateral = -FLOOR_HALF_WIDTH; lateral <= FLOOR_HALF_WIDTH; lateral++) {
                int x = (int) Math.floor(p.x + n.x * lateral);
                int z = (int) Math.floor(p.z + n.z * lateral);
                if (!done.add(((long) x << 32) ^ (z & 0xFFFFFFFFL))) continue;
                world.getChunk(x >> 4, z >> 4);
                if (Math.abs(lateral) <= BEYOND_HALF_WIDTH) {
                    sweepColumn(world, m, x, z, surface, surface + BEYOND_HEIGHT, air, water, floor);
                } else if (!world.getBlockState(m.set(x, surface - 2, z)).isOf(FLOOR.getBlock())
                        && world.getBlockState(m.set(x, surface - 1, z)).isOf(Blocks.WATER)) {
                    world.setBlockState(m.set(x, surface - 2, z), floor, 2);   // chỉ lát đáy dưới nước
                }
            }
        }
        // Lòng Hố Nổ cũ: lấp lại thành đáy biển
        int cx = origin.getX() + LEGACY_CRATER_DX;
        int cz = origin.getZ() + LEGACY_CRATER_DZ;
        for (int dx = -LEGACY_CRATER_RADIUS; dx <= LEGACY_CRATER_RADIUS; dx++) {
            for (int dz = -LEGACY_CRATER_RADIUS; dz <= LEGACY_CRATER_RADIUS; dz++) {
                if (dx * dx + dz * dz > LEGACY_CRATER_RADIUS * LEGACY_CRATER_RADIUS) continue;
                for (int y = surface - 11; y <= surface - 2; y++) world.setBlockState(m.set(cx + dx, y, cz + dz), floor, 2);
            }
        }
        // Cột Tháp cũ: vòng đá bay và tháp ngược lên tới đỉnh
        int sx = origin.getX();
        int sz = origin.getZ() + LEGACY_SPIRE_DZ;
        for (int dx = -LEGACY_SPIRE_RADIUS; dx <= LEGACY_SPIRE_RADIUS; dx++) {
            for (int dz = -LEGACY_SPIRE_RADIUS; dz <= LEGACY_SPIRE_RADIUS; dz++) {
                if (dx * dx + dz * dz > LEGACY_SPIRE_RADIUS * LEGACY_SPIRE_RADIUS) continue;
                world.getChunk((sx + dx) >> 4, (sz + dz) >> 4);
                sweepColumn(world, m, sx + dx, sz + dz, surface, LEGACY_SPIRE_TOP, air, water, floor);
            }
        }
        removeEggs(world, new BlockPos(sx, com.samsara.system.womb.chapters.SpireChapter.POOL_Y, sz));
    }

    private static void sweepColumn(ServerWorld world, BlockPos.Mutable m, int x, int z, int fromY, int toY,
                                    BlockState air, BlockState water, BlockState floor) {
        for (int y = fromY; y <= toY; y++) {
            BlockState st = world.getBlockState(m.set(x, y, z));
            if (st.isAir() || isBuoy(st)) continue;
            world.setBlockState(m, air, 2);
        }
        BlockState w = world.getBlockState(m.set(x, WombRoute.SURFACE_Y - 1, z));
        if (!isBuoy(w) && !world.getFluidState(m).isStill()) world.setBlockState(m, water, 2);
        BlockState f = world.getBlockState(m.set(x, WombRoute.SURFACE_Y - 2, z));
        if (!f.isOf(FLOOR.getBlock())) world.setBlockState(m, floor, 2);
    }

    private static boolean isBuoy(BlockState st) {
        return st.isOf(Blocks.DARK_OAK_FENCE) || st.isOf(Blocks.SOUL_LANTERN);
    }

    private static void loadChunksAlong(ServerWorld world, List<Vec3d> path) {
        int lastX = Integer.MIN_VALUE;
        int lastZ = Integer.MIN_VALUE;
        for (Vec3d p : path) {
            int cx = (int) Math.floor(p.x) >> 4;
            int cz = (int) Math.floor(p.z) >> 4;
            if (cx == lastX && cz == lastZ) continue;
            lastX = cx;
            lastZ = cz;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    world.getChunk(cx + dx, cz + dz);
                }
            }
        }
    }

    private static void clearCorridor(ServerWorld world, List<Vec3d> path) {
        BlockState air = Blocks.AIR.getDefaultState();
        BlockState water = Blocks.WATER.getDefaultState();
        BlockState floor = FLOOR;
        BlockPos.Mutable m = new BlockPos.Mutable();
        int surface = WombRoute.SURFACE_Y;
        for (Vec3d p : path) {
            int px = (int) Math.floor(p.x);
            int pz = (int) Math.floor(p.z);
            for (int dx = -CORRIDOR_HALF_WIDTH; dx <= CORRIDOR_HALF_WIDTH; dx++) {
                for (int dz = -CORRIDOR_HALF_WIDTH; dz <= CORRIDOR_HALF_WIDTH; dz++) {
                    int x = px + dx;
                    int z = pz + dz;
                    for (int y = surface; y < surface + CLEAR_HEIGHT; y++) {
                        BlockState st = world.getBlockState(m.set(x, y, z));
                        // Giữ vách/trần căn phòng cửa, cánh cửa khổng lồ và ánh sáng khe cửa nằm ngay trên tuyến
                        if (st.isAir() || st.isOf(com.samsara.registry.SamsaraBlocks.WOMB_VEIL)
                                || st.isOf(com.samsara.registry.SamsaraBlocks.MEMORY_DOOR_GIANT) || st.isOf(Blocks.LIGHT)) continue;
                        world.setBlockState(m, air, 2);
                    }
                    if (!world.getFluidState(m.set(x, surface - 1, z)).isStill()) world.setBlockState(m, water, 2);
                    if (!world.getBlockState(m.set(x, surface - 2, z)).isOf(FLOOR.getBlock())) world.setBlockState(m, floor, 2);
                }
            }
        }
    }

    private static void placeBuoys(ServerWorld world, BlockPos origin) {
        List<Vec3d> marks = WombRoute.sample(origin, BUOY_SPACING);
        BlockPos spire = WombRoute.Stop.SPIRE.pos(origin);
        BlockState fence = Blocks.DARK_OAK_FENCE.getDefaultState();
        BlockState lantern = Blocks.SOUL_LANTERN.getDefaultState().with(LanternBlock.HANGING, false);
        int surface = WombRoute.SURFACE_Y;

        for (int i = 1; i < marks.size(); i++) {
            Vec3d p = marks.get(i);
            if (p.distanceTo(Vec3d.ofCenter(spire)) < 16.0) break; // để trống quanh bệ trứng
            Vec3d tangent = WombRoute.tangentNear(origin, p);
            Vec3d side = new Vec3d(-tangent.z, 0, tangent.x).multiply(i % 2 == 0 ? BUOY_OFFSET : -BUOY_OFFSET);
            BlockPos base = BlockPos.ofFloored(p.x + side.x, surface - 1, p.z + side.z);

            world.setBlockState(base, fence.with(FenceBlock.WATERLOGGED, true), 3);
            world.setBlockState(base.up(), fence, 3);
            world.setBlockState(base.up(2), lantern, 3);
        }
    }

    private static void removeEggs(ServerWorld world, BlockPos around) {
        Box area = new Box(around).expand(16.0, 16.0, 16.0);
        world.getEntitiesByClass(PrimordialEggEntity.class, area, e -> true).forEach(PrimordialEggEntity::discard);
    }

    /**
     * Phế tích mới sinh gần tuyến có thể chìa ra chắn hành lang: dọn lại đoạn hành lang
     * trong bán kính {@code radius} quanh (x, z). Gọi từ WombRuinManager sau khi dựng phế tích.
     */
    public static void clearCorridorNear(ServerWorld world, int x, int z, double radius) {
        BlockPos origin = EggCycleState.get(world.getServer()).getRouteOrigin();
        if (origin == null) return;
        Vec3d c = new Vec3d(x, WombRoute.SURFACE_Y, z);
        // Không dọn trong khu Bến Đò và Tháp Singularity (đã có công trình nằm ngay trên tuyến)
        Vec3d dock = Vec3d.ofCenter(origin);
        Vec3d spire = Vec3d.ofCenter(WombRoute.Stop.SPIRE.pos(origin));
        List<Vec3d> near = WombRoute.sample(origin, 3.0).stream()
                .filter(p -> p.squaredDistanceTo(c) < radius * radius)
                .filter(p -> p.squaredDistanceTo(dock) > 16.0 * 16.0 && p.squaredDistanceTo(spire) > 80.0 * 80.0)
                .toList();
        if (!near.isEmpty()) clearCorridor(world, near);
    }

    /** Có nằm quá gần tuyến (để WombRuinManager bỏ qua) không. */
    public static boolean isNearRoute(ServerWorld world, int x, int z) {
        BlockPos origin = EggCycleState.get(world.getServer()).getRouteOrigin();
        if (origin == null) return false;
        // Quanh Tháp (chương IV) không có phế tích nào chìa vào khung hình
        BlockPos spire = WombRoute.Stop.SPIRE.pos(origin);
        double sx = x - spire.getX(), sz = z - spire.getZ();
        if (sx * sx + sz * sz < SPIRE_RUIN_CLEARANCE * SPIRE_RUIN_CLEARANCE) return true;
        return (WombRoute.distanceToRoute(origin, x, z) < RUIN_CLEARANCE
                || com.samsara.system.womb.chapters.SaveDoorLayout.insideRoom(origin, x, z, 30));
    }
}
