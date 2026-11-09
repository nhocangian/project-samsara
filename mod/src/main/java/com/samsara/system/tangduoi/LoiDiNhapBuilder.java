package com.samsara.system.tangduoi;

import com.samsara.block.DenOngBlock;
import com.samsara.block.WombCodeBlock;
import com.samsara.registry.SamsaraBlocks;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.SignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

/**
 * Tầng Dưới, tầng 2 "Lối đi nháp" (Sáng tạo-KHÔNG XÓA 08/2016): biển phẳng dưới trời sáng (chiều riêng), đầu nam là bản nháp căn
 * phòng 2018 và một đoạn ống đứt ngang; người chơi đi về bắc bằng Khối Nháp. Toạ độ tương đối so với {@link #ORIGIN}; y = 0 là mặt nước.
 */
public final class LoiDiNhapBuilder {
    private LoiDiNhapBuilder() {}

    public static final BlockPos ORIGIN = new BlockPos(0, 64, 0);

    private static final int FLAGS = 2 | 16;

    private static BlockPos at(int x, int y, int z) {
        return ORIGIN.add(x, y, z);
    }

    /** Chỗ tỉnh dậy, cạnh giường đỏ, nhìn ra bể. */
    public static BlockPos arrival() {
        return at(-2, 1, 12);
    }

    /** Cuối cầu ván: chỗ Khối Nháp bắt đầu (lệnh test). */
    public static BlockPos pierEnd() {
        return at(0, 1, -7);
    }

    /** Đảo thứ nhất (lệnh test). */
    public static BlockPos islandOne() {
        return at(3, 1, -38);
    }

    public static boolean isBuilt(ServerWorld world) {
        return world.getBlockState(arrival().down()).isOf(Blocks.OAK_PLANKS)
                && world.getBlockState(FINAL_BED).isOf(Blocks.RED_BED);
    }

    // ==========================================
    // LỘ TRÌNH: toạ độ tương đối (x, z), người chơi đi về bắc
    // ==========================================

    public static final int SECTIONS = 5;
    public static final int ERASE_SECTION = 2;
    public static final int ERASE_START_Z = -105;
    private static final int STATIC_FROM_Z = -194;
    private static final int FINAL_ROOM_Z = -215;
    private static final BlockPos FINAL_BED = at(0, 1, -228);

    /** Điểm nổi lên của từng đoạn: cuối cầu, rồi mép bắc mỗi đảo. */
    private static final int[][] CHECKPOINTS = {{0, -7}, {3, -50}, {1, -104}, {2, -149}, {1, -192}};
    /** Đã vượt qua z này (đi về bắc) thì đoạn tương ứng trở thành điểm nổi. */
    private static final int[] CHECKPOINT_Z = {100, -37, -91, -135, -190};

    private static final int[][] LINE_TWO = {{3, -52}, {3, -60}, {9, -60}, {9, -70}, {-3, -70}, {-3, -82}, {1, -82}, {1, -90}};
    private static final int[][] LINE_THREE = {{1, -106}, {1, -116}, {8, -116}, {8, -128}, {2, -128}, {2, -134}};
    private static final int[][] LINE_FOUR = {{0, -151}, {0, -160}, {-3, -160}, {-3, -172}, {0, -172}, {0, -189}};
    private static final int[][] LINE_FIVE = {{1, -195}, {1, -201}, {-2, -201}, {-2, -208}, {2, -208}, {2, -214}};

    private static final List<List<BlockPos>> SECTION_TILES = new ArrayList<>();

    static {
        List<BlockPos> first = new ArrayList<>();
        for (int z = -7; z >= -36; z--) {
            int from = z >= -23 ? -1 : 2;
            int to = z >= -20 ? 1 : 4;
            for (int x = from; x <= to; x++) {
                boolean pier = (z == -7 && x <= 0) || (z == -8 && x == 0);
                if (!pier) first.add(at(x, 0, z));
            }
        }
        SECTION_TILES.add(first);
        SECTION_TILES.add(tiles(LINE_TWO));
        SECTION_TILES.add(tiles(LINE_THREE));
        SECTION_TILES.add(tiles(LINE_FOUR));
        SECTION_TILES.add(tiles(LINE_FIVE));
    }

