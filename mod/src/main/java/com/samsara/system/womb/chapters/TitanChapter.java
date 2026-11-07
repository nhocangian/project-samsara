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
 * Giữa các khổng thần là cánh đồng ~280 Tượng Quỳ (MemoryStatueBlock) rải hai bên tuyến, cùng quay về Gargantua.
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

    /** Số tượng quỳ mong muốn và vùng rải (quãng đường dọc tuyến, khoảng cách ngang tới tuyến). */
    private static final int STATUE_TARGET = 280;
    private static final double STATUE_FROM = 100.0;
    private static final double STATUE_TAIL = 30.0;     // rải quá Kẻ Đốt Nhà bao xa
    private static final int STATUE_INNER = 6;
    private static final int STATUE_OUTER = 46;
    private static final double TITAN_CLEARANCE = 21.0;

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

        // Vụ nổ bị đóng băng: mảnh vụn lơ lửng theo vỏ bán cầu
        // (thạch cao trắng và vài mảnh cửa xanh)
        BlockState[] debris = {skin.shirt(), skin.pants(), skin.skin(), skin.skin(), skin.hair(),
                Blocks.LIGHT_BLUE_TERRACOTTA.getDefaultState()};
        for (int i = 0; i < 110; i++) {
            double yaw = random.nextDouble() * Math.PI * 2.0;
            double elev = Math.toRadians(8 + random.nextDouble() * 62);
            double r = 7.0 + random.nextDouble() * 11.0;
            int x = c.getX() + (int) Math.round(Math.cos(yaw) * Math.cos(elev) * r);
            int z = c.getZ() + (int) Math.round(Math.sin(yaw) * Math.cos(elev) * r);
            int y = WombRoute.SURFACE_Y + 2 + (int) Math.round(Math.sin(elev) * r);
            BlockState s = debris[random.nextInt(debris.length)];
            world.setBlockState(new BlockPos(x, y, z), s, 2);
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
     * ~{@link #STATUE_TARGET} Tượng Quỳ trên mặt nước hai bên tuyến, từ sau cánh cửa trắng tới quá Kẻ Đốt Nhà,
     * thành từng hàng lỏng (lưới 3 block có xê dịch), cùng quay về Gargantua. Chừa chỗ quanh các khổng thần.
     */
    private static void buildStatueField(ServerWorld world, BlockPos origin, Random random) {
        double end = WombRoute.progressOf(origin, WombRoute.Stop.TITAN_ARSONIST) + STATUE_TAIL;
        List<BlockPos> candidates = new ArrayList<>();
        for (double along = STATUE_FROM; along <= end; along += 3.0) {
            Vec3d p = WombRoute.pointAlong(origin, along);
            Vec3d t = WombRoute.tangentNear(origin, p);
            Vec3d n = new Vec3d(-t.z, 0, t.x);
            for (int lateral = STATUE_INNER; lateral <= STATUE_OUTER; lateral += 3) {
                for (int side = -1; side <= 1; side += 2) {
                    Vec3d q = p.add(n.multiply(side * lateral));
                    BlockPos pos = BlockPos.ofFloored(q.x + random.nextInt(3) - 1, WombRoute.SURFACE_Y, q.z + random.nextInt(3) - 1);
                    if (nearTitan(origin, pos)) continue;
                    if (WombRoute.distanceToRoute(origin, pos.getX() + 0.5, pos.getZ() + 0.5) < STATUE_INNER - 1) continue;
                    candidates.add(pos);
                }
            }
        }
        float chance = Math.min(1.0f, STATUE_TARGET / (float) Math.max(1, candidates.size()));
        BlockState statue = SamsaraBlocks.MEMORY_STATUE.getDefaultState().with(MemoryStatueBlock.FACING, Direction.NORTH);
        for (BlockPos pos : candidates) {
            if (random.nextFloat() >= chance) continue;
            if (!world.getBlockState(pos).isAir()) continue;
            float r = random.nextFloat();
            int pose = r < 0.15f ? 2 : r < 0.40f ? 1 : 0;   // chắp tay / cúi đầu / ngửa mặt nhìn lên
            world.setBlockState(pos, statue.with(MemoryStatueBlock.POSE, pose), 2);
        }
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
