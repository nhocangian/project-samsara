package com.samsara.system.overworld;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FurnaceBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.PersistentState;

/**
 * Dấu vết người chơi trước (Overworld, quanh điểm spawn): những thứ một người chơi bình thường để lại trong đêm đầu
 * tiên, không biển, không chữ. Thế giới là save của Nam ({@code Save: Nam_1998/level.dat} trên F3), và đây là bằng chứng:
 * <ul>
 *   <li>một lán đất 2x2 có lỗ cửa, bên trong bàn chế tạo, lò nung, một ngọn đuốc ("đêm đầu tiên", tầng 4 Tầng Dưới
 *       trả lời cho nó);</li>
 *   <li>một đường hầm bậc thang đào thẳng xuống từ cạnh lán, cứ vài bậc một ngọn đuốc;</li>
 *   <li>hai cây bị chặt dở: một gốc còn trơ với tán lá lơ lửng (lá không rụng), một cây mất hai khúc gốc;</li>
 *   <li>một cột đất 1x1 kiểu "trốn quái đêm đầu";</li>
 *   <li>vài cây hướng dương trồng cạnh lán (chúng thỉnh thoảng quay về phía người chơi, xem
 *       {@link OverworldSunflowerGaze}).</li>
 * </ul>
 * Dựng một lần cho mỗi thế giới, lúc người chơi vào game lần đầu (cùng lúc với Bia Mộ).
 */
public final class NamTraces extends PersistentState {
    private static final String KEY = "samsara_nam_traces";
    private boolean built;

