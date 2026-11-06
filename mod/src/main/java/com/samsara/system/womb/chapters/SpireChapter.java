package com.samsara.system.womb.chapters;

import com.samsara.entity.PrimordialEggEntity;
import com.samsara.system.cycle.EggCycleState;
import com.samsara.system.ending.EggAltarManager;
import com.samsara.system.womb.WombRoute;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChainBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * Chương 4 - Tháp Singularity & Hồ Gương (cuối tuyến).
 *
 *  - Cầu thang xoắn vuông lơ lửng, trồi lên từ mặt biển, vòng quanh trục tháp gần một vòng, lên cao 38 block.
 *  - Ở đỉnh cầu thang là một cây cầu ngắn với CÁNH CỬA CUỐI: cửa đôi đá đen viền vàng, mở sẵn, hắt ánh hổ phách;
 *    biển ghi save file cuối cùng - "chơi lần cuối: bây giờ".
 *  - Bước qua cửa là Hồ Gương lơ lửng: hồ nước nông có con dấu 7 tia dưới đáy và Quả Trứng (EggAltarManager).
 *  - Ngay trên hồ, một tòa tháp khổng lồ treo ngược từ trời xuống, mũi tháp nhỏ dần chĩa vào Quả Trứng;
 *    các vòng đèn hổ phách quấn quanh, xích buông thõng, đá vụn bay quanh. Chùm sáng beacon của hồ nối thẳng vào mũi tháp.
 */
public final class SpireChapter {
    private SpireChapter() {}

    /** Mặt nước của hồ gương trên cao (chân người chơi ở POOL_Y - 1). */
    public static final int POOL_Y = 100;
    private static final int HALF = 12;            // nửa cạnh vòng xoắn vuông
    private static final int SPIRE_TOP = 214;
    private static final int SPIRE_TIP = 120;
    private static final int SPIRE_RADIUS = 17;

    public static void build(ServerWorld world, BlockPos origin) {
        BlockPos s = WombRoute.Stop.SPIRE.pos(origin);
        Random random = Random.create(origin.asLong() ^ 0x5B12E5L);

        removeEggs(world, s);
        clearColumn(world, s);
        int bridgeStart = buildStairs(world, s);
        buildBridgeAndDoor(world, s, bridgeStart);
        EggAltarManager.build(world, s.withY(POOL_Y), null);
        EggCycleState.get(world.getServer()).setEggAltarPos(s.withY(POOL_Y));
        buildInvertedSpire(world, s, random);
        buildOrbitingStones(world, s, random);
    }

    // ==========================================
    // DỌN CHỖ
    // ==========================================

    private static void removeEggs(ServerWorld world, BlockPos s) {
        Box area = new Box(s).expand(20.0, 60.0, 20.0).offset(0, 30, 0);
        world.getEntitiesByClass(PrimordialEggEntity.class, area, e -> true).forEach(PrimordialEggEntity::discard);
    }

