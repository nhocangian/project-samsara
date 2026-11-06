package com.samsara.system.womb.chapters;

import com.samsara.system.womb.WombRoute;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;

/**
 * Chương 3 - Ba Khổng Thần: ba cái chết của "bạn", kể bằng hình mà không cần chữ.
 * Mỗi tượng là thân người chơi phóng to (1 pixel skin = 1 block), đứng bên tuyến, quay mặt về phía người đi qua.
 *
 *  1. Steve 2009 (quỳ gối): Steve đầu tiên, skin cổ điển, phong hoá nặng nhất; quỳ cúi đầu trong nước,
 *     một khối tinh thể treo trên đầu như vầng hào quang.
 *  2. Hố Nổ 2013: miệng hố khổng lồ giữa biển, đáy khảm mặt Creeper; đôi chân còn đứng ở một bên mép,
 *     nửa thân trên nằm ngửa trôi ở mép kia; mảnh vụn lơ lửng như vụ nổ bị đóng băng.
 *  3. Kẻ Đốt Nhà (đứng): vẫn gương mặt ấy nhưng áo đen, mắt đỏ; một tay giơ cao đá lửa,
 *     tay kia nâng căn nhà gỗ cháy đen còn bốc khói - căn nhà của chính Nam.
 */
public final class TitanChapter {
    private TitanChapter() {}

    private static final int SEABED = WombRoute.SURFACE_Y - 2;   // đáy biển (y = 60)
    private static final int WATER = WombRoute.SURFACE_Y - 1;    // mặt nước (y = 61)

    public static void build(ServerWorld world, BlockPos origin) {
        Random random = Random.create(origin.asLong() ^ 0x717A_7155L);
        buildSteve2009(world, origin, random);
        buildCreeperCrater(world, origin, random);
        buildArsonist(world, origin, random);
    }

    /** Hướng từ khổng thần nhìn về tuyến (tuyến chạy gần bắc-nam nên chỉ cần đông/tây). */
    private static Vec3i facingRoute(WombRoute.Stop stop) {
        return (stop.side > 0 ? Direction.WEST : Direction.EAST).getVector();
    }

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
        clearAbove(world, base, 12, 40);
        TitanBody body = new TitanBody(world, TitanBody.Frame.upright(base, facingRoute(stop)),
                TitanBody.Skin.steve(), random, 0.24f);

        body.shinBack(-4, 0, -8, 6);
        body.shinBack(0, 0, -8, 6);
        body.leg(-4, 0, -2, 6, false);
        body.leg(0, 0, -2, 6, false);
        body.torso(-4, 6, -2);
        body.armDown(-8, 6, -2);
        body.armDown(4, 6, -2);
        body.head(-4, 17, -3);   // hạ 1 và nhô ra trước 1: cúi đầu

