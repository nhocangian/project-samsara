package com.samsara.system.womb.chapters;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;

/**
 * Dựng thân người chơi (model Steve) bằng block, 1 pixel skin = 1 block, trong một hệ trục cục bộ:
 *   x = bên phải của tượng, y = phía đầu, z = phía mặt.
 * Hệ trục được gắn vào thế giới qua Frame (đứng, quỳ, hay nằm ngửa đều dùng chung code).
 * Tượng được "phong hoá": một phần block bị thay bằng đá cuội / đá rêu, thỉnh thoảng một vệt
 * hắc diện thạch khóc như ký ức rỉ ra.
 */
public final class TitanBody {
    /** Bảng màu skin (block thay cho màu pixel). */
    public record Skin(BlockState skin, BlockState hair, BlockState eyeWhite, BlockState iris, BlockState nose,
                       BlockState mouth, BlockState shirt, BlockState pants, BlockState shoes,
                       BlockState weatherA, BlockState weatherB) {
        /** Steve cổ điển. */
        public static Skin steve() {
            return new Skin(Blocks.WHITE_TERRACOTTA.getDefaultState(), Blocks.BROWN_TERRACOTTA.getDefaultState(),
                    Blocks.WHITE_CONCRETE.getDefaultState(), Blocks.BLUE_CONCRETE.getDefaultState(),
                    Blocks.TERRACOTTA.getDefaultState(), Blocks.BROWN_CONCRETE.getDefaultState(),
                    Blocks.CYAN_CONCRETE.getDefaultState(), Blocks.BLUE_CONCRETE.getDefaultState(),
                    Blocks.GRAY_CONCRETE.getDefaultState(),
                    Blocks.MOSSY_COBBLESTONE.getDefaultState(), Blocks.COBBLESTONE.getDefaultState());
        }

        /**
         * Tượng thạch cao trắng (chương III từ 07/11/2026): không màu skin, mắt để trống (hoặc {@code iris} nếu
         * muốn một chi tiết riêng), phong hoá thành vệt đá diorite như vết rạn.
         */
        public static Skin plaster(BlockState iris) {
            BlockState calcite = Blocks.CALCITE.getDefaultState();
            return new Skin(calcite, Blocks.SMOOTH_QUARTZ.getDefaultState(), calcite, iris, calcite, calcite,
                    Blocks.SMOOTH_QUARTZ.getDefaultState(), Blocks.POLISHED_DIORITE.getDefaultState(),
                    Blocks.DIORITE.getDefaultState(),
                    Blocks.DIORITE.getDefaultState(), Blocks.POLISHED_DIORITE.getDefaultState());
        }

        public static Skin plaster() {
            return plaster(Blocks.CALCITE.getDefaultState());
        }

        /** Kẻ phá hoại: áo đen, quần xám, mắt đỏ. Vẫn là gương mặt ấy. */
        public static Skin griefer() {
            return new Skin(Blocks.WHITE_TERRACOTTA.getDefaultState(), Blocks.BLACK_CONCRETE.getDefaultState(),
                    Blocks.WHITE_CONCRETE.getDefaultState(), Blocks.RED_CONCRETE.getDefaultState(),
                    Blocks.TERRACOTTA.getDefaultState(), Blocks.BLACK_TERRACOTTA.getDefaultState(),
                    Blocks.BLACK_CONCRETE.getDefaultState(), Blocks.GRAY_CONCRETE.getDefaultState(),
                    Blocks.BLACK_TERRACOTTA.getDefaultState(),
                    Blocks.MOSSY_COBBLESTONE.getDefaultState(), Blocks.COBBLESTONE.getDefaultState());
        }
    }

    /** Gắn hệ trục cục bộ (right, up, forward) vào thế giới tại base. */
    public record Frame(BlockPos base, Vec3i right, Vec3i up, Vec3i forward) {
        public BlockPos at(int x, int y, int z) {
            return base.add(right.getX() * x + up.getX() * y + forward.getX() * z,
                    right.getY() * x + up.getY() * y + forward.getY() * z,
                    right.getZ() * x + up.getZ() * y + forward.getZ() * z);
        }

        /** Đứng / quỳ thẳng, mặt hướng về {@code facing} (vector ngang). */
        public static Frame upright(BlockPos base, Vec3i facing) {
            Vec3i up = new Vec3i(0, 1, 0);
            return new Frame(base, facing.crossProduct(up), up, facing);
        }

        /** Nằm ngửa trên mặt nước, đỉnh đầu hướng về {@code headDir} (vector ngang). */
        public static Frame lyingFaceUp(BlockPos base, Vec3i headDir) {
            Vec3i fwd = new Vec3i(0, 1, 0);
            return new Frame(base, fwd.crossProduct(headDir), headDir, fwd);
        }
    }

