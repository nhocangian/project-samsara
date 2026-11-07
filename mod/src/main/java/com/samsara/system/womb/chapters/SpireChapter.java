package com.samsara.system.womb.chapters;

import com.samsara.block.MemoryDoorFinalBlock;
import com.samsara.block.MemoryStatueBlock;
import com.samsara.block.TitanHaloBlock;
import com.samsara.entity.PrimordialEggEntity;
import com.samsara.registry.SamsaraBlocks;
import com.samsara.system.cycle.EggCycleState;
import com.samsara.system.ending.EggAltarManager;
import com.samsara.system.womb.WombRoute;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CandleBlock;
import net.minecraft.block.LightBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * Chương IV - Tháp Ngược (cuối tuyến). Bản 07/11/2026 theo ảnh tham chiếu của người dùng (thánh đường tròn có hàng
 * cột, mở ra vũ trụ, các vòng sáng đồng tâm trên trần, chùm sáng rọi xuyên quả cầu, vòng phù văn sáng dưới sàn, nến),
 * dựng bằng ngôn ngữ thạch cao trắng của chương I-III:
 *
 *  - BẾN: một cầu tàu thạch cao chắn ngang tuyến thuyền, cách tháp ~65 block về phía nam; thuyền cập vào đó.
 *  - ĐẠI CẦU THANG: thẳng một mạch về phía bắc, rộng 5, ba đợt có chiếu nghỉ, trụ thạch anh đỡ xuống biển, nến và
 *    ánh sáng hai bên: nhìn từ thuyền là thấy ngay đường lên.
 *  - CÁNH CỬA CUỐI: ở đỉnh cầu thang, lối thu hẹp còn một block; Cửa Ký Ức trắng cỡ người, đóng kín, người chơi tự mở.
 *    Từ chân cầu thang nhìn lên, cánh cửa nhỏ đứng một mình, Gargantua ngay phía sau.
 *  - THÁNH ĐƯỜNG: sàn tròn bán kính 15 ở độ cao mặt hồ, mặt dưới thuôn như một hòn đảo treo. Hai cung cột trắng
 *    (đông, tây) đầu cột điểm vàng, nối bằng dầm cong; phía bắc để trống hẳn, mở thẳng ra Gargantua (và chỗ bóng phản
 *    chiếu lớn dần ở đoạn kết); phía nam là lối vào. Hồ Gương ở giữa (EggAltarManager), nến quanh hồ, Tượng Quỳ giữa
 *    các cột quay vào Quả Trứng.
 *  - TRÊN TRẦN: không có mái; ba vầng hào quang đồng tâm to dần lên phía trên, rồi mũi tháp ngược đen tuyền
 *    (Vách Hư Không) treo từ trần thế giới.
 */
public final class SpireChapter {
    private SpireChapter() {}

    /** Mặt nước của hồ gương trên cao (chân người chơi ở POOL_Y - 1). */
    public static final int POOL_Y = 100;
    private static final int DECK_Y = POOL_Y - 1;          // sàn thánh đường và mặt cầu (đứng ở POOL_Y)
    private static final int SANCTUM_RADIUS = 15;
    private static final int COLUMN_RADIUS = 13;
    private static final int COLUMN_TOP = DECK_Y + 15;
    /** Góc các cột (độ, 0 = bắc, theo chiều kim đồng hồ). Bắc (300..60) để trống, nam (135..225) là lối vào. */
    private static final int[] COLUMN_ANGLES = {60, 85, 110, 135, 225, 250, 275, 300};
    private static final int[] STATUE_ANGLES = {72, 97, 122, 238, 263, 288};
    /** Cánh cửa cuối: z tương đối (phía nam tâm), trên lối hẹp giữa chiếu nghỉ trên cùng và sàn thánh đường. */
    private static final int DOOR_Z = 17;
    private static final int TOP_LANDING_Z = 19;          // chiếu nghỉ trên cùng: z 19..21
    private static final int STAIR_HALF_WIDTH = 2;
    private static final int FLIGHT = 12;
    /** Tháp ngược: mũi ở SPIRE_TIP, đỉnh chạm trần thế giới (The Womb cao 256). */
    private static final int SPIRE_TOP = 255;
    private static final int SPIRE_TIP = 130;
    private static final int SPIRE_RADIUS = 12;
    private static final int CLEAR_RADIUS = 40;
    /** Dọn phế tích cũ (WombRuinManager) quanh tháp; khổng thần Kẻ Đốt Nhà cách ~110 nên không chạm. */
    private static final int RUIN_SWEEP_RADIUS = 85;
    private static final int RUIN_SWEEP_FAR = 160;