        // Vầng hào quang tinh thể
        FloatingCrystals.buildCrystal(world, body.frame().at(0, 34, 0), 3, false);
    }

    // ==========================================
    // 2. HỐ NỔ 2013
    // ==========================================

    private static final String[] CREEPER = {
            "GGGGGGGG",
            "GGGGGGGG",
            "GBBGGBBG",
            "GBBGGBBG",
            "GGGBBGGG",
            "GGBBBBGG",
            "GGBBBBGG",
            "GGBGGBGG",
    };

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
        // Mặt Creeper khảm dưới đáy hố (phóng 2 lần), bám theo lòng hố, nhìn xuyên qua làn nước
        for (int px = 0; px < 8; px++) {
            for (int pz = 0; pz < 8; pz++) {
                BlockState s = CREEPER[pz].charAt(px) == 'B' ? Blocks.BLACK_CONCRETE.getDefaultState()
                        : (random.nextFloat() < 0.3f ? Blocks.LIME_CONCRETE.getDefaultState() : Blocks.GREEN_CONCRETE.getDefaultState());
                for (int a = 0; a < 2; a++) {
                    for (int b = 0; b < 2; b++) {
                        int dx = -8 + px * 2 + a;
                        int dz = -8 + pz * 2 + b;
                        double d = Math.sqrt(dx * dx + dz * dz);
                        int floor = SEABED - (int) Math.round(8.0 * (1.0 - (d / radius) * (d / radius)));
                        world.setBlockState(new BlockPos(c.getX() + dx, floor, c.getZ() + dz), s, 2);
                    }
                }
            }
        }

        Vec3i along = Direction.NORTH.getVector(); // dọc tuyến
        Vec3i back = Direction.SOUTH.getVector();
        TitanBody.Skin skin = TitanBody.Skin.steve();

        // Đôi chân còn đứng ở mép bắc, gãy lởm chởm
        BlockPos legsBase = c.add(along.getX() * 11, 0, along.getZ() * 11);
        TitanBody legs = new TitanBody(world, TitanBody.Frame.upright(legsBase, facingRoute(stop)), skin, random, 0.2f);
        legs.leg(-4, -4, -2, 13, true);   // cắm xuống tận đáy hố, gãy lởm chởm ở trên
        legs.leg(0, -4, -2, 10, true);

        // Nửa thân trên nằm ngửa trôi ở mép nam, đầu chĩa ra ngoài hố
        BlockPos chestBase = c.add(back.getX() * 8, WATER - SEABED, back.getZ() * 8);
        TitanBody upper = new TitanBody(world, TitanBody.Frame.lyingFaceUp(chestBase, back), skin, random, 0.2f);
        upper.torso(-4, 0, -2);
        upper.armDown(-8, 0, -2);
        upper.head(-4, 11, -4);

        // Vụ nổ bị đóng băng: mảnh vụn lơ lửng theo vỏ bán cầu
        BlockState[] debris = {skin.shirt(), skin.pants(), skin.skin(), Blocks.COBBLESTONE.getDefaultState(),
                Blocks.GREEN_CONCRETE.getDefaultState(), Blocks.CRYING_OBSIDIAN.getDefaultState(), Blocks.GRAVEL.getDefaultState()};
        for (int i = 0; i < 110; i++) {
            double yaw = random.nextDouble() * Math.PI * 2.0;
            double elev = Math.toRadians(8 + random.nextDouble() * 62);
            double r = 7.0 + random.nextDouble() * 11.0;
            int x = c.getX() + (int) Math.round(Math.cos(yaw) * Math.cos(elev) * r);
            int z = c.getZ() + (int) Math.round(Math.sin(yaw) * Math.cos(elev) * r);
            int y = WombRoute.SURFACE_Y + 2 + (int) Math.round(Math.sin(elev) * r);
            BlockState s = debris[random.nextInt(debris.length)];
            if (s.isOf(Blocks.GRAVEL)) s = Blocks.COBBLED_DEEPSLATE.getDefaultState(); // không để rơi xuống
            world.setBlockState(new BlockPos(x, y, z), s, 2);
        }
    }

    // ==========================================
    // 3. KẺ ĐỐT NHÀ - ĐỨNG
    // ==========================================

    private static void buildArsonist(ServerWorld world, BlockPos origin, Random random) {
        WombRoute.Stop stop = WombRoute.Stop.TITAN_ARSONIST;
        BlockPos base = stop.landmark(origin).withY(SEABED);
        clearAbove(world, base, 14, 46);
        TitanBody body = new TitanBody(world, TitanBody.Frame.upright(base, facingRoute(stop)),
                TitanBody.Skin.griefer(), random, 0.1f);

        body.leg(-4, 0, -2, 12, true);
        body.leg(0, 0, -2, 12, true);
        body.torso(-4, 12, -2);
        body.armUp(4, 20, -2);            // tay phải giơ cao
        body.armForward(-8, 20, -2);      // tay trái đưa ra trước, ngửa lòng bàn tay
        body.head(-4, 24, -4);

        // Đá lửa trong tay giơ cao
        BlockState steel = Blocks.IRON_BLOCK.getDefaultState();
        BlockState flint = Blocks.DEEPSLATE_TILES.getDefaultState();
        body.putExact(5, 32, -1, steel);
        body.putExact(5, 33, -1, steel);
        body.putExact(6, 33, -1, steel);
        body.putExact(6, 34, -1, steel);
        body.putExact(5, 32, 0, flint);
        body.putExact(4, 33, 0, flint);
        body.putExact(5, 34, 0, Blocks.SHROOMLIGHT.getDefaultState()); // tia lửa

        // Căn nhà gỗ cháy đen trên lòng bàn tay (y 24 trở lên, z 5..12, tránh khỏi đầu)
        BlockState[] charred = {Blocks.COAL_BLOCK.getDefaultState(), Blocks.BLACK_CONCRETE.getDefaultState(),
                Blocks.DARK_OAK_PLANKS.getDefaultState(), Blocks.POLISHED_BLACKSTONE.getDefaultState()};
        for (int x = -12; x <= -3; x++) {
            for (int z = 5; z <= 12; z++) {
                body.putExact(x, 24, z, Blocks.BLACKSTONE.getDefaultState());
                boolean wall = x == -12 || x == -3 || z == 5 || z == 12;
                if (!wall) continue;
                for (int y = 25; y <= 27; y++) {
                    if (random.nextFloat() < 0.18f) continue; // tường cháy thủng
                    boolean window = y == 26 && (x == -8 || x == -7 || z == 8 || z == 9);
                    body.putExact(x, y, z, window ? Blocks.AIR.getDefaultState() : charred[random.nextInt(charred.length)]);
                }
            }
        }
        // Mái sụp một nửa
        for (int layer = 0; layer < 3; layer++) {
            for (int x = -12 + layer; x <= -3 - layer; x++) {
                for (int z = 5; z <= 12; z++) {
                    if (z > 9 && random.nextFloat() < 0.6f) continue;
                    body.putExact(x, 28 + layer, z, Blocks.BLACKSTONE.getDefaultState());
                }
            }
        }
        // Lửa linh hồn còn cháy bên trong, khói bốc cao
        body.putExact(-8, 25, 8, Blocks.SOUL_SOIL.getDefaultState());
        body.putExact(-8, 26, 8, Blocks.SOUL_FIRE.getDefaultState());
        body.putExact(-5, 25, 10, Blocks.SOUL_SOIL.getDefaultState());
        body.putExact(-5, 26, 10, Blocks.SOUL_FIRE.getDefaultState());
        body.putExact(-7, 31, 8, Blocks.SOUL_CAMPFIRE.getDefaultState()
                .with(CampfireBlock.LIT, true).with(CampfireBlock.SIGNAL_FIRE, true));
    }
}
