package com.samsara.system.demdautien;

import com.samsara.registry.SamsaraDimensions;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.FlowerbedBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

/**
 * Tầng 4 "Đêm đầu tiên" (Alpha 1.2.6, 2010): dựng hòn đảo lơ lửng mang texture Alpha, rải ba vết rách thời gian lộ
 * ra bản 1.20.1. Luật chơi ở {@link DemDauTienRuntime}. Phần "Alpha / mới" do shader world6 lo ({@code lib/dem_dau_tien_tear.glsl}): khối nào có
 * texture Alpha ({@code tools/import_alpha_textures.py}) thì ngoài vết rách hiện bản Alpha; khối chỉ có ở bản mới
 * (anh đào, đèn lồng...) thì chỉ thấy qua vết rách. Người chơi đi lại gần được (đồ bản mới vô hình ngoài vết rách nhưng vẫn chạm được).
 */
public final class DemDauTienBuilder {
    public static final BlockPos ORIGIN = new BlockPos(0, 64, 0);
    private static final int FLAGS = 2 | 16;
    static final int RADIUS = 40;

    /** Tâm (x, y, z) và bán kính vết rách. PHẢI khớp {@code tear()} trong shader. */
    public static final double[][] TEARS = {
            {18.5, 68.0, -9.5, 6.5},
            {-15.5, 67.0, 12.5, 6.0},
            {3.5, 66.5, 23.5, 4.5},
    };
    /**
     * Chỗ đứng (x, z) của ba con cừu. Đánh con thứ k thì có một vết rách nhỏ quanh nó: PHẢI khớp {@code sheepSpot()}
     * trong shader.
     */
    public static final int[][] SHEEP = {{6, 8}, {-7, 5}, {9, -6}};

    /**
     * Đồ của bản mới được phép lấn ra ngoài bán kính chừng này khối: mép rách cắt ngang qua chúng như trong clip
     * mẫu (phần ngoài vết rách thì ẩn). Nền vẫn là cỏ thường: trong vết rách là cỏ 1.20.1, ngoài là cỏ Alpha.
     */
    private static final double SPILL = 1.0;

    private DemDauTienBuilder() {
    }

    public static ServerWorld world(ServerPlayerEntity player) {
        return player.getServer().getWorld(SamsaraDimensions.DEM_DAU_TIEN_WORLD_KEY);
    }

    /** Chỗ tỉnh dậy: giữa đảo. */
    public static BlockPos spawn() {
        return new BlockPos(0, height(0, 0) + 1, 0);
    }

    public static void build(ServerWorld world) {
        Random random = Random.create(0x2010L);
        clearAndShape(world, random);
        trees(world, random);
        flowers(world, random);
        cherryTear(world);
        hutTear(world);
        azaleaTear(world);
        sheep(world);
    }

    // ==========================================
    // ĐẢO
    // ==========================================

    private static double hash(int x, int z, int salt) {
        long h = x * 73856093L ^ z * 19349663L ^ salt * 83492791L;
        h = (h ^ (h >>> 13)) * 0x5bd1e995L;
        return ((h ^ (h >>> 15)) & 0xFFFF) / 65535.0;
    }

    private static double smooth(double x, double z, double scale, int salt) {
        double fx = x / scale, fz = z / scale;
        int ix = MathHelper.floor(fx), iz = MathHelper.floor(fz);
        double tx = fx - ix, tz = fz - iz;
        tx = tx * tx * (3 - 2 * tx);
        tz = tz * tz * (3 - 2 * tz);
        double a = MathHelper.lerp(tx, hash(ix, iz, salt), hash(ix + 1, iz, salt));
        double b = MathHelper.lerp(tx, hash(ix, iz + 1, salt), hash(ix + 1, iz + 1, salt));
        return MathHelper.lerp(tz, a, b);
    }

    /** Mép đảo theo góc: tròn méo. */
    static double edge(int x, int z) {
        double ang = Math.atan2(z, x);
        return RADIUS * (0.82 + 0.18 * smooth(Math.cos(ang) * 6 + 20, Math.sin(ang) * 6 + 20, 1.7, 3));
    }

    static int height(int x, int z) {
        double d = Math.sqrt(x * x + z * z) / edge(x, z);
        double hills = smooth(x, z, 11, 1) * 4.0 + smooth(x, z, 5, 2) * 1.5;
        return ORIGIN.getY() + (int) Math.round(hills * (1.0 - d * d) - 1.0);
    }