    /** Vị trí nửa dưới của cánh cửa cuối (dùng cho nhãn máy quay và lệnh test). */
    public static BlockPos finalDoor(BlockPos origin) {
        return WombRoute.Stop.SPIRE.pos(origin).add(0, 0, DOOR_Z).withY(POOL_Y);
    }

    /** Một điểm trên cầu tàu, ngay chỗ tuyến thuyền cắt qua (lệnh test tp 4). */
    public static BlockPos dock(BlockPos origin) {
        BlockPos s = WombRoute.Stop.SPIRE.pos(origin);
        int z = dockZ();
        return new BlockPos(routeX(origin, z), WombRoute.SURFACE_Y + 1, s.getZ() + z + 1);
    }

    public static void build(ServerWorld world, BlockPos origin) {
        BlockPos s = WombRoute.Stop.SPIRE.pos(origin);
        Random random = Random.create(origin.asLong() ^ 0x5B12E5L);

        removeEggs(world, s);
        sweepRuins(world, s);
        clearColumn(world, s);
        buildSanctumFloor(world, s);
        EggAltarManager.build(world, s.withY(POOL_Y), null);
        EggCycleState.get(world.getServer()).setEggAltarPos(s.withY(POOL_Y));
        buildColumns(world, s);
        buildCongregation(world, s, random);
        // s nằm ở mặt biển (y 62): các hàm dưới cộng độ cao tuyệt đối nên dùng gốc y 0
        // (lỗi cũ: cầu thang, bến và cánh cửa bị đẩy lên thêm 62 block, treo tít trên trời)
        BlockPos s0 = s.withY(0);
        buildStairsAndDock(world, origin, s0, random);
        buildDoor(world, s0);
        buildCeilingAndSpire(world, s);
    }

    // ==========================================
    // DỌN CHỖ
    // ==========================================

    private static void removeEggs(ServerWorld world, BlockPos s) {
        Box area = new Box(s).expand(20.0, 60.0, 20.0).offset(0, 30, 0);
        world.getEntitiesByClass(PrimordialEggEntity.class, area, e -> true).forEach(PrimordialEggEntity::discard);
    }

    private static boolean keep(BlockState st) {
        return st.isOf(Blocks.DARK_OAK_FENCE) || st.isOf(Blocks.SOUL_LANTERN);   // phao đèn của tuyến
    }

