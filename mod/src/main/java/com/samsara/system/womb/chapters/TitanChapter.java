package com.samsara.system.womb.chapters;

import com.samsara.block.MemoryDoorGiantBlock;
import com.samsara.block.MemoryStatueBlock;
import com.samsara.registry.SamsaraBlocks;
import com.samsara.system.womb.WombRoute;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.LightBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

/**
 * Chương 3 - Những Người Khổng Lồ ("những lần bạn đã chết"), sau cánh cửa trắng của căn phòng cửa.
 *
 * Cùng ngôn ngữ với căn phòng cửa (07/11/2026): mọi thứ là thạch cao trắng (TitanBody.Skin.plaster), màu duy nhất là
 * những cánh cửa màu phấn (MemoryDoorGiantBlock). Mọi bức tượng quay mặt về Gargantua (phía bắc).
 *  1. Steve 2009: quỳ cúi đầu trước một cánh cửa trắng hé mở đầy ánh sáng - cánh cửa World1.
 *  2. Hố Nổ 2013: miệng hố giữa biển; đôi chân còn đứng ở mép bắc, nửa thân trên nằm ngửa ở mép nam, mảnh vụn
 *     lơ lửng như vụ nổ bị đóng băng; dưới đáy, qua làn nước, cánh cửa xanh "hầm mỏ" nằm bẹp, bị thổi bay khỏi khung.
 *  3. Kẻ Đốt Nhà: đứng, mắt đỏ; tay trái giơ cao đá lửa, tay phải chìa ra phía tuyến một cánh cửa hồng
 *     "server với Tuấn" đang cháy lửa linh hồn.
 * Tượng Quỳ (MemoryStatueBlock) xếp thành đoàn tín đồ quỳ theo hàng cong trước mỗi khổng thần (tuyến là lối đi
 * giữa), giữa các khổng thần chỉ lác đác vài tượng; tất cả quay về Gargantua.
 * Không khí thánh đường: mỗi khổng thần có vầng hào quang (TitanHalo) và ánh sáng thật rọi lên thân (bodyLight),
 * vòng đèn biển dưới nước quanh chân, tiếng chuông trầm và tiếng ngân (client/womb/TitanAmbienceClient); thuyền chậm
 * lại khi đi ngang mỗi khổng thần (WombRoute.titanSilence).
 * Qua máy quay: khổng thần và tượng quỳ hiện dòng thông báo chết (client/womb/SaveDoorLabels).
 * Ký tự phù phép đỏ phát sáng bay lên khắp chương (client/womb/RuneAmbienceClient).
 */
public final class TitanChapter {
    private TitanChapter() {}

    private static final int SEABED = WombRoute.SURFACE_Y - 2;   // đáy biển (y = 60)
    private static final int WATER = WombRoute.SURFACE_Y - 1;    // mặt nước (y = 61)

    /** Đoàn tín đồ trước mỗi khổng thần: số hàng, bán kính hàng đầu, khoảng cách giữa hàng và giữa ghế, nửa góc quạt. */
    private static final int CONGREGATION_ROWS = 7;
    private static final double CONGREGATION_FIRST = 23.0;
    private static final double CONGREGATION_ROW_GAP = 3.0;
    private static final double CONGREGATION_SEAT_GAP = 2.6;
    private static final double CONGREGATION_HALF_ANGLE = 55.0;
    /** Tượng lẻ loi giữa các khổng thần và vùng rải của chúng. */
    private static final int STRAGGLERS = 30;
    private static final double STATUE_FROM = 100.0;
    private static final double STATUE_TAIL = 30.0;     // rải quá Kẻ Đốt Nhà bao xa
    private static final int STATUE_INNER = 6;
    private static final int STATUE_OUTER = 46;
    private static final double TITAN_CLEARANCE = 20.5;

    public static void build(ServerWorld world, BlockPos origin) {
        Random random = Random.create(origin.asLong() ^ 0x717A_7155L);
        buildSteve2009(world, origin, random);
        buildCreeperCrater(world, origin, random);
        buildArsonist(world, origin, random);
        buildStatueField(world, origin, Random.create(origin.asLong() ^ 0x57A7_0E5L));
    }

    /** Mọi khổng thần quay về Gargantua (phía bắc). */
    private static final Vec3i GARGANTUA = Direction.NORTH.getVector();