    private static void clearAndShape(ServerWorld world, Random random) {
        BlockState air = Blocks.AIR.getDefaultState();
        for (int x = -RADIUS - 4; x <= RADIUS + 4; x++) {
            for (int z = -RADIUS - 4; z <= RADIUS + 4; z++) {
                double d = Math.sqrt(x * x + z * z);
                double e = edge(x, z);
                for (int y = ORIGIN.getY() - 34; y <= ORIGIN.getY() + 20; y++) world.setBlockState(new BlockPos(x, y, z), air, FLAGS);
                if (d > e) continue;
                int top = height(x, z);
                // Mặt dưới thuôn thành hình nón méo
                double k = d / e;
                int bottom = ORIGIN.getY() - 4 - (int) ((1.0 - k * k) * (18 + 10 * smooth(x, z, 6, 4)));
                boolean beach = k > 0.9 && top <= ORIGIN.getY();
                for (int y = bottom; y <= top; y++) {
                    BlockState s;
                    if (y == top) s = beach ? Blocks.SAND.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState();
                    else if (y >= top - 3) s = beach ? Blocks.SAND.getDefaultState() : Blocks.DIRT.getDefaultState();
                    else {
                        double r = random.nextDouble();
                        if (r < 0.02) s = Blocks.COAL_ORE.getDefaultState();
                        else if (r < 0.03) s = Blocks.IRON_ORE.getDefaultState();
                        else if (r < 0.08) s = Blocks.GRAVEL.getDefaultState();
                        else s = Blocks.STONE.getDefaultState();
                    }
                    world.setBlockState(new BlockPos(x, y, z), s, FLAGS);
                }
            }
        }
    }

    public static int surface(ServerWorld world, int x, int z) {
        return height(x, z);
    }

    private static boolean nearTear(int x, int z, double margin) {
        for (double[] t : TEARS) {
            double dx = x + 0.5 - t[0], dz = z + 0.5 - t[2];
            if (Math.sqrt(dx * dx + dz * dz) < t[3] + margin) return true;
        }
        return false;
    }

    private static boolean onIsland(int x, int z, double inset) {
        return Math.sqrt(x * x + z * z) < edge(x, z) - inset;
    }

    // ==========================================
    // CÂY, HOA (BẢN ALPHA)
    // ==========================================