    private static void clearColumn(ServerWorld world, BlockPos s) {
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int dx = -22; dx <= 22; dx++) {
            for (int dz = -22; dz <= 22; dz++) {
                if (dx * dx + dz * dz > 22 * 22) continue;
                for (int y = WombRoute.SURFACE_Y; y <= SPIRE_TIP + 2; y++) {
                    if (!world.getBlockState(m.set(s.getX() + dx, y, s.getZ() + dz)).isAir()) {
                        world.setBlockState(m, Blocks.AIR.getDefaultState(), 2);
                    }
                }
            }
        }
    }

    // ==========================================
    // CẦU THANG XOẮN VUÔNG
    // ==========================================

    /**
     * Đi ngược chiều kim đồng hồ quanh hình vuông cạnh 24 (bắt đầu ở góc tây nam, đi về đông),
     * cứ 2 ô lên 1 bậc: bậc thang rồi một ô phẳng. Làn thứ hai nằm phía trong (bên trái chiều đi).
     * @return toạ độ x (tương đối) nơi cầu nối bắt đầu trên cạnh tây
     */
    private static int buildStairs(ServerWorld world, BlockPos s) {
        BlockState stair = Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.getDefaultState();
        BlockState tread = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        BlockState accent = Blocks.GILDED_BLACKSTONE.getDefaultState();
        int base = WombRoute.SURFACE_Y - 1;          // bậc đầu tiên chìm ngang mặt nước
        int rise = POOL_Y - 1 - base;                 // số bậc cần lên (đỉnh bậc cuối = chân trong hồ)

        int x = -HALF;
        int z = HALF;
        Direction dir = Direction.EAST;
        int cells = rise * 2;
        for (int j = 0; j < cells; j++) {
            int level = j / 2;
            int y = base + level;
            boolean isStair = j % 2 == 0;
            Direction inner = dir.rotateYCounterclockwise();
            BlockState state = isStair ? stair.with(StairsBlock.FACING, dir)
                    .with(StairsBlock.WATERLOGGED, y < WombRoute.SURFACE_Y)
                    : (j % 12 == 1 ? accent : tread);
            BlockPos p = s.add(x, y, z);
            world.setBlockState(p, state, 2);
            world.setBlockState(p.offset(inner), state, 2);

            // Bước tới; rẽ trái ở góc vuông
            int nx = x + dir.getOffsetX();
            int nz = z + dir.getOffsetZ();
            if (Math.abs(nx) > HALF || Math.abs(nz) > HALF) {
                dir = dir.rotateYCounterclockwise();
                nx = x + dir.getOffsetX();
                nz = z + dir.getOffsetZ();
            }
            x = nx;
            z = nz;
        }
        return z; // điểm dừng trên cạnh tây (x = -HALF)
    }

    // ==========================================
    // CẦU NỐI + CÁNH CỬA CUỐI
    // ==========================================

    private static void buildBridgeAndDoor(ServerWorld world, BlockPos s, int z) {
        BlockState stair = Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.EAST);
        BlockState deck = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        BlockState frame = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        BlockState gold = Blocks.GILDED_BLACKSTONE.getDefaultState();
        int deckY = POOL_Y - 1;                       // mặt cầu ngang mặt hồ

        // Cầu đi về phía đông (vào tâm), rộng 2 (z và z+1... phía trong của cạnh tây là phía đông)
        world.setBlockState(s.add(-HALF, deckY, z), stair, 2);
        world.setBlockState(s.add(-HALF, deckY, z + 1), stair, 2);
        for (int x = -HALF + 1; x <= -8; x++) {
            world.setBlockState(s.add(x, deckY, z), deck, 2);
            world.setBlockState(s.add(x, deckY, z + 1), deck, 2);
        }

        // Cửa đôi ở giữa cầu, quay mặt về phía tây (người đi tới)
        int doorX = -10;
        BlockPos left = s.add(doorX, deckY + 1, z);
        BlockPos right = s.add(doorX, deckY + 1, z + 1);
        for (int y = 0; y <= 3; y++) {
            world.setBlockState(left.add(0, y, -1), frame, 2);
            world.setBlockState(right.add(0, y, 1), frame, 2);
        }
        for (int dz = -1; dz <= 2; dz++) {
            world.setBlockState(s.add(doorX, deckY + 4, z + dz), gold, 2);
        }
        world.setBlockState(s.add(doorX, deckY + 3, z), frame, 2);
        world.setBlockState(s.add(doorX, deckY + 3, z + 1), frame, 2);

        BlockState door = Blocks.DARK_OAK_DOOR.getDefaultState()
                .with(DoorBlock.FACING, Direction.WEST)
                .with(DoorBlock.OPEN, true);
        world.setBlockState(left, door.with(DoorBlock.HINGE, DoorHinge.RIGHT).with(DoorBlock.HALF, DoubleBlockHalf.LOWER), 2);
        world.setBlockState(left.up(), door.with(DoorBlock.HINGE, DoorHinge.RIGHT).with(DoorBlock.HALF, DoubleBlockHalf.UPPER), 2);
        world.setBlockState(right, door.with(DoorBlock.HINGE, DoorHinge.LEFT).with(DoorBlock.HALF, DoubleBlockHalf.LOWER), 2);
        world.setBlockState(right.up(), door.with(DoorBlock.HINGE, DoorHinge.LEFT).with(DoorBlock.HALF, DoubleBlockHalf.UPPER), 2);

        // Đèn hổ phách hai bên khung
        world.setBlockState(left.add(0, 0, -2), Blocks.OCHRE_FROGLIGHT.getDefaultState(), 2);
        world.setBlockState(right.add(0, 0, 2), Blocks.OCHRE_FROGLIGHT.getDefaultState(), 2);

        // Biển save file cuối cùng trên khung, mặt về phía tây
        BlockPos signPos = s.add(doorX - 1, deckY + 3, z);
        world.setBlockState(signPos, Blocks.DARK_OAK_WALL_SIGN.getDefaultState().with(WallSignBlock.FACING, Direction.WEST), 2);
        if (world.getBlockEntity(signPos) instanceof SignBlockEntity sign) {
            SignText text = sign.getFrontText()
                    .withMessage(0, Text.literal("§6New World"))
                    .withMessage(1, Text.translatable("samsara.spire_chapter.choi_lan_cuoi"))
                    .withMessage(2, Text.translatable("samsara.spire_chapter.bay_gio"))
                    .withMessage(3, Text.translatable("samsara.spire_chapter.ban"))
                    .withGlowing(true);
            sign.setText(text, true);
        }
    }

    // ==========================================
    // THÁP NGƯỢC
    // ==========================================

    private static void buildInvertedSpire(ServerWorld world, BlockPos s, Random random) {
        BlockState[] wall = {Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState(), Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState(),
                Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.getDefaultState(), Blocks.BLACKSTONE.getDefaultState(),
                Blocks.OBSIDIAN.getDefaultState()};
        BlockState band = Blocks.OCHRE_FROGLIGHT.getDefaultState();
        BlockState rib = Blocks.CRYING_OBSIDIAN.getDefaultState();
        BlockState gold = Blocks.GILDED_BLACKSTONE.getDefaultState();
        BlockPos.Mutable m = new BlockPos.Mutable();
        int length = SPIRE_TOP - SPIRE_TIP;

        for (int y = SPIRE_TIP; y <= SPIRE_TOP; y++) {
            double t = (y - SPIRE_TIP) / (double) length;          // 0 ở mũi, 1 ở đỉnh
            double r = Math.max(0.6, SPIRE_RADIUS * Math.pow(t, 0.85));
            int ri = (int) Math.ceil(r);
            boolean bandRow = (y - SPIRE_TIP) % 12 == 6;
            for (int dx = -ri; dx <= ri; dx++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d > r || d < r - 2.2) continue;            // chỉ dựng vỏ dày 2
                    BlockState st;
                    boolean isRib = Math.abs(dx) <= 0 || Math.abs(dz) <= 0;
                    if (bandRow && d > r - 1.0) st = band;
                    else if (isRib && d > r - 1.0) st = rib;
                    else if (random.nextFloat() < 0.04f) st = gold;
                    else st = wall[random.nextInt(wall.length)];
                    world.setBlockState(m.set(s.getX() + dx, y, s.getZ() + dz), st, 2);
                }
            }
        }
        // Mũi tháp: hắc diện thạch khóc nhỏ giọt ánh sáng xuống Quả Trứng
        for (int y = SPIRE_TIP - 3; y < SPIRE_TIP; y++) {
            world.setBlockState(m.set(s.getX(), y, s.getZ()), rib, 2);
        }
        world.setBlockState(m.set(s.getX(), SPIRE_TIP - 4, s.getZ()), Blocks.SOUL_LANTERN.getDefaultState()
                .with(net.minecraft.block.LanternBlock.HANGING, true), 2);

        // Xích buông thõng từ thân dưới tháp
        int[][] anchors = {{6, 0}, {-6, 0}, {0, 6}, {0, -6}, {4, 4}, {-4, -4}};
        for (int[] a : anchors) {
            double ar = Math.hypot(a[0], a[1]);
            int anchorY = SPIRE_TIP + (int) Math.ceil(length * Math.pow(ar / SPIRE_RADIUS, 1.0 / 0.85));
            int hang = 6 + random.nextInt(9);
            for (int y = anchorY - 1; y >= anchorY - hang; y--) {
                world.setBlockState(m.set(s.getX() + a[0], y, s.getZ() + a[1]),
                        Blocks.CHAIN.getDefaultState().with(ChainBlock.AXIS, Direction.Axis.Y), 2);
            }
        }
    }

    /** Đá vụn và tinh thể nhỏ bay quanh thân tháp. */
    private static void buildOrbitingStones(ServerWorld world, BlockPos s, Random random) {
        for (int i = 0; i < 9; i++) {
            double a = i * (Math.PI * 2.0 / 9.0) + random.nextDouble() * 0.4;
            double r = 26.0 + random.nextDouble() * 10.0;
            int y = SPIRE_TIP + 6 + random.nextInt(60);
            BlockPos c = s.add((int) Math.round(Math.cos(a) * r), 0, (int) Math.round(Math.sin(a) * r)).withY(y);
            FloatingCrystals.buildCrystal(world, c, 1 + random.nextInt(2), random.nextBoolean());
        }
    }
}
