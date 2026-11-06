package com.samsara.system.womb.chapters;

import com.samsara.system.womb.WombRoute;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

/**
 * Chương 1 - Bến Đò, "thức dậy" (nơi người chơi bước vào The Womb).
 *  - Bến gỗ mục sát mặt nước, ván thiếu ván thừa, cột đèn linh hồn ở đầu bến.
 *  - Chiếc giường đỏ của Nam đang chìm dần cạnh bến: thứ cuối cùng còn lại của căn phòng.
 *  - Những mảnh đất cỏ Overworld vỡ vụn lơ lửng quanh bến (hoa, đuốc, cây non...): thế giới vừa rời bỏ.
 *  - Chiếc thuyền gỗ sồi tối buộc ở đầu bến, mũi hướng về phía Quả Trứng (WombCurrentSystem giữ cho luôn có thuyền).
 */
public final class DockChapter {
    private DockChapter() {}

    /** Mặt sàn bến (đỉnh ván) = mặt nước, chân người chơi đứng ở y = DECK_Y + 1. */
    private static final int DECK_Y = WombRoute.SURFACE_Y - 1;
    private static final int NORTH_END = -8;
    private static final int SOUTH_END = 5;

    /** Chỗ buộc thuyền (đầu bắc của bến). */
    public static Vec3d mooring(BlockPos origin) {
        return new Vec3d(origin.getX() + 0.5, WombRoute.SURFACE_Y - 0.4, origin.getZ() + NORTH_END - 2.5);
    }

    public static void build(ServerWorld world, BlockPos origin) {
        Random random = Random.create(origin.asLong() ^ 0xD0C4L);
        buildPier(world, origin, random);
        buildSinkingBed(world, origin);
        buildFragments(world, origin, random);
        ensureBoat(world, origin);
    }

    private static void buildPier(ServerWorld world, BlockPos o, Random random) {
        BlockState[] boards = {Blocks.SPRUCE_PLANKS.getDefaultState(), Blocks.SPRUCE_PLANKS.getDefaultState(),
                Blocks.OAK_PLANKS.getDefaultState(), Blocks.DARK_OAK_PLANKS.getDefaultState()};
        BlockState post = Blocks.SPRUCE_FENCE.getDefaultState();
        for (int x = -2; x <= 2; x++) {
            for (int z = NORTH_END; z <= SOUTH_END; z++) {
                // Ván mục: thỉnh thoảng thiếu một tấm ở mép (không thiếu ở lối đi giữa)
                if (Math.abs(x) == 2 && random.nextFloat() < 0.12f) continue;
                BlockState b = Math.abs(x) == 2 ? Blocks.STRIPPED_SPRUCE_LOG.getDefaultState()
                        .with(net.minecraft.block.PillarBlock.AXIS, Direction.Axis.Z) : boards[random.nextInt(boards.length)];
                world.setBlockState(o.add(x, DECK_Y - o.getY(), z), b, 2);
                for (int y = DECK_Y + 1; y <= DECK_Y + 6; y++) {
                    world.setBlockState(o.add(x, y - o.getY(), z), Blocks.AIR.getDefaultState(), 2);
                }
            }
        }
        // Cột đèn linh hồn ở đầu bến, cột buộc thuyền
        for (int x : new int[]{-2, 2}) {
            BlockPos p = new BlockPos(o.getX() + x, DECK_Y + 1, o.getZ() + NORTH_END);
            world.setBlockState(p, post, 3);
            world.setBlockState(p.up(), post, 3);
            world.setBlockState(p.up(2), Blocks.SOUL_LANTERN.getDefaultState().with(LanternBlock.HANGING, false), 3);
        }
        world.setBlockState(new BlockPos(o.getX(), DECK_Y + 1, o.getZ() + NORTH_END), post, 3);

        // Vài thùng gỗ, chiếc rương cũ đã mở nắp trống rỗng
        world.setBlockState(new BlockPos(o.getX() + 1, DECK_Y + 1, o.getZ() + SOUTH_END - 1), Blocks.BARREL.getDefaultState(), 2);
        world.setBlockState(new BlockPos(o.getX() + 1, DECK_Y + 2, o.getZ() + SOUTH_END - 1), Blocks.BARREL.getDefaultState(), 2);
        world.setBlockState(new BlockPos(o.getX() - 1, DECK_Y + 1, o.getZ() + SOUTH_END), Blocks.BARREL.getDefaultState(), 2);
    }

    /** Giường đỏ chìm một nửa dưới làn nước cạnh bến, đầu giường hướng về Quả Trứng. */
    private static void buildSinkingBed(ServerWorld world, BlockPos o) {
        BlockState bed = Blocks.RED_BED.getDefaultState().with(BedBlock.FACING, Direction.NORTH);
        BlockPos foot = new BlockPos(o.getX() + 5, DECK_Y, o.getZ() - 1);
        world.setBlockState(foot.north(), bed.with(BedBlock.PART, BedPart.HEAD), 2);
        world.setBlockState(foot, bed.with(BedBlock.PART, BedPart.FOOT), 2);
    }

    /** Mảnh đất Overworld lơ lửng: đỉnh cỏ, lõi đất thu nhỏ dần, chóp đá ở dưới. */
    private static void buildFragments(ServerWorld world, BlockPos o, Random random) {
        BlockState[] decor = {Blocks.POPPY.getDefaultState(), Blocks.DANDELION.getDefaultState(),
                Blocks.GRASS.getDefaultState(), Blocks.OAK_SAPLING.getDefaultState(),
                Blocks.TORCH.getDefaultState(), Blocks.CRAFTING_TABLE.getDefaultState()};
        for (int i = 0; i < 7; i++) {
            double a = i * (Math.PI * 2.0 / 7.0) + random.nextDouble() * 0.5;
            double r = 11.0 + random.nextDouble() * 16.0;
            int cx = o.getX() + (int) Math.round(Math.cos(a) * r);
            int cz = o.getZ() + 6 + (int) Math.round(Math.sin(a) * r * 0.8);
            int top = WombRoute.SURFACE_Y + 5 + random.nextInt(16);
            int radius = 1 + random.nextInt(3);

            for (int layer = 0; layer <= radius + 2; layer++) {
                double lr = radius + 0.6 - layer * 0.75;
                if (lr < 0.4) lr = 0.4;
                int y = top - layer;
                for (int dx = -radius - 1; dx <= radius + 1; dx++) {
                    for (int dz = -radius - 1; dz <= radius + 1; dz++) {
                        if (dx * dx + dz * dz > lr * lr + random.nextFloat() * 1.2f) continue;
                        BlockState s = layer == 0 ? Blocks.GRASS_BLOCK.getDefaultState()
                                : layer >= radius + 1 ? Blocks.STONE.getDefaultState() : Blocks.DIRT.getDefaultState();
                        world.setBlockState(new BlockPos(cx + dx, y, cz + dz), s, 2);
                    }
                }
            }
            BlockState d = decor[random.nextInt(decor.length)];
            world.setBlockState(new BlockPos(cx, top + 1, cz), d, 2);
        }
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