    private static final int FLAGS = 2;

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerWorld world = server.getOverworld();
            NamTraces state = world.getPersistentStateManager().getOrCreate(NamTraces::fromNbt, NamTraces::new, KEY);
            if (state.built) return;
            build(world, world.getSpawnPos());
            state.built = true;
            state.markDirty();
        });
    }

    /** Chỗ của lần dựng gần nhất (lán, gốc chặt, cây mất gốc, cột đất), cho lệnh test đứng xem. */
    private static final BlockPos[] LAST = new BlockPos[4];

    /** Dựng toàn bộ dấu vết quanh {@code center} (lệnh test dựng quanh người chơi). */
    public static void build(ServerWorld world, BlockPos center) {
        BlockPos hut = spot(world, center, 15, -11, 4, 2);
        // Đồi núi không có chỗ phẳng: lấy chỗ đất khô gần nhất rồi tự san nền (như người chơi san đất trước khi dựng)
        if (hut == null) hut = level(world, center, 15, -11, 4, 1);
        if (hut != null) {
            hut(world, hut);
            stairMine(world, hut.add(4, 0, 1), Direction.EAST);
            sunflowers(world, hut.add(0, 0, 5), hut.getY());
        }
        BlockPos stump = spot(world, center, -8, 13, 1, 2);
        if (stump != null) choppedStump(world, stump);
        BlockPos floating = spot(world, center, 11, 16, 1, 2);
        if (floating != null) floatingTrunk(world, floating);
        BlockPos pillar = spot(world, center, -13, -7, 1, 2);
        if (pillar != null) dirtPillar(world, pillar);
        LAST[0] = hut;
        LAST[1] = stump;
        LAST[2] = floating;
        LAST[3] = pillar;
    }

    /**
     * Lệnh test: đưa người chơi tới đứng nhìn dấu vết thứ {@code i} của lần dựng gần nhất (0 lán, 1 gốc chặt, 2 cây
     * mất gốc, 3 cột đất). Đứng cách 7 khối về phía nam, cao hơn 2 khối, nhìn vào nó. Trả về false nếu chưa dựng.
     */
    public static boolean view(net.minecraft.server.network.ServerPlayerEntity player, int i) {
        if (i < 0 || i >= LAST.length || LAST[i] == null) return false;
        BlockPos t = LAST[i];
        double tx = t.getX() + (i == 0 ? 2.0 : 0.5), tz = t.getZ() + (i == 0 ? 2.0 : 0.5);
        double ty = t.getY() + (i == 0 ? 1.5 : 3.0);
        double sx = tx - 3.0, sz = tz + 7.0;
        // Đứng trên mặt đất thật ở chỗ xem (dưới tán cây), cao hơn dấu vết ít nhất 1 khối
        double sy = Math.max(t.getY() + 1.0, top(world(player), (int) Math.floor(sx), (int) Math.floor(sz)).getY());
        double dx = tx - sx, dz = tz - sz, dy = ty - (sy + 1.62);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        player.teleport(player.getServerWorld(), sx, sy, sz, yaw, pitch);
        return true;
    }

    // ==========================================
    // CHỌN CHỖ
    // ==========================================

    /**
     * Mặt đất khô, khá phẳng cho khuôn {@code size}x{@code size} và cả vành {@code margin} khối quanh nó (để không
     * dựng sát mép vách: nhìn từ chân vách trông như một cái tháp), gần (center + dx, dz); thử vài chỗ quanh đó.
     * Trả về ô không khí đầu tiên trên mặt đất (góc tây bắc của khuôn), hoặc null.
     */
    private static BlockPos spot(ServerWorld world, BlockPos center, int dx, int dz, int size, int margin) {
        int[][] tries = {{0, 0}, {4, 0}, {-4, 0}, {0, 4}, {0, -4}, {7, 7}, {-7, 7}, {7, -7}, {-7, -7}};
        for (int[] t : tries) {
            BlockPos base = surface(world, center.getX() + dx + t[0], center.getZ() + dz + t[1]);
            if (base == null) continue;
            boolean ok = true;
            for (int x = -margin; x < size + margin && ok; x++) {
                for (int z = -margin; z < size + margin && ok; z++) {
                    boolean inside = x >= 0 && z >= 0 && x < size && z < size;
                    BlockPos p = surface(world, base.getX() + x, base.getZ() + z);
                    if (p == null) {
                        // Ô vành ngoài là nước thì vẫn được, miễn mặt nước không lệch quá 1 khối; ô trong khuôn phải là đất khô
                        ok = !inside && Math.abs(top(world, base.getX() + x, base.getZ() + z).getY() - base.getY()) <= 1;
                        continue;
                    }
                    ok = Math.abs(p.getY() - base.getY()) <= 1;
                }
            }
            if (ok) return base;
        }
        return null;
    }

    /** Ô trống đầu tiên trên mặt đất thật: bỏ qua thân cây và lá (rừng rậm thì cây nào cũng che mất mặt đất). */
    private static BlockPos top(ServerWorld world, int x, int z) {
        BlockPos.Mutable p = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z)).mutableCopy();
        for (int i = 0; i < 40; i++) {
            BlockState below = world.getBlockState(p.down());
            if (!below.isIn(BlockTags.LOGS) && !below.isIn(BlockTags.LEAVES)) break;
            p.move(Direction.DOWN);
        }
        return p.toImmutable();
    }

    /**
     * Không có chỗ phẳng: lấy chỗ đất khô đầu tiên quanh (center + dx, dz) và san phẳng khuôn {@code size}x{@code size}
     * cùng vành {@code margin} khối về đúng độ cao đó (đắp đất chỗ trũng, phạt chỗ cao, mặt là cỏ).
     */
    private static BlockPos level(ServerWorld world, BlockPos center, int dx, int dz, int size, int margin) {
        int[][] tries = {{0, 0}, {4, 0}, {-4, 0}, {0, 4}, {0, -4}, {7, 7}, {-7, 7}, {7, -7}, {-7, -7}};
        for (int[] t : tries) {
            BlockPos base = surface(world, center.getX() + dx + t[0], center.getZ() + dz + t[1]);
            if (base == null) continue;
            for (int x = -margin; x < size + margin; x++) {
                for (int z = -margin; z < size + margin; z++) {
                    BlockPos col = new BlockPos(base.getX() + x, base.getY(), base.getZ() + z);
                    for (int y = -4; y < -1; y++) {
                        if (world.getBlockState(col.up(y)).isReplaceable()) world.setBlockState(col.up(y), Blocks.DIRT.getDefaultState(), FLAGS);
                    }
                    world.setBlockState(col.down(), Blocks.GRASS_BLOCK.getDefaultState(), FLAGS);
                    for (int y = 0; y < 8; y++) world.setBlockState(col.up(y), Blocks.AIR.getDefaultState(), FLAGS);
                }
            }
            return base;
        }
        return null;
    }

    private static BlockPos surface(ServerWorld world, int x, int z) {
        BlockPos top = top(world, x, z);
        BlockState ground = world.getBlockState(top.down());
        if (!ground.getFluidState().isEmpty() || !ground.isSolidBlock(world, top.down())) return null;
        if (ground.isIn(BlockTags.LOGS)) return null;   // đỉnh một thân cây, không phải mặt đất
        return top;
    }

    // ==========================================
    // CÁC DẤU VẾT
    // ==========================================

    /** Lán đất 4x4 (trong lòng 2x2), cao 3, mái phẳng, lỗ cửa phía nam; trong có bàn chế tạo, lò nung, đuốc. */
    private static void hut(ServerWorld world, BlockPos o) {
        BlockState dirt = Blocks.DIRT.getDefaultState();
        BlockState air = Blocks.AIR.getDefaultState();
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                world.setBlockState(o.add(x, -1, z), dirt, FLAGS);
                boolean wall = x == 0 || x == 3 || z == 0 || z == 3;
                for (int y = 0; y < 3; y++) world.setBlockState(o.add(x, y, z), wall ? dirt : air, FLAGS);
                world.setBlockState(o.add(x, 3, z), dirt, FLAGS);
                for (int y = 4; y < 7; y++) world.setBlockState(o.add(x, y, z), air, FLAGS);
            }
        }
        // Lỗ cửa (không có cửa: đêm đầu tiên ai cũng đào một lỗ rồi lấp bằng đất)
        world.setBlockState(o.add(1, 0, 3), air, FLAGS);
        world.setBlockState(o.add(1, 1, 3), air, FLAGS);
        world.setBlockState(o.add(2, 0, 1), Blocks.CRAFTING_TABLE.getDefaultState(), FLAGS);
        world.setBlockState(o.add(1, 0, 1), Blocks.FURNACE.getDefaultState().with(FurnaceBlock.FACING, Direction.SOUTH), FLAGS);
        world.setBlockState(o.add(2, 0, 2), Blocks.TORCH.getDefaultState(), FLAGS);
    }

    /** Đường hầm bậc thang 1 khối đào chéo xuống, đuốc cắm trên bậc mỗi 5 bậc; gặp nước / dung nham thì dừng. */
    private static void stairMine(ServerWorld world, BlockPos start, Direction dir) {
        BlockState air = Blocks.AIR.getDefaultState();
        for (int i = 0; i < 18; i++) {
            BlockPos step = start.offset(dir, i).down(i + 1);
            for (int y = 1; y <= 3; y++) {
                if (!world.getFluidState(step.up(y)).isEmpty()) return;
            }
            if (!world.getBlockState(step).isSolidBlock(world, step)) world.setBlockState(step, Blocks.COBBLESTONE.getDefaultState(), FLAGS);
            for (int y = 1; y <= 3; y++) world.setBlockState(step.up(y), air, FLAGS);
            if (i % 5 == 2) world.setBlockState(step.up(), Blocks.TORCH.getDefaultState(), FLAGS);
        }
    }

    private static void sunflowers(ServerWorld world, BlockPos origin, int baseY) {
        int[][] at = {{0, 0}, {1, 1}, {2, 0}, {3, 1}, {1, -1}};
        for (int[] a : at) {
            BlockPos p = surface(world, origin.getX() + a[0], origin.getZ() + a[1]);
            if (p == null || Math.abs(p.getY() - baseY) > 1 || !world.getBlockState(p.down()).isIn(BlockTags.DIRT)) continue;
            if (!world.isAir(p) && !world.getBlockState(p).isReplaceable()) continue;
            if (!world.isAir(p.up())) continue;
            BlockState s = Blocks.SUNFLOWER.getDefaultState();
            world.setBlockState(p, s.with(TallPlantBlock.HALF, DoubleBlockHalf.LOWER), FLAGS);
            world.setBlockState(p.up(), s.with(TallPlantBlock.HALF, DoubleBlockHalf.UPPER), FLAGS);
        }
    }

    /** Gốc cây sồi còn trơ một khúc, tán lá lơ lửng phía trên (lá giữ nguyên, không rụng). */
    private static void choppedStump(ServerWorld world, BlockPos o) {
        world.setBlockState(o, Blocks.OAK_LOG.getDefaultState(), FLAGS);
        for (int y = 1; y < 4; y++) world.setBlockState(o.up(y), Blocks.AIR.getDefaultState(), FLAGS);
        canopy(world, o.up(5));
    }

    /** Cây sồi mất hai khúc gốc: thân còn lại và tán lá treo lơ lửng. */
    private static void floatingTrunk(ServerWorld world, BlockPos o) {
        for (int y = 0; y < 2; y++) world.setBlockState(o.up(y), Blocks.AIR.getDefaultState(), FLAGS);
        for (int y = 2; y < 5; y++) world.setBlockState(o.up(y), Blocks.OAK_LOG.getDefaultState(), FLAGS);
        canopy(world, o.up(5));
    }

    private static void canopy(ServerWorld world, BlockPos c) {
        BlockState leaves = Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    int r = dy == 1 ? 1 : 2;
                    if (Math.abs(dx) > r || Math.abs(dz) > r) continue;
                    if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
                    BlockPos p = c.add(dx, dy, dz);
                    if (world.isAir(p)) world.setBlockState(p, leaves, FLAGS);
                }
            }
        }
    }

    /** Cột đất 1x1 cao 9 khối giữa đồng. */
    private static void dirtPillar(ServerWorld world, BlockPos o) {
        for (int y = 0; y < 9; y++) world.setBlockState(o.up(y), Blocks.DIRT.getDefaultState(), FLAGS);
    }

    private static ServerWorld world(net.minecraft.server.network.ServerPlayerEntity player) {
        return player.getServerWorld();
    }

    // ==========================================
    // LƯU TRẠNG THÁI
    // ==========================================

    private static NamTraces fromNbt(NbtCompound nbt) {
        NamTraces s = new NamTraces();
        s.built = nbt.getBoolean("Built");
        return s;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putBoolean("Built", built);
        return nbt;
    }
}