    // Mặt Steve 8x8, hàng trên cùng trước. H tóc, S da, W lòng trắng, I mống mắt, N mũi, M miệng
    private static final String[] FACE = {
            "HHHHHHHH",
            "HHHHHHHH",
            "HSSSSSSH",
            "SSSSSSSS",
            "SWISSIWS",
            "SSSNNSSS",
            "SSMSSMSS",
            "SSMMMMSS",
    };

    private final ServerWorld world;
    private final Frame frame;
    private final Skin skin;
    private final Random random;
    private final float weathering;

    public TitanBody(ServerWorld world, Frame frame, Skin skin, Random random, float weathering) {
        this.world = world;
        this.frame = frame;
        this.skin = skin;
        this.random = random;
        this.weathering = weathering;
    }

    // ==========================================
    // CÁC BỘ PHẬN (toạ độ cục bộ, gốc ở giữa hai bàn chân khi đứng)
    // ==========================================

    /** Đầu 8x8x8 với gốc dưới ở (x0, y0, z0) = góc trái-dưới-sau. Mặt ở z = z0 + 7. */
    public void head(int x0, int y0, int z0) {
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                for (int z = 0; z < 8; z++) {
                    boolean shell = x == 0 || x == 7 || y == 0 || y == 7 || z == 0 || z == 7;
                    if (!shell) continue;
                    int row = 7 - y;
                    BlockState s;
                    if (z == 7) s = faceBlock(FACE[row].charAt(7 - x));     // nhìn từ trước: trái người xem = phải tượng
                    else if (y == 7 || z == 0) s = skin.hair();
                    else if (row <= 1) s = skin.hair();
                    else s = skin.skin();
                    put(x0 + x, y0 + y, z0 + z, s);
                }
            }
        }
    }

    /** Thân áo 8x12x4. */
    public void torso(int x0, int y0, int z0) {
        box(x0, y0, z0, 8, 12, 4, (x, y, z) -> skin.shirt());
    }

    /** Tay 4x12x4 dọc theo trục y cục bộ (đầu tay áo ở trên). */
    public void armDown(int x0, int y0, int z0) {
        box(x0, y0, z0, 4, 12, 4, (x, y, z) -> y >= 8 ? skin.shirt() : skin.skin());
    }

    /** Tay giơ thẳng lên trời (vai ở dưới, bàn tay ở trên). */
    public void armUp(int x0, int y0, int z0) {
        box(x0, y0, z0, 4, 12, 4, (x, y, z) -> y < 4 ? skin.shirt() : skin.skin());
    }

    /** Tay đưa thẳng ra trước (vai ở z0, bàn tay ở z0 + 11). */
    public void armForward(int x0, int y0, int z0) {
        box(x0, y0, z0, 4, 4, 12, (x, y, z) -> z < 4 ? skin.shirt() : skin.skin());
    }

    /** Chân 4x{len}x4, giày ở 1 hàng dưới cùng nếu shoes = true. */
    public void leg(int x0, int y0, int z0, int len, boolean shoes) {
        box(x0, y0, z0, 4, len, 4, (x, y, z) -> shoes && y == 0 ? skin.shoes() : skin.pants());
    }

    /** Cẳng chân nằm ngang ra sau (gối ở z0 + len - 1, bàn chân ở z0). */
    public void shinBack(int x0, int y0, int z0, int len) {
        box(x0, y0, z0, 4, 4, len, (x, y, z) -> z == 0 ? skin.shoes() : skin.pants());
    }

    // ==========================================
    // TIỆN ÍCH
    // ==========================================

    public interface Paint {
        BlockState at(int x, int y, int z);
    }

    public void box(int x0, int y0, int z0, int sx, int sy, int sz, Paint paint) {
        for (int x = 0; x < sx; x++) {
            for (int y = 0; y < sy; y++) {
                for (int z = 0; z < sz; z++) {
                    put(x0 + x, y0 + y, z0 + z, paint.at(x, y, z));
                }
            }
        }
    }

    /** Đặt một block (toạ độ cục bộ) có phong hoá. */
    public void put(int x, int y, int z, BlockState state) {
        BlockPos pos = frame.at(x, y, z);
        float r = random.nextFloat();
        if (r < weathering * 0.6f) state = skin.weatherA();
        else if (r < weathering) state = skin.weatherB();
        else if (r < weathering + 0.015f && !skin.skin().isOf(Blocks.CALCITE)) state = Blocks.CRYING_OBSIDIAN.getDefaultState(); // thạch cao: không vệt tím
        world.setBlockState(pos, state, 2);
    }

    /** Đặt block không phong hoá (đồ vật, chi tiết cần rõ nét). */
    public void putExact(int x, int y, int z, BlockState state) {
        world.setBlockState(frame.at(x, y, z), state, 2);
    }

    public Frame frame() {
        return frame;
    }

    private BlockState faceBlock(char c) {
        return switch (c) {
            case 'H' -> skin.hair();
            case 'W' -> skin.eyeWhite();
            case 'I' -> skin.iris();
            case 'N' -> skin.nose();
            case 'M' -> skin.mouth();
            default -> skin.skin();
        };
    }
}
