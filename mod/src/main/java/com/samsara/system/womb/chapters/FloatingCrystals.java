package com.samsara.system.womb.chapters;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

/**
 * Tinh thể tím lơ lửng dọc toàn tuyến (cảm hứng: những khối đá treo trên hồ gương).
 * Khối bát diện dựng bằng block, đầu dưới dài và nhọn hơn đầu trên; hai mặt sáng tối
 * (hắc diện thạch khóc / hắc diện thạch) để thấy rõ các mặt cắt khi in bóng xuống biển gương.
 * Treo so le hai bên tuyến, cao dần về phía Tháp Singularity như đang bị hút về Quả Trứng.
 */
public final class FloatingCrystals {
    private FloatingCrystals() {}

    private static final double SPACING = 34.0;

    public static void build(ServerWorld world, BlockPos origin, double routeLength) {
        Random random = Random.create(origin.asLong() ^ 0xC12A57A1L);
        int index = 0;
        double titansEnd = com.samsara.system.womb.WombRoute.progressOf(origin, com.samsara.system.womb.WombRoute.Stop.TITAN_ARSONIST) + 30.0;
        for (double along = 12.0; along < routeLength - 30.0; along += SPACING * (0.7 + random.nextDouble() * 0.6)) {
            // Chỉ còn ở chương IV: căn phòng cửa (I-II) và cánh đồng thạch cao (III) không có tinh thể
            if (along < titansEnd) continue;
            Vec3d p = SaveDoorChapter.pointAlong(origin, along);
            Vec3d tangent = com.samsara.system.womb.WombRoute.tangentNear(origin, p);
            int side = index++ % 2 == 0 ? 1 : -1;
            Vec3d normal = new Vec3d(-tangent.z, 0, tangent.x).multiply(side);
            double lateral = 9.0 + random.nextDouble() * 22.0;
            double progress = along / routeLength;
            int height = (int) (14 + progress * 18 + random.nextInt(8));
            int radius = 2 + random.nextInt(3);
            BlockPos center = BlockPos.ofFloored(p.add(normal.multiply(lateral)))
                    .withY(com.samsara.system.womb.WombRoute.SURFACE_Y + height);
            buildCrystal(world, center, radius, random.nextBoolean());
        }
    }

    /** Bát diện "kim cương" theo khoảng cách Manhattan; đầu trên cao radius+1, đầu dưới gấp ~1.8 lần. */
    public static void buildCrystal(ServerWorld world, BlockPos center, int radius, boolean flip) {
        BlockState dark = Blocks.OBSIDIAN.getDefaultState();
        BlockState lit = Blocks.CRYING_OBSIDIAN.getDefaultState();
        BlockState core = Blocks.AMETHYST_BLOCK.getDefaultState();
        int up = radius + 1;
        int down = (int) Math.round(radius * 1.8) + 1;

        for (int dy = -down; dy <= up; dy++) {
            double r = dy >= 0 ? radius * (1.0 - dy / (double) up) : radius * (1.0 + dy / (double) down);
            int ri = (int) Math.floor(r + 0.35);
            for (int dx = -ri; dx <= ri; dx++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    int m = Math.abs(dx) + Math.abs(dz);
                    if (m > ri) continue;
                    BlockState state;
                    if (m < ri - 1) state = core;
                    else state = ((flip ? -dx : dx) + dz >= 0) ? lit : dark;
                    world.setBlockState(center.add(dx, dy, dz), state, 2);
                }
            }
        }
    }
}