    private static void trees(ServerWorld world, Random random) {
        BlockState log = Blocks.OAK_LOG.getDefaultState();
        BlockState leaves = Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
        int placed = 0;
        for (int tries = 0; tries < 200 && placed < 14; tries++) {
            int x = random.nextInt(RADIUS * 2) - RADIUS, z = random.nextInt(RADIUS * 2) - RADIUS;
            if (!onIsland(x, z, 5) || nearTear(x, z, 4) || Math.abs(x) + Math.abs(z) < 6) continue;
            int y = height(x, z) + 1;
            int h = 4 + random.nextInt(2);
            // Cây Alpha: thân thẳng, tán khối tròn vuông vức
            for (int dy = h - 2; dy <= h + 1; dy++) {
                int r = dy >= h ? 1 : 2;
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.abs(dx) == r && Math.abs(dz) == r && (dy == h + 1 || random.nextBoolean())) continue;
                        world.setBlockState(new BlockPos(x + dx, y + dy, z + dz), leaves, FLAGS);
                    }
                }
            }
            for (int dy = 0; dy < h; dy++) world.setBlockState(new BlockPos(x, y + dy, z), log, FLAGS);
            placed++;
        }
    }

    private static void flowers(ServerWorld world, Random random) {
        for (int i = 0; i < 70; i++) {
            int x = random.nextInt(RADIUS * 2) - RADIUS, z = random.nextInt(RADIUS * 2) - RADIUS;
            if (!onIsland(x, z, 3) || nearTear(x, z, 2)) continue;
            BlockPos ground = new BlockPos(x, height(x, z), z);
            if (!world.getBlockState(ground).isOf(Blocks.GRASS_BLOCK) || !world.isAir(ground.up())) continue;
            world.setBlockState(ground.up(), (random.nextBoolean() ? Blocks.DANDELION : Blocks.POPPY).getDefaultState(), FLAGS);
        }
    }

    // ==========================================
    // VẾT RÁCH: ĐỒ CỦA BẢN MỚI (chỉ hiện trong vết rách)
    // ==========================================

    static BlockPos ground(double[] t, int dx, int dz) {
        int x = MathHelper.floor(t[0]) + dx, z = MathHelper.floor(t[2]) + dz;
        return new BlockPos(x, height(x, z) + 1, z);
    }

    /** Có nằm gọn trong vết rách (kể cả khi mép co vào) không. */
    private static boolean fits(double[] t, BlockPos p) {
        double dx = p.getX() + 0.5 - t[0], dy = (p.getY() + 0.5 - t[1]) * 1.25, dz = p.getZ() + 0.5 - t[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz) < t[3] + SPILL;
    }

    private static void place(ServerWorld world, double[] t, BlockPos p, BlockState s) {
        if (fits(t, p)) world.setBlockState(p, s, FLAGS);
    }

    private static void cherryTear(ServerWorld world) {
        double[] t = TEARS[0];
        BlockState log = Blocks.CHERRY_LOG.getDefaultState();
        BlockState leaves = Blocks.CHERRY_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
        BlockPos base = ground(t, 0, 0);
        for (int dy = 0; dy < 4; dy++) world.setBlockState(base.up(dy), log, FLAGS);
        BlockPos crown = base.up(4);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    if (dx * dx + dz * dz + dy * dy * 3 > 10) continue;
                    place(world, t, crown.add(dx, dy, dz), leaves);
                }
            }
        }
        world.setBlockState(crown, log, FLAGS);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if ((dx + dz) % 2 != 0 || (dx == 0 && dz == 0)) continue;
                BlockPos p = ground(t, dx, dz);
                if (world.isAir(p)) place(world, t, p, Blocks.PINK_PETALS.getDefaultState()
                        .with(FlowerbedBlock.FLOWER_AMOUNT, 1 + Math.floorMod(dx * 3 + dz, 4)));
            }
        }
    }

    private static void hutTear(ServerWorld world) {
        double[] t = TEARS[1];
        BlockState wall = Blocks.MUD_BRICKS.getDefaultState();
        BlockState plank = Blocks.SPRUCE_PLANKS.getDefaultState();
        BlockPos c = ground(t, 0, 0);
        // Một góc nhà kiểu mới: hai bức tường, mái gỗ vân sam, đèn lồng, thùng gỗ
        for (int i = -2; i <= 2; i++) {
            for (int dy = 0; dy <= 2; dy++) {
                place(world, t, c.add(i, dy, -2), wall);
                place(world, t, c.add(-2, dy, i), wall);
            }
            for (int j = -2; j <= 2; j++) place(world, t, c.add(i, 3, j), plank);
        }
        place(world, t, c.add(-1, 0, -1), Blocks.BARREL.getDefaultState());
        place(world, t, c.add(1, 2, 1), Blocks.LANTERN.getDefaultState().with(net.minecraft.block.LanternBlock.HANGING, true));
        place(world, t, c.add(0, 0, 1), Blocks.FERN.getDefaultState());
        place(world, t, c.add(1, 0, -1), Blocks.GRASS.getDefaultState());
    }

    private static void azaleaTear(ServerWorld world) {
        double[] t = TEARS[2];
        BlockPos c = ground(t, 0, 0);
        place(world, t, c, Blocks.FLOWERING_AZALEA.getDefaultState());
        int[][] spots = {{1, 0}, {-1, 1}, {0, -1}, {1, 1}, {-1, -1}, {2, 0}, {0, 2}};
        for (int i = 0; i < spots.length; i++) {
            BlockPos p = ground(t, spots[i][0], spots[i][1]);
            place(world, t, p, (i % 3 == 0 ? Blocks.FERN : i % 3 == 1 ? Blocks.GRASS : Blocks.CORNFLOWER).getDefaultState());
        }
    }

    static void sheep(ServerWorld world) {
        world.getEntitiesByType(EntityType.SHEEP, e -> true).forEach(e -> e.discard());
        for (int[] s : SHEEP) {
            SheepEntity sheep = EntityType.SHEEP.create(world);
            if (sheep == null) continue;
            int y = height(s[0], s[1]) + 1;
            sheep.refreshPositionAndAngles(s[0] + 0.5, y, s[1] + 0.5, 0, 0);
            Vec3d to = new Vec3d(0.5, y, 0.5).subtract(sheep.getPos());
            float yaw = (float) (MathHelper.atan2(to.z, to.x) * 180 / Math.PI) - 90f;
            sheep.setYaw(yaw);
            sheep.setHeadYaw(yaw);
            sheep.setBodyYaw(yaw);
            sheep.setAiDisabled(true);
            sheep.setPersistent();
            world.spawnEntity(sheep);
        }
    }
}