    private static List<BlockPos> tiles(int[][] line) {
        List<BlockPos> out = new ArrayList<>();
        out.add(at(line[0][0], 0, line[0][1]));
        for (int i = 0; i + 1 < line.length; i++) {
            int dx = Integer.signum(line[i + 1][0] - line[i][0]);
            int dz = Integer.signum(line[i + 1][1] - line[i][1]);
            int x = line[i][0];
            int z = line[i][1];
            while (x != line[i + 1][0] || z != line[i + 1][1]) {
                x += dx;
                z += dz;
                out.add(at(x, 0, z));
            }
        }
        return out;
    }

    public static List<BlockPos> section(int i) {
        return SECTION_TILES.get(i);
    }

    public static BlockPos checkpoint(int i) {
        return at(CHECKPOINTS[i][0], 1, CHECKPOINTS[i][1]);
    }

    public static int checkpointAt(double relativeZ) {
        int cp = 0;
        for (int i = 0; i < CHECKPOINT_Z.length; i++) {
            if (relativeZ <= CHECKPOINT_Z[i]) cp = i;
        }
        return cp;
    }

    /** Đoạn 5: máy quay chỉ thấy nhiễu. Nhận z tuyệt đối. */
    public static boolean isStaticZone(double worldZ) {
        double z = worldZ - ORIGIN.getZ();
        return z < STATIC_FROM_Z && z > FINAL_ROOM_Z;
    }

    /** Độ nhiễu của máy quay (0..1): dày dần trong 5 block đầu đoạn 5. Nhận z tuyệt đối. */
    public static float staticAmount(double worldZ) {
        if (!isStaticZone(worldZ)) return 0.0f;
        return (float) Math.min(1.0, (STATIC_FROM_Z - (worldZ - ORIGIN.getZ())) / 5.0);
    }

    /** Đã vào phòng cuối. Nhận z tuyệt đối. */
    public static boolean isFinalRoom(double worldZ) {
        return worldZ - ORIGIN.getZ() < FINAL_ROOM_Z;
    }

    public static boolean isFinalBed(BlockPos pos) {
        return pos.equals(FINAL_BED) || pos.equals(FINAL_BED.south());
    }

    public static BlockPos finalRoom() {
        return at(0, 1, -218);
    }

    public static void restoreErasing(ServerWorld world) {
        BlockState draft = SamsaraBlocks.KHOI_NHAP.getDefaultState();
        for (BlockPos t : section(ERASE_SECTION)) {
            if (!world.getBlockState(t).isOf(SamsaraBlocks.KHOI_NHAP)) world.setBlockState(t, draft, FLAGS);
        }
    }

    public static void placeRedoSign(ServerWorld world, int cp) {
        BlockPos p = checkpoint(cp).add(2, 0, 0);
        standingSign(world, p, 0, null, "samsara.tang_duoi2.nhap_thi", "samsara.tang_duoi2.lam_lai_duoc");
    }

    public static void build(ServerWorld world) {
        LoiDiNhapKhoi.removeAll(world);
        Random random = Random.create(0x2016L);
        buildBrokenPipe(world, random);
        buildFloatingLamps(world);
        buildWallFragments(world, random);
        buildDraftRoom(world);
        buildPier(world);
        buildSectionOne(world);
        buildIslandOne(world);
        buildLaterSections(world);
        buildFinalRoom(world, random);
    }

    // ==========================================
    // ĐOẠN ỐNG CHƯA XÂY XONG VÀ DẤU VẾT ĐƯỜNG ỐNG
    // ==========================================