    private static void clearAbove(ServerWorld world, BlockPos center, int radius, int height) {
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int y = WombRoute.SURFACE_Y; y < WombRoute.SURFACE_Y + height; y++) {
                    if (!world.getBlockState(m.set(center.getX() + dx, y, center.getZ() + dz)).isAir()) {
                        world.setBlockState(m, Blocks.AIR.getDefaultState(), 2);
                    }
                }
            }
        }
    }

    // ==========================================
    // 1. STEVE 2009 - QUỲ GỐI
    // ==========================================

    private static void buildSteve2009(ServerWorld world, BlockPos origin, Random random) {
        WombRoute.Stop stop = WombRoute.Stop.TITAN_STEVE;
        BlockPos base = stop.landmark(origin).withY(SEABED);
        clearAbove(world, base, 16, 40);
        TitanBody body = new TitanBody(world, TitanBody.Frame.upright(base, GARGANTUA),
                TitanBody.Skin.plaster(), random, 0.03f);

        body.shinBack(-4, 0, -8, 6);
        body.shinBack(0, 0, -8, 6);
        body.leg(-4, 0, -2, 6, false);
        body.leg(0, 0, -2, 6, false);
        body.torso(-4, 6, -2);
        body.armDown(-8, 6, -2);
        body.armDown(4, 6, -2);
        body.head(-4, 17, -3);   // hạ 1 và nhô ra trước 1: cúi đầu

        // Trước mặt Steve: cánh cửa trắng hé mở đầy ánh sáng (World1), mặt cửa quay về phía Steve
        BlockPos door = body.frame().at(0, WombRoute.SURFACE_Y - SEABED, 12);
        world.setBlockState(door, SamsaraBlocks.MEMORY_DOOR_GIANT.getDefaultState()
                .with(MemoryDoorGiantBlock.FACING, Direction.SOUTH)
                .with(MemoryDoorGiantBlock.COLOR, MemoryDoorGiantBlock.Color.WHITE)
                .with(MemoryDoorGiantBlock.LIT, true)
                .with(MemoryDoorGiantBlock.CURTAIN, false), 3);
        // Ánh sáng từ khe cửa rọi lên thân tượng
        BlockState bright = Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, 15);
        for (int y = 4; y <= 16; y += 6) world.setBlockState(body.frame().at(0, y, 8), bright, 2);

        halo(world, body.frame().at(0, 27, 0), 6);
        waterRing(world, base, 17);
        bodyLight(world, base, 9, 27);
    }

    // ==========================================
    // 2. HỐ NỔ 2013
    // ==========================================


    private static void buildCreeperCrater(ServerWorld world, BlockPos origin, Random random) {
        WombRoute.Stop stop = WombRoute.Stop.TITAN_CREEPER;
        BlockPos c = stop.landmark(origin).withY(SEABED);
        int radius = 15;
        clearAbove(world, c, radius + 4, 40);

        // Miệng hố: đáy cháy sém sâu dần vào tâm, nước lấp đầy
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > radius) continue;
                int depth = (int) Math.round(8.0 * (1.0 - (d / radius) * (d / radius)));
                int floor = SEABED - depth;
                BlockState scorch = random.nextFloat() < 0.5f ? Blocks.BLACKSTONE.getDefaultState()
                        : (random.nextBoolean() ? Blocks.BASALT.getDefaultState() : Blocks.COAL_BLOCK.getDefaultState());
                world.setBlockState(m.set(c.getX() + dx, floor, c.getZ() + dz), scorch, 2);
                for (int y = floor + 1; y <= WATER; y++) {
                    world.setBlockState(m.set(c.getX() + dx, y, c.getZ() + dz), Blocks.WATER.getDefaultState(), 2);
                }
            }
        }
        // Dưới đáy hố, qua làn nước: cánh cửa xanh "hầm mỏ" nằm bẹp, bị thổi bay khỏi khung
        int bottom = SEABED - 8;
        world.setBlockState(new BlockPos(c.getX(), bottom + 1, c.getZ()), SamsaraBlocks.MEMORY_DOOR_GIANT.getDefaultState()
                .with(MemoryDoorGiantBlock.FACING, Direction.SOUTH)
                .with(MemoryDoorGiantBlock.COLOR, MemoryDoorGiantBlock.Color.BLUE)
                .with(MemoryDoorGiantBlock.LIT, false)
                .with(MemoryDoorGiantBlock.FLAT, true)
                .with(MemoryDoorGiantBlock.WATERLOGGED, true), 3);

        Vec3i along = Direction.NORTH.getVector(); // dọc tuyến
        Vec3i back = Direction.SOUTH.getVector();
        TitanBody.Skin skin = TitanBody.Skin.plaster();

        // Đôi chân còn đứng ở mép bắc, gãy lởm chởm
        BlockPos legsBase = c.add(along.getX() * 11, 0, along.getZ() * 11);
        TitanBody legs = new TitanBody(world, TitanBody.Frame.upright(legsBase, GARGANTUA), skin, random, 0.03f);
        legs.leg(-4, -4, -2, 13, true);   // cắm xuống tận đáy hố, gãy lởm chởm ở trên
        legs.leg(0, -4, -2, 10, true);

        // Nửa thân trên nằm ngửa trôi ở mép nam, đầu chĩa ra ngoài hố
        BlockPos chestBase = c.add(back.getX() * 8, WATER - SEABED, back.getZ() * 8);
        TitanBody upper = new TitanBody(world, TitanBody.Frame.lyingFaceUp(chestBase, back), skin, random, 0.03f);
        upper.torso(-4, 0, -2);
        upper.armDown(-8, 0, -2);
        upper.head(-4, 11, -4);

        halo(world, c.withY(WombRoute.SURFACE_Y + 14), 7);
        waterRing(world, c, 19);
        bodyLight(world, c.add(0, 0, -11), 6, 16);   // đôi chân ở mép bắc
        bodyLight(world, c.add(0, 0, 8), 7, 6);      // nửa thân trên nằm ở mép nam

        // Vụ nổ bị đóng băng: một vòng sóng nổ rõ hình quanh miệng hố, ~30 mảnh lớn (cụm 2x2x2 sứt mẻ) lơ lửng
        // ở cùng một vành, nhấp nhô nhẹ; vài mảnh văng cao hơn ở giữa (thạch cao trắng, vài mảnh cửa xanh)
        BlockState[] debris = {skin.shirt(), skin.skin(), skin.skin(), skin.hair(), Blocks.LIGHT_BLUE_TERRACOTTA.getDefaultState()};
        int fragments = 30;
        for (int i = 0; i < fragments; i++) {
            double yaw = i * (Math.PI * 2.0 / fragments) + (random.nextDouble() - 0.5) * 0.12;
            double r = 17.0 + random.nextDouble() * 2.5;
            int x = c.getX() + (int) Math.round(Math.cos(yaw) * r);
            int z = c.getZ() + (int) Math.round(Math.sin(yaw) * r);
            int y = WombRoute.SURFACE_Y + 4 + (int) Math.round(2.0 * Math.sin(yaw * 3.0) + random.nextDouble() * 1.5);
            fragment(world, new BlockPos(x, y, z), debris[random.nextInt(debris.length)], random);
        }
        for (int i = 0; i < 6; i++) {
            double yaw = random.nextDouble() * Math.PI * 2.0;
            double r = 4.0 + random.nextDouble() * 6.0;
            BlockPos p = new BlockPos(c.getX() + (int) Math.round(Math.cos(yaw) * r),
                    WombRoute.SURFACE_Y + 10 + random.nextInt(6), c.getZ() + (int) Math.round(Math.sin(yaw) * r));
            fragment(world, p, debris[random.nextInt(debris.length)], random);
        }
    }

    // ==========================================
    // 3. KẺ ĐỐT NHÀ - ĐỨNG
    // ==========================================

    private static void buildArsonist(ServerWorld world, BlockPos origin, Random random) {
        WombRoute.Stop stop = WombRoute.Stop.TITAN_ARSONIST;
        BlockPos base = stop.landmark(origin).withY(SEABED);
        clearAbove(world, base, 16, 46);
        // Vẫn gương mặt ấy, chỉ khác đôi mắt đỏ
        TitanBody body = new TitanBody(world, TitanBody.Frame.upright(base, GARGANTUA),
                TitanBody.Skin.plaster(Blocks.RED_CONCRETE.getDefaultState()), random, 0.02f);

        body.leg(-4, 0, -2, 12, true);
        body.leg(0, 0, -2, 12, true);
        body.torso(-4, 12, -2);
        body.armUp(-8, 20, -2);           // tay trái giơ cao
        body.armForward(4, 20, -2);       // tay phải chìa ra phía tuyến, ngửa lòng bàn tay
        body.head(-4, 24, -4);

        // Đá lửa trong tay giơ cao
        BlockState steel = Blocks.IRON_BLOCK.getDefaultState();
        BlockState flint = Blocks.DEEPSLATE_TILES.getDefaultState();
        body.putExact(-7, 32, -1, steel);
        body.putExact(-7, 33, -1, steel);
        body.putExact(-6, 33, -1, steel);
        body.putExact(-6, 34, -1, steel);
        body.putExact(-7, 32, 0, flint);
        body.putExact(-8, 33, 0, flint);
        body.putExact(-7, 34, 0, Blocks.SHROOMLIGHT.getDefaultState()); // tia lửa

        // Trên lòng bàn tay phải: cánh cửa hồng "server với Tuấn" đứng thẳng, mặt quay về tuyến, đang cháy
        Direction toRoute = stop.side > 0 ? Direction.WEST : Direction.EAST;
        world.setBlockState(body.frame().at(5, 24, 7), SamsaraBlocks.MEMORY_DOOR_GIANT.getDefaultState()
                .with(MemoryDoorGiantBlock.FACING, toRoute)
                .with(MemoryDoorGiantBlock.COLOR, MemoryDoorGiantBlock.Color.PINK)
                .with(MemoryDoorGiantBlock.LIT, false)
                .with(MemoryDoorGiantBlock.BURNING, true), 3);
        body.putExact(5, 37, 7, Blocks.SOUL_CAMPFIRE.getDefaultState()
                .with(CampfireBlock.LIT, true).with(CampfireBlock.SIGNAL_FIRE, true));

        halo(world, body.frame().at(0, 34, 0), 6);
        waterRing(world, base, 17);
        bodyLight(world, base, 10, 36);
    }

    /** Một mảnh vụn: cụm 2x2x2 sứt mất vài góc. */
    private static void fragment(ServerWorld world, BlockPos at, BlockState state, Random random) {
        for (int dx = 0; dx < 2; dx++) {
            for (int dy = 0; dy < 2; dy++) {
                for (int dz = 0; dz < 2; dz++) {
                    if (random.nextFloat() < 0.3f) continue;
                    world.setBlockState(at.add(dx, dy, dz), state, 2);
                }
            }
        }
    }

    // ==========================================
    // HÀO QUANG & VÒNG SÁNG
    // ==========================================

    /** Vầng hào quang + tia sáng từ Gargantua (TitanHaloBlock, vẽ bởi TitanHaloRenderer). */
    private static void halo(ServerWorld world, BlockPos center, int radius) {
        world.setBlockState(center, SamsaraBlocks.TITAN_HALO.getDefaultState()
                .with(com.samsara.block.TitanHaloBlock.RADIUS, radius), 3);
    }

    /**
     * Ánh sáng thật rọi lên thân tượng (block ánh sáng vô hình, không vẽ gì thêm): các cột quanh thân, cách tâm
     * {@code radius} block, sáng nhất ở đầu (mức 14) và nhạt dần xuống chân (mức 6), phía bắc (hướng Gargantua) dày hơn.
     */
    private static void bodyLight(ServerWorld world, BlockPos base, int radius, int height) {
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4.0;
            int x = base.getX() + (int) Math.round(Math.cos(a) * radius);
            int z = base.getZ() + (int) Math.round(Math.sin(a) * radius);
            boolean north = Math.sin(a) < -0.1;     // -z = phía Gargantua
            int step = north ? 3 : 5;
            for (int dy = 2; dy <= height; dy += step) {
                int y = WombRoute.SURFACE_Y + dy;
                int level = 6 + Math.round(8.0f * dy / height);
                if (!north) level -= 2;
                if (!world.getBlockState(m.set(x, y, z)).isAir()) continue;
                world.setBlockState(m, Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, Math.min(15, level)), 2);
            }
        }
        // Ngay trên đỉnh đầu: nguồn sáng mạnh nhất, như rọi từ trên xuống
        BlockPos top = base.withY(WombRoute.SURFACE_Y + height + 2);
        if (world.getBlockState(top).isAir()) {
            world.setBlockState(top, Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, 15), 2);
        }
    }

    /** Vòng tròn thiêng: đèn biển ở đáy (y = 60) dưới lớp nước nông, quanh chân khổng thần. */
    private static void waterRing(ServerWorld world, BlockPos center, int radius) {
        BlockState lamp = Blocks.SEA_LANTERN.getDefaultState();
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int dx = -radius - 1; dx <= radius + 1; dx++) {
            for (int dz = -radius - 1; dz <= radius + 1; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (Math.abs(d - radius) > 0.5) continue;
                world.setBlockState(m.set(center.getX() + dx, SEABED, center.getZ() + dz), lamp, 2);
            }
        }
    }

    // ==========================================
    // CÁNH ĐỒNG TƯỢNG QUỲ
    // ==========================================

    /**
     * Tượng Quỳ xếp thành đoàn tín đồ: trước (phía nam) mỗi khổng thần là các hàng cong đồng tâm, như hàng ghế của một
     * giáo đường không mái; tuyến đi xuyên qua như lối đi giữa. Hàng đầu nhiều người chắp tay. Khoảng biển giữa hai
     * khổng thần chỉ lác đác vài tượng lẻ loi. Mọi tượng quay về phía bắc (khổng thần và Gargantua phía sau).
     */
    private static void buildStatueField(ServerWorld world, BlockPos origin, Random random) {
        BlockState statue = SamsaraBlocks.MEMORY_STATUE.getDefaultState().with(MemoryStatueBlock.FACING, Direction.NORTH);
        java.util.Set<BlockPos> used = new java.util.HashSet<>();

        // Đoàn tín đồ trước mỗi khổng thần
        for (WombRoute.Stop stop : titans()) {
            BlockPos c = stop.landmark(origin);
            for (int row = 0; row < CONGREGATION_ROWS; row++) {
                double radius = CONGREGATION_FIRST + row * CONGREGATION_ROW_GAP;
                double arc = Math.toRadians(CONGREGATION_HALF_ANGLE);
                int seats = (int) Math.round(2 * arc * radius / CONGREGATION_SEAT_GAP);
                for (int i = 0; i <= seats; i++) {
                    double a = -arc + 2 * arc * i / Math.max(1, seats);
                    // a = 0 là chính nam (+z)
                    BlockPos pos = BlockPos.ofFloored(c.getX() + 0.5 + Math.sin(a) * radius, WombRoute.SURFACE_Y,
                            c.getZ() + 0.5 + Math.cos(a) * radius);
                    if (random.nextFloat() < 0.12f) continue;   // vài ghế trống
                    float prayChance = row == 0 ? 0.45f : row == 1 ? 0.25f : 0.08f;
                    place(world, origin, used, pos, statue, random, prayChance);
                }
            }
        }

        // Vài tượng lẻ loi giữa các khổng thần
        double from = STATUE_FROM;
        double to = WombRoute.progressOf(origin, WombRoute.Stop.TITAN_ARSONIST) + STATUE_TAIL;
        for (int i = 0; i < STRAGGLERS; i++) {
            double along = from + random.nextDouble() * (to - from);
            Vec3d p = WombRoute.pointAlong(origin, along);
            Vec3d t = WombRoute.tangentNear(origin, p);
            Vec3d n = new Vec3d(-t.z, 0, t.x);
            double lateral = (random.nextBoolean() ? 1 : -1) * (STATUE_INNER + random.nextDouble() * (STATUE_OUTER - STATUE_INNER));
            Vec3d q = p.add(n.multiply(lateral));
            place(world, origin, used, BlockPos.ofFloored(q.x, WombRoute.SURFACE_Y, q.z), statue, random, 0.1f);
        }
    }

    private static void place(ServerWorld world, BlockPos origin, java.util.Set<BlockPos> used, BlockPos pos,
                              BlockState statue, Random random, float prayChance) {
        if (WombRoute.distanceToRoute(origin, pos.getX() + 0.5, pos.getZ() + 0.5) < 4.5) return;   // lối đi giữa
        if (nearTitan(origin, pos)) return;
        if (!used.add(pos) || used.contains(pos.east()) || used.contains(pos.west())
                || used.contains(pos.north()) || used.contains(pos.south())) return;   // không đứng dính nhau
        if (!world.getBlockState(pos).isAir()) return;
        float r = random.nextFloat();
        int pose = r < prayChance ? 2 : r < prayChance + 0.25f ? 1 : 0;   // chắp tay / cúi đầu / ngửa mặt
        world.setBlockState(pos, statue.with(MemoryStatueBlock.POSE, pose), 2);
    }

    private static boolean nearTitan(BlockPos origin, BlockPos pos) {
        WombRoute.Stop[] titans = {WombRoute.Stop.TITAN_STEVE, WombRoute.Stop.TITAN_CREEPER, WombRoute.Stop.TITAN_ARSONIST};
        for (WombRoute.Stop stop : titans) {
            BlockPos l = stop.landmark(origin);
            double dx = pos.getX() - l.getX();
            double dz = pos.getZ() - l.getZ();
            if (dx * dx + dz * dz < TITAN_CLEARANCE * TITAN_CLEARANCE) return true;
        }
        return false;
    }

    /** Ba khổng thần theo thứ tự dọc tuyến (dùng cho nhãn máy quay). */
    public static WombRoute.Stop[] titans() {
        return new WombRoute.Stop[]{WombRoute.Stop.TITAN_STEVE, WombRoute.Stop.TITAN_CREEPER, WombRoute.Stop.TITAN_ARSONIST};
    }
}