    /** Phế tích lớn sinh ngoài tuyến có thể chìa vào khung hình của tháp: dọn phần nổi trên mặt nước. */
    private static void sweepRuins(ServerWorld world, BlockPos s) {
        BlockPos.Mutable m = new BlockPos.Mutable();
        BlockState air = Blocks.AIR.getDefaultState();
        int r2 = RUIN_SWEEP_RADIUS * RUIN_SWEEP_RADIUS;
        for (int dx = -RUIN_SWEEP_FAR; dx <= RUIN_SWEEP_FAR; dx++) {
            for (int dz = -RUIN_SWEEP_FAR; dz <= RUIN_SWEEP_RADIUS; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 <= CLEAR_RADIUS * CLEAR_RADIUS) continue;   // vòng trong do clearColumn lo
                // Ngoài 85 block chỉ dọn phía bắc và hai bên (phía nam là tuyến và khổng thần Kẻ Đốt Nhà)
                if (d2 > r2 && (d2 > RUIN_SWEEP_FAR * RUIN_SWEEP_FAR || dz > 40)) continue;
                world.getChunk((s.getX() + dx) >> 4, (s.getZ() + dz) >> 4);
                for (int y = WombRoute.SURFACE_Y; y <= 200; y++) {
                    BlockState st = world.getBlockState(m.set(s.getX() + dx, y, s.getZ() + dz));
                    if (!st.isAir() && !keep(st)) world.setBlockState(m, air, 2);
                }
            }
        }
    }

    /** Dọn cột quanh tháp tới trần thế giới (kể cả các bản dựng trước). */
    private static void clearColumn(ServerWorld world, BlockPos s) {
        BlockPos.Mutable m = new BlockPos.Mutable();
        BlockState air = Blocks.AIR.getDefaultState();
        for (int dx = -CLEAR_RADIUS; dx <= CLEAR_RADIUS; dx++) {
            for (int dz = -CLEAR_RADIUS; dz <= CLEAR_RADIUS; dz++) {
                if (dx * dx + dz * dz > CLEAR_RADIUS * CLEAR_RADIUS) continue;
                world.getChunk((s.getX() + dx) >> 4, (s.getZ() + dz) >> 4);
                for (int y = WombRoute.SURFACE_Y; y <= SPIRE_TOP; y++) {
                    BlockState st = world.getBlockState(m.set(s.getX() + dx, y, s.getZ() + dz));
                    if (!st.isAir() && !keep(st)) world.setBlockState(m, air, 2);
                }
                // Bậc chìm dưới nước của bản trước: trả lại nước
                BlockState below = world.getBlockState(m.set(s.getX() + dx, WombRoute.SURFACE_Y - 1, s.getZ() + dz));
                if (!below.isOf(Blocks.WATER) && !keep(below)) world.setBlockState(m, Blocks.WATER.getDefaultState(), 2);
            }
        }
    }

    // ==========================================
    // SÀN THÁNH ĐƯỜNG + MẶT DƯỚI
    // ==========================================

    private static void buildSanctumFloor(ServerWorld world, BlockPos s) {
        BlockState base = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState bricks = Blocks.QUARTZ_BRICKS.getDefaultState();
        BlockState chiseled = Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState();
        BlockState calcite = Blocks.CALCITE.getDefaultState();
        BlockPos.Mutable m = new BlockPos.Mutable();
        int R = SANCTUM_RADIUS;
        for (int dx = -R; dx <= R; dx++) {
            for (int dz = -R; dz <= R; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > R + 0.3) continue;
                // Mặt sàn: nền thạch anh nhẵn, vòng gạch thạch anh, 12 tia chạm khắc, viền calcite
                BlockState top = base;
                double ang = Math.atan2(dx, -dz);
                double sector = (ang + Math.PI) / (2 * Math.PI) * 12.0;
                if (d > R - 0.8) top = calcite;
                else if (Math.abs(d - 12.5) < 0.55) top = bricks;
                else if (d > 11.0 && Math.abs(sector - Math.round(sector)) * d < 0.5) top = chiseled;
                world.setBlockState(m.set(s.getX() + dx, DECK_Y, s.getZ() + dz), top, 2);
                world.setBlockState(m.set(s.getX() + dx, DECK_Y - 1, s.getZ() + dz), base, 2);
                world.setBlockState(m.set(s.getX() + dx, DECK_Y - 2, s.getZ() + dz), base, 2);
            }
        }
        // Mép sàn: dải thạch anh chạm ba tầng, điểm vàng mỗi 30°
        BlockState gold = Blocks.GOLD_BLOCK.getDefaultState();
        for (int i = 0; i < 360; i++) {
            double a = Math.toRadians(i);
            int ex = (int) Math.round(Math.sin(a) * R), ez = (int) Math.round(-Math.cos(a) * R);
            for (int y = DECK_Y - 3; y <= DECK_Y - 1; y++) {
                world.setBlockState(m.set(s.getX() + ex, y, s.getZ() + ez), i % 30 == 0 && y == DECK_Y - 2 ? gold : chiseled, 2);
            }
        }
        // Mặt dưới: vài bậc thu vào nông như mặt dưới một chiếc chuông, rồi một mũi kim thạch anh chĩa xuống biển,
        // đối xứng với mũi tháp ngược phía trên
        int[][] tiers = {{DECK_Y - 3, 14}, {DECK_Y - 4, 12}, {DECK_Y - 5, 9}, {DECK_Y - 6, 6}};
        for (int[] t : tiers) {
            for (int dx = -t[1]; dx <= t[1]; dx++) {
                for (int dz = -t[1]; dz <= t[1]; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d > t[1] + 0.3) continue;
                    world.setBlockState(m.set(s.getX() + dx, t[0], s.getZ() + dz), d > t[1] - 0.8 ? calcite : base, 2);
                }
            }
        }
        BlockState pillar = Blocks.QUARTZ_PILLAR.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Y);
        int spikeTop = DECK_Y - 7, spikeLen = 16;
        for (int i = 0; i < spikeLen; i++) {
            double r = 3.2 * (1.0 - i / (double) spikeLen);
            int ri = (int) Math.ceil(r);
            for (int dx = -ri; dx <= ri; dx++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    if (dx * dx + dz * dz > r * r + 0.2) continue;
                    world.setBlockState(m.set(s.getX() + dx, spikeTop - i, s.getZ() + dz), pillar, 2);
                }
            }
        }
        world.setBlockState(m.set(s.getX(), spikeTop - spikeLen, s.getZ()), Blocks.END_ROD.getDefaultState()
                .with(net.minecraft.block.EndRodBlock.FACING, Direction.DOWN), 2);
    }

    // ==========================================
    // HÀNG CỘT + DẦM CONG
    // ==========================================

    private static BlockPos polar(BlockPos s, double angleDeg, double r, int y) {
        double a = Math.toRadians(angleDeg);
        return new BlockPos(s.getX() + (int) Math.round(Math.sin(a) * r), y, s.getZ() - (int) Math.round(Math.cos(a) * r));
    }

    private static void buildColumns(ServerWorld world, BlockPos s) {
        BlockState plinth = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState shaft = Blocks.QUARTZ_PILLAR.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Y);
        BlockState capital = Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState();
        BlockState gold = Blocks.GOLD_BLOCK.getDefaultState();
        BlockState lamp = Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, 7);   // thạch anh trắng dễ cháy sáng
        int y0 = DECK_Y + 1;
        for (int angle : COLUMN_ANGLES) {
            BlockPos c = polar(s, angle, COLUMN_RADIUS, y0);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    boolean corner = dx != 0 && dz != 0;
                    world.setBlockState(c.add(dx, 0, dz), plinth, 2);
                    world.setBlockState(c.add(dx, COLUMN_TOP - y0, dz), corner ? gold : capital, 2);
                    if (corner) {
                        world.setBlockState(c.add(dx, 1, dz), candle(4), 2);   // nến trên bệ cột
                    }
                }
            }
            for (int y = 1; y < COLUMN_TOP - y0; y++) {
                world.setBlockState(c.up(y), shaft, 2);
                if (y >= 2) {
                    world.setBlockState(c.add(1, y, 0), shaft, 2);
                    world.setBlockState(c.add(-1, y, 0), shaft, 2);
                    world.setBlockState(c.add(0, y, 1), shaft, 2);
                    world.setBlockState(c.add(0, y, -1), shaft, 2);
                }
            }
            world.setBlockState(polar(s, angle, COLUMN_RADIUS - 2.5, DECK_Y + 5), lamp, 2);
        }
        // Dầm cong nối đầu cột: cung đông (60..135) và cung tây (225..300); phía bắc và nam để trống
        BlockState beam = Blocks.SMOOTH_QUARTZ.getDefaultState();
        for (int[] arc : new int[][]{{60, 135}, {225, 300}}) {
            for (double a = arc[0]; a <= arc[1]; a += 1.0) {
                world.setBlockState(polar(s, a, COLUMN_RADIUS, COLUMN_TOP + 1), beam, 2);
            }
        }
    }

    private static BlockState candle(int count) {
        return Blocks.WHITE_CANDLE.getDefaultState().with(CandleBlock.CANDLES, count).with(CandleBlock.LIT, true);
    }

    /** Tượng Quỳ giữa các cột quay vào Quả Trứng, và vòng nến quanh hồ. */
    private static void buildCongregation(ServerWorld world, BlockPos s, Random random) {
        int y = DECK_Y + 1;
        for (int angle : STATUE_ANGLES) {
            BlockPos p = polar(s, angle, 11.0, y);
            Direction facing = Math.abs(p.getX() - s.getX()) >= Math.abs(p.getZ() - s.getZ())
                    ? (p.getX() > s.getX() ? Direction.WEST : Direction.EAST)
                    : (p.getZ() > s.getZ() ? Direction.NORTH : Direction.SOUTH);
            world.setBlockState(p, SamsaraBlocks.MEMORY_STATUE.getDefaultState()
                    .with(MemoryStatueBlock.FACING, facing).with(MemoryStatueBlock.POSE, 2), 2);
        }
        for (int i = 0; i < 20; i++) {
            BlockPos p = polar(s, i * 18.0 + 9.0, 11.6, y);
            if (world.getBlockState(p).isAir()) world.setBlockState(p, candle(1 + random.nextInt(4)), 2);
        }
        // Ánh sáng trắng ngà quanh hồ (block ánh sáng vô hình)
        BlockState lamp = Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, 12);
        for (int i = 0; i < 8; i++) world.setBlockState(polar(s, i * 45.0 + 22.5, 8.0, POOL_Y + 3), lamp, 2);
    }

    // ==========================================
    // ĐẠI CẦU THANG + BẾN
    // ==========================================

    /** z tương đối của cầu tàu (hàng giữa). */
    private static int dockZ() {
        int rise = DECK_Y - (WombRoute.SURFACE_Y + 1);           // từ mặt bến (y 62) lên mặt cầu
        int flights = (rise + FLIGHT - 1) / FLIGHT;
        return TOP_LANDING_Z + 3 + rise + (flights - 1) * 3 + 1;
    }

    /** x (tuyệt đối) của tuyến thuyền ở hàng z tương đối {@code relZ}, trên đoạn cuối Kẻ Đốt Nhà -> Tháp. */
    private static int routeX(BlockPos origin, int relZ) {
        BlockPos a = WombRoute.Stop.TITAN_ARSONIST.pos(origin);
        BlockPos b = WombRoute.Stop.SPIRE.pos(origin);
        double t = (double) relZ / (a.getZ() - b.getZ());       // a.z > b.z
        return (int) Math.round(b.getX() + (a.getX() - b.getX()) * Math.abs(t));
    }

    private static void buildStairsAndDock(ServerWorld world, BlockPos origin, BlockPos s, Random random) {
        BlockState step = Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH);
        BlockState deck = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState edge = Blocks.CALCITE.getDefaultState();
        BlockState pillar = Blocks.QUARTZ_PILLAR.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Y);
        BlockState lamp = Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, 11);
        int W = STAIR_HALF_WIDTH;
        int seabed = WombRoute.SURFACE_Y - 2;

        // Chiếu nghỉ trên cùng (z 19..21) nối với lối hẹp tới cửa (z 16..18) và mép sàn thánh đường
        landing(world, s, TOP_LANDING_Z, DECK_Y, deck, edge, pillar, seabed, random);
        for (int z = SANCTUM_RADIUS; z < TOP_LANDING_Z; z++) world.setBlockState(s.add(0, DECK_Y, z), z == DOOR_Z ? edge : deck, 2);

        int z = TOP_LANDING_Z + 3;
        int y = DECK_Y - 1;
        int inFlight = 0;
        while (y > WombRoute.SURFACE_Y) {
            for (int x = -W; x <= W; x++) {
                world.setBlockState(s.add(x, y, z), step, 2);
                world.setBlockState(s.add(x, y - 1, z), Math.abs(x) == W ? edge : deck, 2);   // dầm đỡ dưới bậc
            }
            if (inFlight % 4 == 0) {
                world.setBlockState(s.add(-W, y + 2, z), lamp, 2);
                world.setBlockState(s.add(W, y + 2, z), lamp, 2);
            }
            z++;
            y--;
            if (++inFlight == FLIGHT && y > WombRoute.SURFACE_Y) {
                landing(world, s, z, y, deck, edge, pillar, seabed, random);
                z += 3;
                y--;
                inFlight = 0;
            }
        }
        // Bến: mặt ngang mặt biển + 1 (y 62), chắn ngang tuyến thuyền để thuyền cập vào
        int bz = z;
        int rx = routeX(origin, bz + 1) - s.getX();
        int west = Math.min(-W, rx - 6);
        int east = Math.max(W, rx + 6);
        for (int x = west; x <= east; x++) {
            for (int dz = 0; dz <= 2; dz++) {
                boolean rim = x == west || x == east || dz == 0 || dz == 2;
                world.setBlockState(s.add(x, WombRoute.SURFACE_Y, bz + dz), rim ? edge : deck, 2);
            }
            if ((x - west) % 4 == 0) {
                world.setBlockState(s.add(x, WombRoute.SURFACE_Y - 1, bz), pillar, 2);
                world.setBlockState(s.add(x, WombRoute.SURFACE_Y - 1, bz + 2), pillar, 2);
                world.setBlockState(s.add(x, WombRoute.SURFACE_Y + 1, bz + 2), candle(1 + random.nextInt(4)), 2);
            }
        }
        world.setBlockState(s.add(rx, WombRoute.SURFACE_Y + 3, bz + 1), lamp, 2);
        world.setBlockState(s.add(0, WombRoute.SURFACE_Y + 3, bz + 1), lamp, 2);
    }

    /** Chiếu nghỉ 3 hàng (z..z+2) ở mặt {@code y}, trụ thạch anh đỡ xuống đáy biển, nến ở bốn góc. */
    private static void landing(ServerWorld world, BlockPos s, int z, int y, BlockState deck, BlockState edge,
                                BlockState pillar, int seabed, Random random) {
        int W = STAIR_HALF_WIDTH;
        for (int x = -W; x <= W; x++) {
            for (int dz = 0; dz <= 2; dz++) world.setBlockState(s.add(x, y, z + dz), Math.abs(x) == W ? edge : deck, 2);
        }
        for (int x : new int[]{-W, W}) {
            for (int py = seabed; py < y; py++) world.setBlockState(s.add(x, py, z + 1), pillar, 2);
            world.setBlockState(s.add(x, y + 1, z), candle(1 + random.nextInt(4)), 2);
            world.setBlockState(s.add(x, y + 1, z + 2), candle(1 + random.nextInt(4)), 2);
        }
        world.setBlockState(s.add(0, y + 3, z + 1), Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, 12), 2);
    }

    // ==========================================
    // CÁNH CỬA CUỐI
    // ==========================================

    private static void buildDoor(ServerWorld world, BlockPos s) {
        BlockState door = SamsaraBlocks.MEMORY_DOOR_FINAL.getDefaultState()
                .with(MemoryDoorFinalBlock.FACING, Direction.SOUTH)
                .with(MemoryDoorFinalBlock.OPEN, false);
        BlockPos lower = s.add(0, POOL_Y, DOOR_Z);
        world.setBlockState(lower, door.with(MemoryDoorFinalBlock.HALF, DoubleBlockHalf.LOWER), 3);
        world.setBlockState(lower.up(), door.with(MemoryDoorFinalBlock.HALF, DoubleBlockHalf.UPPER), 3);
        // Dịu phía trước cửa, sáng hơn phía sau (bên hồ)
        world.setBlockState(s.add(0, POOL_Y + 2, DOOR_Z + 3), Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, 9), 2);
        world.setBlockState(s.add(0, POOL_Y + 2, DOOR_Z - 2), Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, 14), 2);
    }

    // ==========================================
    // VÒNG SÁNG TRÊN TRẦN + THÁP NGƯỢC
    // ==========================================

    private static void buildCeilingAndSpire(ServerWorld world, BlockPos s) {
        // Ba vầng hào quang đồng tâm, to dần lên phía trên như một phễu sáng dưới mũi tháp
        int[][] rings = {{POOL_Y + 14, 3}, {POOL_Y + 19, 6}, {POOL_Y + 24, 9}};
        for (int[] r : rings) {
            world.setBlockState(new BlockPos(s.getX(), r[0], s.getZ()), SamsaraBlocks.TITAN_HALO.getDefaultState()
                    .with(TitanHaloBlock.RADIUS, r[1]), 3);
        }
        world.setBlockState(new BlockPos(s.getX(), POOL_Y + 13, s.getZ()), Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, 15), 2);

        // Tháp ngược đen tuyền: vỏ dày 2
        BlockState veil = SamsaraBlocks.WOMB_VEIL.getDefaultState();
        BlockPos.Mutable m = new BlockPos.Mutable();
        int length = SPIRE_TOP - SPIRE_TIP;
        for (int y = SPIRE_TIP; y <= SPIRE_TOP; y++) {
            double t = (y - SPIRE_TIP) / (double) length;
            double r = Math.max(1.2, SPIRE_RADIUS * Math.pow(t, 0.85));
            int ri = (int) Math.ceil(r);
            for (int dx = -ri; dx <= ri; dx++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d > r || d < r - 2.2) continue;
                    world.setBlockState(m.set(s.getX() + dx, y, s.getZ() + dz), veil, 2);
                }
            }
        }
    }
}