    /** Nền gạch dưới căn phòng; ống gạch men đi ra biển rồi đứt ngang, trần và tường rụng dần về phía bắc. */
    private static void buildBrokenPipe(ServerWorld world, Random random) {
        for (int x = -9; x <= 9; x++) {
            for (int z = 0; z <= 16; z++) {
                for (int y = -3; y <= 0; y++) world.setBlockState(at(x, y, z), tile(random), FLAGS);
            }
        }
        for (int z = 0; z >= -11; z--) {
            // Từ z = -5 trở đi ống cụt dần, mỗi cột một độ cao
            for (int x = -5; x <= 5; x++) {
                int bite = z > -5 ? 0 : (-5 - z) + ((x * 7 + z * 3) & 3);
                boolean wall = Math.abs(x) == 5;
                if (wall) {
                    for (int y = -3; y <= 6 - bite; y++) world.setBlockState(at(x, y, z), tile(random), FLAGS);
                } else if (bite < 3) {
                    world.setBlockState(at(x, 6, z), tile(random), FLAGS);
                }
            }
        }
        world.setBlockState(at(0, 6, -2), SamsaraBlocks.DEN_HUYNH_QUANG.getDefaultState().with(DenOngBlock.AXIS, Direction.Axis.Z), FLAGS);
    }

    private static BlockState tile(Random random) {
        return (random.nextFloat() < 0.05f ? SamsaraBlocks.GACH_MEN_GLITCH : SamsaraBlocks.GACH_MEN_O).getDefaultState();
    }

    /** Hai hàng đèn huỳnh quang lơ lửng giữa trời, không gắn vào đâu. */
    private static void buildFloatingLamps(ServerWorld world) {
        BlockState lamp = SamsaraBlocks.DEN_HUYNH_QUANG.getDefaultState().with(DenOngBlock.AXIS, Direction.Axis.Z);
        for (int z = -18; z >= -140; z -= 13) {
            world.setBlockState(at(-8, 7, z), lamp, FLAGS);
            world.setBlockState(at(13, 7, z - 5), lamp, FLAGS);
        }
    }

    /** Vài mảng tường gạch đứng trơ giữa biển. */
    private static void buildWallFragments(ServerWorld world, Random random) {
        BlockState code = SamsaraBlocks.WOMB_CODE.getDefaultState();
        int[][] walls = {{-24, -30, 9, 7}, {22, -62, 7, 5}, {-17, -96, 11, 8}, {28, -118, 6, 4}};
        List<BlockPos> codes = new ArrayList<>();
        for (int[] w : walls) {
            for (int dx = 0; dx < w[2]; dx++) {
                int top = w[3] - ((dx * 5) & 3);
                for (int y = -5; y <= top; y++) {
                    BlockPos p = at(w[0] + dx, y, w[1]);
                    if (y > 0 && random.nextFloat() < 0.08f) {
                        world.setBlockState(p, WombCodeBlock.at(code, p), FLAGS);
                        codes.add(p);
                    } else {
                        world.setBlockState(p, tile(random), FLAGS);
                    }
                }
            }
        }
        for (BlockPos p : codes) WombCodeBlock.refresh(world, p);
    }

    // ==========================================
    // CĂN PHÒNG NHÁP
    // ==========================================

