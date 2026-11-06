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
        clearCorridor(world, path);
        placeBuoys(world, origin);

        // Kể chuyện bằng hình ảnh dọc tuyến
        com.samsara.system.womb.chapters.DockChapter.build(world, origin);
        com.samsara.system.womb.chapters.SaveDoorChapter.build(world, origin);
        com.samsara.system.womb.chapters.TitanChapter.build(world, origin);
        com.samsara.system.womb.chapters.FloatingCrystals.build(world, origin, WombRoute.length(origin));

        // Cuối tuyến: cầu thang xoắn, cánh cửa cuối, hồ gương lơ lửng với Quả Trứng, tháp ngược
        com.samsara.system.womb.chapters.SpireChapter.build(world, origin);

        // Client cần gốc tuyến cho dòng chảy & thuyền bóng ma
        WombCurrentSystem.syncAll(world.getServer());
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
        BlockState floor = Blocks.BLACKSTONE.getDefaultState();
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
                        if (!world.getBlockState(m.set(x, y, z)).isAir()) world.setBlockState(m, air, 2);
                    }
                    if (!world.getFluidState(m.set(x, surface - 1, z)).isStill()) world.setBlockState(m, water, 2);
                    if (world.getBlockState(m.set(x, surface - 2, z)).isAir()
                            || !world.getFluidState(m).isEmpty()) {
                        world.setBlockState(m, floor, 2);
                    }
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
                .filter(p -> p.squaredDistanceTo(dock) > 16.0 * 16.0 && p.squaredDistanceTo(spire) > 30.0 * 30.0)
                .toList();
        if (!near.isEmpty()) clearCorridor(world, near);
    }

    /** Có nằm quá gần tuyến (để WombRuinManager bỏ qua) không. */
    public static boolean isNearRoute(ServerWorld world, int x, int z) {
        BlockPos origin = EggCycleState.get(world.getServer()).getRouteOrigin();
        return origin != null && WombRoute.distanceToRoute(origin, x, z) < RUIN_CLEARANCE;
    }
}
