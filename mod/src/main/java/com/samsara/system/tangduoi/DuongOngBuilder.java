package com.samsara.system.tangduoi;

import com.samsara.block.DenChopBlock;
import com.samsara.block.DenOngBlock;
import com.samsara.block.MemorySunflowerBlock;
import com.samsara.block.WombCodeBlock;
import com.samsara.registry.SamsaraBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tầng Dưới, tầng 1 "Hoa Quay Lưng": năm ngã tư nối với nhau bằng ống thật, KHÔNG có dịch chuyển nào
 * (vân ướt của shader tính theo toạ độ thế giới, dịch chuyển sẽ làm nó nhảy).
 *
 * Mỗi ngã có toạ độ tâm trong {@link #CENTER}; toạ độ (x, z) tương đối so với tâm, người chơi tới từ phía nam.
 *   Ống vào: x = 0, từ z = 40 lên ngã tư (0, 0).
 *   Nhánh sai là đường vòng thật, nhập lại ống vào từ bên hông:
 *     tây / đông: ra x = ±20, xuống tới z = 14, nhập ở (0, 14);
 *     bắc: lên z = -16, ra x = ±34 (phía không có lối đúng), xuống z = 28, nhập ở (0, 28).
 *   Lối đúng nối thẳng sang điểm đầu ống vào của ngã sau.
 */
public final class DuongOngBuilder {
    private DuongOngBuilder() {}

    /** Góc đặt tầng 1 trong chiều Tầng Dưới; {@code ORIGIN.y} là tầng nước. */
    public static final BlockPos ORIGIN = new BlockPos(0, 64, 0);
    public static final int JUNCTIONS = 5;

    /** Tâm các ngã; phần tử cuối là đoạn cụt sau ngã 5 (bước 3 sẽ có cửa gỗ và phòng ngủ ở đây). */
    public static final int[][] CENTER = {{0, 0}, {0, -70}, {60, -110}, {0, -150}, {0, -220}, {60, -260}};
    /** Lối đúng của từng ngã: 0 = bắc, 1 = tây, 2 = đông. */
    public static final int[] CORRECT = {0, 2, 1, 0, 2};

    public static final int ENTRY_LENGTH = 40;
    private static final int INNER_X = 20;
    private static final int INNER_MERGE_Z = 14;
    private static final int OUTER_X = 34;
    private static final int OUTER_TOP_Z = -16;
    private static final int OUTER_MERGE_Z = 28;

    /** Ngã 4 không có đèn trong bán kính này: chỉ thấy hoa qua máy quay. */
    private static final int DARK_PLOT = 3;
    private static final int DARK_RADIUS = 24;
    /** Ngã 5 không còn hoa, chỉ còn dòng nước. */
    private static final int EMPTY_PLOT = 4;

    /** Tỉ lệ ô gạch glitch và số cụm Khối Mã: tổng khoảng 5..10% bề mặt (người dùng yêu cầu). */
    private static final float GLITCH_RATE = 0.05f;
    private static final float CODE_RATE = 0.004f;

    /** Tỉ lệ đèn hỏng / chập chờn: ngẫu nhiên, để chỗ tối không lặp theo nhịp (không bao giờ hai đèn hỏng liền nhau). */
    private static final float BROKEN_RATE = 0.12f;
    private static final float FLICKER_RATE = 0.075f;
    private static final float DARK_GAP_CHANCE = 0.09f;

    /** Đèn hỏng / chập chờn đầu tiên của lần dựng gần nhất (lệnh test đưa người chơi tới xem). */
    public static BlockPos firstDrip = null;
    private static final float DRIP_RATE = 0.025f;
    public static BlockPos firstBroken = null;
    public static BlockPos firstFlicker = null;

    /** Dãy dấu chân: plot, x tương đối (gờ không có miệng ống nhập), z bắt đầu, z dừng. */
    private static final int[][] FOOTPRINTS = {{0, 3, 32, 22}, {1, 3, 34, 19}, {2, -3, 33, 24}, {4, 3, 32, 18}};
    /** Đèn thường, để tắt dần khi người chơi đứng yên quá lâu. */
    public static final List<BlockPos> LAMPS = new ArrayList<>();
    /** Đèn chưa được đặt lại để tính sáng. */
    public static final List<BlockPos> RELIGHT = new ArrayList<>();

    /** Các đoạn ống thẳng để rải đèn: plot, x0, z0, x1, z1 (tương đối). */
    private static final List<int[]> SEGMENTS = new ArrayList<>();

    private static BlockPos at(int plot, int x, int y, int z) {
        return ORIGIN.add(CENTER[plot][0] + x, y, CENTER[plot][1] + z);
    }

    /** Chỗ đứng giữa ống trước ngã {@code plot}, nhìn về bắc. */
    public static BlockPos before(int plot) {
        return at(plot, 0, 0, 10);
    }

    public static BlockPos start() {
        return at(0, 0, 0, 36);
    }

    public static BlockPos entryLamp(int plot) {
        return at(plot, 0, 6, 8);
    }

    public static BlockPos wrongSignPos(int plot) {
        return at(plot, 3, 2, 9);
    }

    /** Hướng lối đúng vòng sang: nhánh bắc sai đi về phía ngược với lối đúng. */
    private static int outerSide(int plot) {
        return CORRECT[plot] == 2 ? -1 : 1;
    }

    public static boolean isNorthWrong(int plot) {
        return CORRECT[plot] != 0;
    }

    /** Vạch đếm lần sai trên nhánh vòng: {x tương đối, z tương đối}; đi qua sau khi đã tới ngã tư. */
    public static List<int[]> wrongGates(int plot) {
        List<int[]> gates = new ArrayList<>();
        if (CORRECT[plot] != 1) gates.add(new int[]{-8, INNER_MERGE_Z});
        if (CORRECT[plot] != 2) gates.add(new int[]{8, INNER_MERGE_Z});
        if (isNorthWrong(plot)) gates.add(new int[]{outerSide(plot) * 8, OUTER_MERGE_Z});
        return gates;
    }

    /** Đường dòng chảy, gồm các điểm tuyệt đối theo (x, z), từ đầu ống vào của ngã tới đầu ống vào của ngã sau. */
    public static int[][] route(int plot) {
        int cx = CENTER[plot][0];
        int cz = CENTER[plot][1];
        int nx = CENTER[plot + 1][0];
        int nz = CENTER[plot + 1][1] + ENTRY_LENGTH;
        return new int[][]{{cx, cz + ENTRY_LENGTH}, {cx, cz}, {nx, cz}, {nx, nz}};
    }

    public static void buildTest(ServerWorld world) {
        firstBroken = null;
        firstFlicker = null;
        SEGMENTS.clear();
        LAMPS.clear();

        // Xoá hẳn vùng cũ để dựng lại không sót khối
        BlockState air = Blocks.AIR.getDefaultState();
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int x = -60; x <= 125; x++) {
            for (int z = -310; z <= 55; z++) {
                for (int y = ORIGIN.getY() - 2; y <= ORIGIN.getY() + 10; y++) {
                    world.setBlockState(m.set(x, y, z), air, 2);
                }
            }
        }

        Set<BlockPos> inner = new HashSet<>();
        for (int p = 0; p < JUNCTIONS; p++) carvePlot(inner, p);
        seg(inner, JUNCTIONS, 0, ENTRY_LENGTH, 0, 0);

        Random random = Random.create(0x0D6E0L);
        Set<BlockPos> shell = new HashSet<>();
        for (BlockPos p : inner) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos n = p.add(dx, dy, dz);
                        if (!inner.contains(n)) shell.add(n);
                    }
                }
            }
        }

        // Sau bảng gỗ không đặt glitch
        Set<BlockPos> plain = new HashSet<>();
        plain.add(at(0, -4, 2, 34));
        for (int p = 0; p < JUNCTIONS; p++) plain.add(at(p, 4, 2, 9));

        BlockState tile = SamsaraBlocks.GACH_MEN_O.getDefaultState();
        BlockState glitch = SamsaraBlocks.GACH_MEN_GLITCH.getDefaultState();
        for (BlockPos p : shell) {
            world.setBlockState(p, !plain.contains(p) && random.nextFloat() < GLITCH_RATE ? glitch : tile, 2);
        }
        BlockState water = Blocks.WATER.getDefaultState();
        for (BlockPos p : inner) {
            world.setBlockState(p, p.getY() == ORIGIN.getY() ? water : air, 2);
        }

        // Khối Mã thành cụm nhỏ
        BlockState code = SamsaraBlocks.WOMB_CODE.getDefaultState();
        List<BlockPos> all = new ArrayList<>(shell);
        all.removeAll(plain);
        all.sort(BlockPos::compareTo);          // thứ tự cố định: dựng lại vẫn ra cùng một kết quả
        List<BlockPos> placed = new ArrayList<>();
        int clusters = Math.max(1, (int) (all.size() * CODE_RATE));
        for (int i = 0; i < clusters; i++) {
            BlockPos c = all.get(random.nextInt(all.size()));
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos p = c.add(dx, dy, dz);
                        if (shell.contains(p) && !plain.contains(p) && random.nextFloat() < 0.3f) {
                            world.setBlockState(p, WombCodeBlock.at(code, p), 2);
                            placed.add(p);
                        }
                    }
                }
            }
        }
        for (BlockPos p : placed) WombCodeBlock.refresh(world, p);

        // Vài viên gạch trần rỉ nước (đặt trước đèn: chỗ nào là đèn thì đèn đè lên)
        BlockState drip = SamsaraBlocks.GACH_MEN_RI.getDefaultState();
        firstDrip = null;
        for (BlockPos p : all) {
            if (p.getY() == ORIGIN.getY() + 6 && world.getBlockState(p).isOf(SamsaraBlocks.GACH_MEN_O) && random.nextFloat() < DRIP_RATE) {
                world.setBlockState(p, drip, 2);
                if (firstDrip == null) firstDrip = p;
            }
        }

        for (int[] s : SEGMENTS) placeLampLine(world, shell, random, s);
        for (int p = 0; p < JUNCTIONS; p++) {
            if (p != DARK_PLOT) {
                world.setBlockState(at(p, 0, 8, 0), SamsaraBlocks.DEN_HUYNH_QUANG.getDefaultState(), 2);
                world.setBlockState(entryLamp(p), SamsaraBlocks.DEN_HUYNH_QUANG.getDefaultState(), 2);
                LAMPS.add(entryLamp(p));
            }
            placeFlowers(world, p);
        }

        // Dấu chân ướt trên gờ ống, đi cùng chiều người chơi rồi dừng giữa chừng, không có đường quay về
        BlockState prints = SamsaraBlocks.DAU_CHAN_GACH.getDefaultState().with(com.samsara.block.DockPropBlock.FACING, Direction.NORTH);
        for (int[] f : FOOTPRINTS) {
            for (int z = f[2]; z >= f[3]; z -= 2) world.setBlockState(at(f[0], f[1], 1, z), prints, 2);
        }

        // Ánh sáng vừa dựng có thể sót ô tối: đèn sẽ được đặt lại lần lượt khi người chơi tới gần (DuongOngRuntime)
        RELIGHT.clear();
        TangDuoiRoom.build(world);
        for (BlockPos p : shell) {
            BlockState s = world.getBlockState(p);
            if (s.getLuminance() > 0 && !s.isOf(SamsaraBlocks.DEN_CHOP)) RELIGHT.add(p);
        }

        placeSign(world, at(0, -3, 2, 34), Direction.EAST, "samsara.tang_duoi.bang_dau_1", "samsara.tang_duoi.bang_dau_2", null,
                "samsara.tang_duoi.bang_dau_4");
    }

    // ==========================================
    // HÌNH DÁNG
    // ==========================================

    private static void carvePlot(Set<BlockPos> inner, int plot) {
        seg(inner, plot, 0, ENTRY_LENGTH, 0, 6);
        junction(inner, plot);
        int correct = CORRECT[plot];
        int rx = CENTER[plot + 1][0] - CENTER[plot][0];
        int rz = CENTER[plot + 1][1] + ENTRY_LENGTH - CENTER[plot][1];

        // Bắc
        if (correct == 0) {
            seg(inner, plot, 0, -6, 0, rz);
        } else {
            int s = outerSide(plot);
            seg(inner, plot, 0, -6, 0, OUTER_TOP_Z);
            seg(inner, plot, 0, OUTER_TOP_Z, s * OUTER_X, OUTER_TOP_Z);
            seg(inner, plot, s * OUTER_X, OUTER_TOP_Z, s * OUTER_X, OUTER_MERGE_Z);
            seg(inner, plot, s * OUTER_X, OUTER_MERGE_Z, 0, OUTER_MERGE_Z);
        }
        // Tây
        if (correct == 1) {
            seg(inner, plot, -6, 0, rx, 0);
        } else {
            seg(inner, plot, -6, 0, -INNER_X, 0);
            seg(inner, plot, -INNER_X, 0, -INNER_X, INNER_MERGE_Z);
            seg(inner, plot, -INNER_X, INNER_MERGE_Z, 0, INNER_MERGE_Z);
        }
        // Đông
        if (correct == 2) {
            seg(inner, plot, 6, 0, rx, 0);
        } else {
            seg(inner, plot, 6, 0, INNER_X, 0);
            seg(inner, plot, INNER_X, 0, INNER_X, INNER_MERGE_Z);
            seg(inner, plot, INNER_X, INNER_MERGE_Z, 0, INNER_MERGE_Z);
        }
    }

    /** Khoét một đoạn ống thẳng (theo trục) và nhớ lại để rải đèn. */
    private static void seg(Set<BlockPos> inner, int plot, int x0, int z0, int x1, int z1) {
        SEGMENTS.add(new int[]{plot, x0, z0, x1, z1});
        if (x0 == x1) {
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                for (int a = -3; a <= 3; a++) {
                    for (int y = 0; y <= 5; y++) {
                        if (inSection(a, y)) inner.add(at(plot, x0 + a, y, z));
                    }
                }
            }
        } else {
            for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
                for (int a = -3; a <= 3; a++) {
                    for (int y = 0; y <= 5; y++) {
                        if (inSection(a, y)) inner.add(at(plot, x, y, z0 + a));
                    }
                }
            }
        }
    }

    /** Mặt cắt ống tròn: rộng 7, cao 6, vát cả trên lẫn dưới (sàn nước rộng 5); {@code a} = độ lệch ngang so với tim ống. */
    private static boolean inSection(int a, int y) {
        if (y == 0 || y == 4) return Math.abs(a) <= 2;
        if (y == 5) return Math.abs(a) <= 1;
        return y >= 1 && y <= 3 && Math.abs(a) <= 3;
    }

    /** Ngã tư: phòng tròn, trần vòm cao hơn ống. */
    private static void junction(Set<BlockPos> inner, int plot) {
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > 6.6) continue;
                int top = d < 2.5 ? 7 : d < 4.5 ? 6 : d < 5.8 ? 5 : 4;
                for (int y = 0; y <= top; y++) inner.add(at(plot, dx, y, dz));
            }
        }
    }

    // ==========================================
    // ĐÈN
    // ==========================================

    /** Đèn trên trần dọc một đoạn ống, cách quãng không đều. */
    private static void placeLampLine(ServerWorld world, Set<BlockPos> shell, Random random, int[] s) {
        int plot = s[0];
        int dx = Integer.signum(s[3] - s[1]);
        int dz = Integer.signum(s[4] - s[2]);
        int len = Math.abs(s[3] - s[1]) + Math.abs(s[4] - s[2]);
        boolean lastLit = true;
        for (int t = 2; t <= len; t += 6 + random.nextInt(4)) {
            // Thỉnh thoảng cả một quãng không có đèn: tối ở chỗ bất ngờ chứ không đều nhịp
            if (random.nextFloat() < DARK_GAP_CHANCE) {
                t += 7 + random.nextInt(8);
                if (t > len) break;
            }
            int x = s[1] + dx * t;
            int z = s[2] + dz * t;
            if (plot == DARK_PLOT && x * x + z * z <= DARK_RADIUS * DARK_RADIUS) continue;
            lastLit = placeLamp(world, shell, at(plot, x, 6, z), random, dx != 0 ? Direction.Axis.X : Direction.Axis.Z, lastLit);
        }
    }

    private static BlockState lampOf(net.minecraft.block.Block block, Direction.Axis axis) {
        return block.getDefaultState().with(DenOngBlock.AXIS, axis);
    }

    private static boolean placeLamp(ServerWorld world, Set<BlockPos> shell, BlockPos p, Random random, Direction.Axis axis, boolean lastLit) {
        if (!shell.contains(p)) return lastLit;
        boolean lit = !lastLit || random.nextFloat() >= BROKEN_RATE;
        if (!lit) {
            world.setBlockState(p, lampOf(SamsaraBlocks.DEN_HONG, axis), 2);
            if (firstBroken == null) firstBroken = p;
        } else if (random.nextFloat() < FLICKER_RATE) {
            world.setBlockState(p, lampOf(SamsaraBlocks.DEN_CHOP, axis), 2);
            world.scheduleBlockTick(p, SamsaraBlocks.DEN_CHOP, 10 + random.nextInt(40));
            if (firstFlicker == null) firstFlicker = p;
        } else {
            world.setBlockState(p, lampOf(SamsaraBlocks.DEN_HUYNH_QUANG, axis), 2);
            LAMPS.add(p);
        }
        return lit;
    }

    /** Lần sai thứ 2: đèn phía sau tắt hẳn. */
    public static void killEntryLamp(ServerWorld world, int plot) {
        world.setBlockState(entryLamp(plot), SamsaraBlocks.DEN_CHOP.getDefaultState().with(DenChopBlock.LIT, false), 3);
    }

    // ==========================================
    // HOA VÀ BẢNG
    // ==========================================

    /** Hoa nhìn về nhà: lối đúng nằm phía sau lưng hoa. */
    private static void placeFlowers(ServerWorld world, int plot) {
        if (plot == EMPTY_PLOT) {
            world.setBlockState(at(plot, 0, 0, 0), SamsaraBlocks.GACH_MEN_O.getDefaultState(), 2);
            return;
        }
        Direction look = switch (CORRECT[plot]) {
            case 0 -> Direction.SOUTH;
            case 1 -> Direction.EAST;
            default -> Direction.WEST;
        };
        if (plot == 2) {
            // Ngã 3: hai bông, chỉ bông còn sống mới đúng
            flower(world, plot, 1, SamsaraBlocks.MEMORY_SUNFLOWER.getDefaultState(), look);
            flower(world, plot, -1, SamsaraBlocks.HOA_KHO.getDefaultState(), look.getOpposite());
        } else {
            flower(world, plot, 0, SamsaraBlocks.MEMORY_SUNFLOWER.getDefaultState(), look);
        }
    }

    private static void flower(ServerWorld world, int plot, int x, BlockState base, Direction look) {
        world.setBlockState(at(plot, x, 0, 0), SamsaraBlocks.GACH_MEN_O.getDefaultState(), 2);
        BlockState s = base.with(MemorySunflowerBlock.FACING, look);
        // Cờ 2 | 16: không chạy cập nhật hình dạng, nếu không cây tự rụng vì đứng trên gạch chứ không phải đất
        world.setBlockState(at(plot, x, 1, 0), s.with(TallPlantBlock.HALF, DoubleBlockHalf.LOWER), 2 | 16);
        world.setBlockState(at(plot, x, 2, 0), s.with(TallPlantBlock.HALF, DoubleBlockHalf.UPPER), 2 | 16);
    }

    /** Bảng gỗ gắn vách; {@code facing} là hướng mặt chữ. Mỗi dòng là một khoá ngôn ngữ, null = dòng trống. */
    public static void placeSign(ServerWorld world, BlockPos pos, Direction facing, String... keys) {
        world.setBlockState(pos, Blocks.OAK_WALL_SIGN.getDefaultState().with(WallSignBlock.FACING, facing), 2);
        if (world.getBlockEntity(pos) instanceof SignBlockEntity sign) {
            SignText text = new SignText();
            for (int i = 0; i < keys.length && i < 4; i++) {
                if (keys[i] != null) text = text.withMessage(i, Text.translatable(keys[i]));
            }
            sign.setText(text, true);
            sign.markDirty();
            world.updateListeners(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        }
    }

    /** Lần sai thứ 5: lối đúng sáng lên rất nhẹ, để không ai bị kẹt. */
    public static void lightCorrectBranch(ServerWorld world, int plot) {
        BlockState light = Blocks.LIGHT.getDefaultState().with(net.minecraft.block.LightBlock.LEVEL_15, 7);
        switch (CORRECT[plot]) {
            case 0 -> { for (int z = -8; z >= -18; z -= 3) world.setBlockState(at(plot, 0, 1, z), light, 3); }
            case 1 -> { for (int x = -8; x >= -26; x -= 3) world.setBlockState(at(plot, x, 1, 0), light, 3); }
            default -> { for (int x = 8; x <= 26; x += 3) world.setBlockState(at(plot, x, 1, 0), light, 3); }
        }
    }
}