    /** Đúng căn phòng 2018 nhưng còn đang xây: mặt bắc mở ra bể, tường đứt đoạn, đồ đạc đặt lệch. */
    private static void buildDraftRoom(ServerWorld world) {
        BlockState wall = SamsaraBlocks.NAM_WALLPAPER.getDefaultState();
        BlockState plank = Blocks.OAK_PLANKS.getDefaultState();
        for (int x = -7; x <= 7; x++) {
            for (int z = 1; z <= 15; z++) {
                if (((x * 5 + z * 3) & 7) != 0 || x == -2) world.setBlockState(at(x, 0, z), plank, FLAGS);
            }
        }
        world.setBlockState(arrival().down(), plank, FLAGS);
        for (int x = -7; x <= 7; x++) {
            int top = x < 3 ? 4 : x < 6 ? 2 : 1;
            for (int y = 1; y <= top; y++) world.setBlockState(at(x, y, 15), wall, FLAGS);
        }
        for (int z = 5; z <= 15; z++) {
            int top = z >= 9 ? 4 : z >= 7 ? 2 : 1;
            for (int y = 1; y <= top; y++) world.setBlockState(at(-7, y, z), wall, FLAGS);
        }
        for (int z = 8; z <= 15; z += 2) {
            for (int y = 1; y <= 3; y++) world.setBlockState(at(7, y, z), wall, FLAGS);
        }

        BlockState bed = Blocks.RED_BED.getDefaultState().with(BedBlock.FACING, Direction.NORTH);
        world.setBlockState(at(-4, 1, 13), bed.with(BedBlock.PART, BedPart.HEAD), FLAGS);
        world.setBlockState(at(-4, 1, 14), bed.with(BedBlock.PART, BedPart.FOOT), FLAGS);

        for (int x = 1; x <= 4; x++) {
            for (int z = 4; z <= 6; z++) world.setBlockState(at(x, 1, z), Blocks.BROWN_CARPET.getDefaultState(), FLAGS);
        }
        // Đồ đạc chưa vào chỗ
        world.setBlockState(at(3, 1, 11), Blocks.BOOKSHELF.getDefaultState(), FLAGS);
        world.setBlockState(at(5, 1, 3), Blocks.BARREL.getDefaultState(), FLAGS);
        world.setBlockState(at(-5, 1, 3), Blocks.LECTERN.getDefaultState(), FLAGS);
    }

    // ==========================================
    // ĐOẠN 1
    // ==========================================

    private static void buildPier(ServerWorld world) {
        BlockState plank = Blocks.OAK_PLANKS.getDefaultState();
        for (int z = 0; z >= -6; z--) {
            for (int x = -1; x <= 1; x++) world.setBlockState(at(x, 0, z), plank, FLAGS);
        }
        // Cầu chưa đóng xong: đầu cầu nham nhở
        world.setBlockState(at(-1, 0, -7), plank, FLAGS);
        world.setBlockState(at(0, 0, -7), plank, FLAGS);
        world.setBlockState(at(0, 0, -8), plank, FLAGS);
        standingSign(world, at(1, 1, -5), 0, "samsara.tang_duoi2.khong_xoa", "samsara.tang_duoi2.nhap_thoi",
                "samsara.tang_duoi2.nhung_dung_xoa");
    }

    /** Rộng 3 ô, đi thẳng, rồi dịch sang đông một quãng: phải ngắm máy quay mới thấy chỗ rẽ. */
    private static void buildSectionOne(ServerWorld world) {
        place(world, section(0), SamsaraBlocks.KHOI_NHAP.getDefaultState());
    }

    private static void place(ServerWorld world, List<BlockPos> tiles, BlockState state) {
        for (BlockPos t : tiles) world.setBlockState(t, state, FLAGS);
    }

    // ==========================================
    // ĐOẠN 2-5
    // ==========================================

    private static void island(ServerWorld world, int x0, int x1, int z0, int z1) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                world.setBlockState(at(x, 0, z), Blocks.GRASS_BLOCK.getDefaultState(), FLAGS);
                world.setBlockState(at(x, -1, z), Blocks.DIRT.getDefaultState(), FLAGS);
                world.setBlockState(at(x, -2, z), Blocks.DIRT.getDefaultState(), FLAGS);
                world.setBlockState(at(x, -3, z), Blocks.BEDROCK.getDefaultState(), FLAGS);
            }
        }
    }

    private static void buildLaterSections(ServerWorld world) {
        BlockState draft = SamsaraBlocks.KHOI_NHAP.getDefaultState();
        BlockState plank = Blocks.OAK_PLANKS.getDefaultState();

        // Đoạn 2, rộng 1 ô
        place(world, section(1), draft);
        standingSign(world, at(5, 1, -50), 0, "samsara.tang_duoi2.khong_xoa", "samsara.tang_duoi2.chua_xong");

        // Đảo 2: một hàng ghế quay mặt về phía người tới
        island(world, -6, 8, -105, -91);
        BlockState seat = Blocks.OAK_STAIRS.getDefaultState().with(net.minecraft.block.StairsBlock.FACING, Direction.NORTH);
        for (int i = 0; i < 6; i++) {
            int x = -4 + i * 2;
            world.setBlockState(at(x, 1, -97), seat, FLAGS);
            numberSign(world, at(x, 1, -96), i + 6);
        }

        // Đoạn 3, tự xoá sau lưng
        place(world, section(2), draft);
        standingSign(world, at(3, 1, -104), 0, "samsara.tang_duoi2.khong_xoa_gap", "samsara.tang_duoi2.khong_xoa_gap");
        standingSign(world, at(2, 1, -112), 0, "samsara.tang_duoi2.khong_xoa_gap");
        standingSign(world, at(9, 1, -122), 0, "samsara.tang_duoi2.khong_xoa_gap", null, "samsara.tang_duoi2.khong_xoa_gap");

        // Đảo 3: ba phòng mới dựng được ba bức tường
        island(world, -5, 9, -150, -135);
        BlockState wall = SamsaraBlocks.NAM_WALLPAPER.getDefaultState();
        for (int room = 0; room < 3; room++) {
            int x0 = -4 + room * 5;
            for (int y = 1; y <= 3 - room; y++) {
                for (int dx = 0; dx <= 3; dx++) world.setBlockState(at(x0 + dx, y, -145), wall, FLAGS);
                for (int dz = 1; dz <= 3; dz++) {
                    world.setBlockState(at(x0, y, -145 + dz), wall, FLAGS);
                    world.setBlockState(at(x0 + 3, y, -145 + dz), wall, FLAGS);
                }
            }
        }

        // Đoạn 4: cầu ván thẳng tắp là giả, lối thật ngoằn ngoèo bên cạnh
        place(world, section(3), draft);
        BlockState fake = SamsaraBlocks.VAN_GIA.getDefaultState();
        for (int z = -151; z >= -189; z--) {
            world.setBlockState(at(2, 0, z), fake, FLAGS);
            world.setBlockState(at(3, 0, z), fake, FLAGS);
        }
        standingSign(world, at(4, 1, -149), 0, "samsara.tang_duoi2.cai_nhin_thay", "samsara.tang_duoi2.anh_xoa_roi");

        // Sàn ván thật trước đoạn cuối
        for (int x = -3; x <= 5; x++) {
            for (int z = -194; z <= -190; z++) world.setBlockState(at(x, 0, z), plank, FLAGS);
        }

        // Đoạn 5: máy quay nhiễu, chỉ còn các tấm bảng
        place(world, section(4), draft);
        standingSign(world, at(2, 1, -197), 0, "samsara.tang_duoi2.khong_xoa_thuong");
        standingSign(world, at(-1, 1, -202), 0, "samsara.tang_duoi2.khong");
        standingSign(world, at(-1, 1, -205), 0, "samsara.tang_duoi2.xoa");
        standingSign(world, at(1, 1, -207), 0, "samsara.tang_duoi2.dung");
        standingSign(world, at(3, 1, -211), 0, "samsara.tang_duoi2.xin_em");
    }

    // ==========================================
    // PHÒNG CUỐI
    // ==========================================

    /** Lần xây thứ tư: vẫn dở dang. Mắt thường chỉ thấy một giường; qua máy quay có thêm hai giường nháp hai bên. */
    private static void buildFinalRoom(ServerWorld world, Random random) {
        BlockState plank = Blocks.OAK_PLANKS.getDefaultState();
        BlockState wall = SamsaraBlocks.NAM_WALLPAPER.getDefaultState();
        for (int x = -7; x <= 7; x++) {
            for (int z = -229; z <= -215; z++) {
                world.setBlockState(at(x, 0, z), plank, FLAGS);
                boolean north = z == -229;
                boolean side = Math.abs(x) == 7;
                if (!north && !side) continue;
                int top = north ? 4 : z < -222 ? 4 : z < -218 ? 2 : 0;
                for (int y = 1; y <= top; y++) world.setBlockState(at(x, y, z), wall, FLAGS);
            }
        }
        BlockState bed = Blocks.RED_BED.getDefaultState().with(BedBlock.FACING, Direction.NORTH);
        world.setBlockState(FINAL_BED, bed.with(BedBlock.PART, BedPart.HEAD), FLAGS);
        world.setBlockState(FINAL_BED.south(), bed.with(BedBlock.PART, BedPart.FOOT), FLAGS);
        DuongOngBuilder.placeSign(world, at(0, 3, -228), Direction.SOUTH, null, "samsara.tang_duoi2.dung_bam_ngu");

        BlockState draftBed = SamsaraBlocks.KHOI_NHAP.getDefaultState().with(com.samsara.block.KhoiNhapBlock.WATERLOGGED, false);
        for (int x : new int[]{-3, 3}) {
            world.setBlockState(at(x, 1, -228), draftBed, FLAGS);
            world.setBlockState(at(x, 1, -227), draftBed, FLAGS);
        }

        decorateFinalRoom(world);

        BlockPos lectern = at(5, 1, -219);
        world.setBlockState(lectern, Blocks.LECTERN.getDefaultState().with(net.minecraft.block.LecternBlock.FACING, Direction.SOUTH)
                .with(net.minecraft.block.LecternBlock.HAS_BOOK, true), FLAGS);
        if (world.getBlockEntity(lectern) instanceof net.minecraft.block.entity.LecternBlockEntity be) {
            be.setBook(journal());
            be.markDirty();
        }

        BlockState unloaded = SamsaraBlocks.KHOI_CHUA_TAI.getDefaultState();
        world.setBlockState(at(7, 2, -225), unloaded, FLAGS);
        world.setBlockState(at(7, 3, -225), unloaded, FLAGS);
        world.setBlockState(at(-7, 1, -220), unloaded, FLAGS);
    }

    /** Vẫn là căn phòng ấy, xây lần thứ tư: đồ đã vào gần đúng chỗ, sàn còn lỗ, hai tấm bảng hỏi giường em ở đâu. */
    private static void decorateFinalRoom(ServerWorld world) {
        BlockState tile = SamsaraBlocks.GACH_MEN_O.getDefaultState();
        for (int x = -6; x <= 6; x++) {
            for (int z = -228; z <= -216; z++) {
                if (x >= -4 && x <= 4 && z <= -225) continue;   // quanh giường giữ sàn ván
                if (((x * 5 + z * 3) & 15) == 0) world.setBlockState(at(x, 0, z), tile, FLAGS);
            }
        }
        // Tường bắc chưa xây tới đỉnh
        for (int x = 1; x <= 7; x++) {
            for (int y = 3 + (x > 4 ? 0 : 1); y <= 4; y++) world.setBlockState(at(x, y, -229), Blocks.AIR.getDefaultState(), FLAGS);
        }
        for (int x = -3; x <= 3; x++) {
            for (int z = -224; z <= -221; z++) world.setBlockState(at(x, 1, z), Blocks.BROWN_CARPET.getDefaultState(), FLAGS);
        }
        BlockState shelf = Blocks.BOOKSHELF.getDefaultState();
        for (int z = -226; z <= -223; z++) {
            world.setBlockState(at(-6, 1, z), shelf, FLAGS);
            if (z <= -225) world.setBlockState(at(-6, 2, z), shelf, FLAGS);
        }
        world.setBlockState(at(6, 1, -227), Blocks.BARREL.getDefaultState(), FLAGS);
        world.setBlockState(at(6, 1, -226), Blocks.BARREL.getDefaultState(), FLAGS);
        BlockState seat = Blocks.OAK_STAIRS.getDefaultState().with(net.minecraft.block.StairsBlock.FACING, Direction.WEST);
        world.setBlockState(at(5, 1, -222), seat, FLAGS);
        world.setBlockState(at(-5, 1, -219), seat, FLAGS);
        // Hai tấm bảng chỉ vào chỗ trống hai bên giường; máy quay mới thấy thứ đặt ở đó
        standingSign(world, at(-3, 1, -225), 0, null, "samsara.tang_duoi2.ben_trai");
        standingSign(world, at(3, 1, -225), 0, null, "samsara.tang_duoi2.ben_phai");
    }

    private static net.minecraft.item.ItemStack journal() {
        net.minecraft.item.ItemStack book = new net.minecraft.item.ItemStack(net.minecraft.item.Items.WRITTEN_BOOK);
        net.minecraft.nbt.NbtCompound nbt = book.getOrCreateNbt();
        nbt.putString(net.minecraft.item.WrittenBookItem.TITLE_KEY, "Mảnh Nhật Ký: 08/2016");
        nbt.putString(net.minecraft.item.WrittenBookItem.AUTHOR_KEY, "Nam_1998");
        nbt.putInt(net.minecraft.item.WrittenBookItem.GENERATION_KEY, 3);
        net.minecraft.nbt.NbtList pages = new net.minecraft.nbt.NbtList();
        pages.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(Text.translatable("samsara.tang_duoi2.nhat_ky"))));
        nbt.put(net.minecraft.item.WrittenBookItem.PAGES_KEY, pages);
        return book;
    }

    // ==========================================
    // ĐẢO 1: CÁC HÀNG THỬ NGHIỆM
    // ==========================================

    private static void buildIslandOne(ServerWorld world) {
        // Một lát cắt Superflat nổi giữa bể: cỏ, hai lớp đất, đá gốc, bên dưới là nước
        for (int x = -4; x <= 10; x++) {
            for (int z = -51; z <= -37; z++) {
                world.setBlockState(at(x, 0, z), Blocks.GRASS_BLOCK.getDefaultState(), FLAGS);
                world.setBlockState(at(x, -1, z), Blocks.DIRT.getDefaultState(), FLAGS);
                world.setBlockState(at(x, -2, z), Blocks.DIRT.getDefaultState(), FLAGS);
                world.setBlockState(at(x, -3, z), Blocks.BEDROCK.getDefaultState(), FLAGS);
            }
        }

        // Năm cái giường, mỗi cái một bảng số
        BlockState bed = Blocks.RED_BED.getDefaultState().with(BedBlock.FACING, Direction.NORTH);
        for (int i = 0; i < 5; i++) {
            int x = -2 + i * 3;
            world.setBlockState(at(x, 1, -42), bed.with(BedBlock.PART, BedPart.HEAD), FLAGS);
            world.setBlockState(at(x, 1, -41), bed.with(BedBlock.PART, BedPart.FOOT), FLAGS);
            numberSign(world, at(x, 1, -40), i + 1);
        }

        // Một hàng cửa đứng trơ, không có tường
        BlockState door = Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.SOUTH);
        for (int x = -2; x <= 8; x += 2) {
            world.setBlockState(at(x, 1, -47), door.with(DoorBlock.HALF, DoubleBlockHalf.LOWER), FLAGS);
            world.setBlockState(at(x, 2, -47), door.with(DoorBlock.HALF, DoubleBlockHalf.UPPER), FLAGS);
        }
    }

    // ==========================================
    // BẢNG
    // ==========================================

    private static void numberSign(ServerWorld world, BlockPos pos, int n) {
        world.setBlockState(pos, Blocks.OAK_SIGN.getDefaultState().with(SignBlock.ROTATION, 0), FLAGS);
        if (world.getBlockEntity(pos) instanceof SignBlockEntity sign) {
            sign.setText(new SignText().withMessage(1, Text.translatable("samsara.tang_duoi2.thu", n)), true);
            sign.markDirty();
        }
    }

    private static void standingSign(ServerWorld world, BlockPos pos, int rotation, String... keys) {
        world.setBlockState(pos, Blocks.OAK_SIGN.getDefaultState().with(SignBlock.ROTATION, rotation), FLAGS);
        if (world.getBlockEntity(pos) instanceof SignBlockEntity sign) {
            SignText text = new SignText();
            for (int i = 0; i < keys.length && i < 4; i++) {
                if (keys[i] != null) text = text.withMessage(i, Text.translatable(keys[i]));
            }
            sign.setText(text, true);
            sign.markDirty();
        }
    }
}
