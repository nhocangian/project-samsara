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
import net.minecraft.util.math.Box;
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

    public static final int SECTIONS = 7;
    public static final int ERASE_SECTION = 2;
    /** Đoạn 5: ô chỉ có thật khi đang được nhìn qua máy quay. */
    public static final int LOOK_SECTION = 4;
    /** Đoạn 6: lối đi đổi qua lại giữa hai bản nháp theo nhịp tự lưu. */
    public static final int SAVE_SECTION = 5;
    /** Phần sau đảo 5 (sàn ván, đoạn nhiễu, phòng cuối) lùi về bắc chừng này block so với bản 5 đoạn cũ. */
    private static final int D = -93;
    public static final int ERASE_START_Z = -105;
    /** Ra khỏi đảo 4 (z nhỏ hơn) thì luật nhìn của đoạn 5 bắt đầu. */
    public static final int LOOK_START_Z = -204;
    /** Ophanim chỉ bắt đầu ló ra khi người chơi đã lên tới đảo 2 (đã quen vừa ngắm máy quay vừa đi). */
    public static final int ANGEL_FROM_Z = -91;
    /** Tới đảo 3 / đảo 4 / đảo 5 thì Ophanim được phép tiến lại gần thêm một bậc. */
    public static final int[] ANGEL_STAGE_Z = {-91, -135, -190, -235};
    private static final int STATIC_FROM_Z = -194 + D;
    private static final int FINAL_ROOM_Z = -215 + D;
    private static final BlockPos FINAL_BED = at(0, 1, -228 + D);

    /** Điểm nổi lên của từng đoạn: cuối cầu, rồi mép bắc mỗi đảo. */
    private static final int[][] CHECKPOINTS = {{0, -7}, {3, -50}, {1, -104}, {2, -149}, {2, -203}, {3, -248}, {1, -285}};
    /** Đã vượt qua z này (đi về bắc) thì đoạn tương ứng trở thành điểm nổi. */
    private static final int[] CHECKPOINT_Z = {100, -37, -91, -135, -190, -235, -283};

    private static final int[][] LINE_TWO = {{3, -52}, {3, -60}, {9, -60}, {9, -70}, {-3, -70}, {-3, -82}, {1, -82}, {1, -90}};
    private static final int[][] LINE_THREE = {{1, -106}, {1, -116}, {8, -116}, {8, -128}, {2, -128}, {2, -134}};
    private static final int[][] LINE_FOUR = {{0, -151}, {0, -160}, {-3, -160}, {-3, -172}, {0, -172}, {0, -189}};
    private static final int[][] LINE_LOOK = {{2, -205}, {2, -212}, {6, -212}, {6, -220}, {0, -220}, {0, -228}, {3, -228}, {3, -234}};
    /** Đoạn 6: ba chặng giữa bốn ô ngã ba (x 3); chặng 1 và 3 chỉ có ở bản nháp 0 (vòng tây), chặng 2 chỉ ở bản 1 (vòng đông). */
    private static final int[] SAVE_JUNCTIONS_Z = {-250, -261, -272, -283};
    private static final int[][] LINE_FIVE = {{1, -195 + D}, {1, -201 + D}, {-2, -201 + D}, {-2, -208 + D}, {2, -208 + D}, {2, -214 + D}};
    private static final List<List<BlockPos>> SAVE_VERSIONS = new ArrayList<>();

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
        SECTION_TILES.add(tiles(LINE_LOOK));

        List<BlockPos> v0 = new ArrayList<>();
        List<BlockPos> v1 = new ArrayList<>();
        List<BlockPos> all = new ArrayList<>();
        for (int leg = 0; leg < 3; leg++) {
            int z0 = SAVE_JUNCTIONS_Z[leg];
            int z1 = SAVE_JUNCTIONS_Z[leg + 1];
            int x = leg % 2 == 0 ? -1 : 7;
            List<BlockPos> path = tiles(new int[][]{{3, z0}, {3, z0 - 1}, {x, z0 - 1}, {x, z1 + 1}, {3, z1 + 1}, {3, z1}});
            if (leg == 0) all.add(path.get(0));
            for (int i = 1; i < path.size() - 1; i++) {
                (leg % 2 == 0 ? v0 : v1).add(path.get(i));
                all.add(path.get(i));
            }
            if (leg < 2) all.add(path.get(path.size() - 1));    // ô ngã ba cuối là sàn ván
        }
        for (int i = 0; i < 3; i++) {
            v0.add(at(3, 0, SAVE_JUNCTIONS_Z[i]));
            v1.add(at(3, 0, SAVE_JUNCTIONS_Z[i]));
        }
        SAVE_VERSIONS.add(v0);
        SAVE_VERSIONS.add(v1);
        SECTION_TILES.add(all);
        SECTION_TILES.add(tiles(LINE_FIVE));
    }

    /** Các ô của đoạn 6 có mặt ở bản nháp {@code version} (0 hoặc 1), kể cả các ô ngã ba. */
    public static List<BlockPos> saveVersion(int version) {
        return SAVE_VERSIONS.get(version);
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

    /** Đã bước vào đoạn nhiễu hay xa hơn (phòng cuối). Nhận z tuyệt đối. */
    public static boolean isPastStatic(double worldZ) {
        return worldZ - ORIGIN.getZ() < STATIC_FROM_Z;
    }

    /** Đã vào phòng cuối. Nhận z tuyệt đối. */
    public static boolean isFinalRoom(double worldZ) {
        return worldZ - ORIGIN.getZ() < FINAL_ROOM_Z;
    }

    public static boolean isFinalBed(BlockPos pos) {
        return pos.equals(FINAL_BED) || pos.equals(FINAL_BED.south());
    }

    // ==========================================
    // TRANG XÉ VÀ GIƯỜNG CỦA EM
    // ==========================================

    public static final String PAGE_TAG = "samsara_trang";
    public static final String BED_ITEM_KEY = "samsara_giuong_em";
    /** Bốn trang xé trên đảo 2-5 (x, z tương đối), lệch khỏi lối đi; mắt thường không thấy, máy quay mới thấy. */
    private static final int[][] PAGE_SPOTS = {{-5, -102}, {-3, -143}, {6, -198}, {8, -238}};
    /** Giường em ở bên có nắng sớm: phía đông, bên phải khi nhìn về bắc. */
    public static final int RIGHT_SIDE = 1;

    private static void spawnPages(ServerWorld world) {
        for (int i = 0; i < PAGE_SPOTS.length; i++) {
            BlockPos p = at(PAGE_SPOTS[i][0], 1, PAGE_SPOTS[i][1]);
            Box around = new Box(p).expand(2.0);
            world.getEntitiesByClass(net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity.class, around,
                    e -> e.getCommandTags().contains(PAGE_TAG)).forEach(e -> e.discard());
            net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity e = net.minecraft.entity.EntityType.ITEM_DISPLAY.create(world);
            if (e == null) continue;
            e.refreshPositionAndAngles(p.getX() + 0.5, p.getY() + 0.08, p.getZ() + 0.5, 25.0f + i * 70.0f, 0.0f);
            net.minecraft.nbt.NbtCompound nbt = e.writeNbt(new net.minecraft.nbt.NbtCompound());
            nbt.put("item", new net.minecraft.item.ItemStack(net.minecraft.item.Items.PAPER).writeNbt(new net.minecraft.nbt.NbtCompound()));
            nbt.putString("item_display", "fixed");
            net.minecraft.nbt.NbtCompound t = new net.minecraft.nbt.NbtCompound();
            t.put("left_rotation", floats(-0.64278764f, 0f, 0f, 0.76604444f));   // gần như nằm phẳng, hơi vênh một mép
            t.put("right_rotation", floats(0f, 0f, 0f, 1f));
            t.put("translation", floats(0f, 0f, 0f));
            t.put("scale", floats(1.3f, 1.3f, 1.3f));
            nbt.put("transformation", t);
            e.readNbt(nbt);
            e.addCommandTag(PAGE_TAG);
            e.addCommandTag(PAGE_TAG + "_" + (i + 1));
            world.spawnEntity(e);
        }
    }

    private static net.minecraft.nbt.NbtList floats(float... v) {
        net.minecraft.nbt.NbtList l = new net.minecraft.nbt.NbtList();
        for (float f : v) l.add(net.minecraft.nbt.NbtFloat.of(f));
        return l;
    }

    /** Trang xé lần {@code n}: một bản nháp khác của cùng một đoạn nhật ký. */
    public static net.minecraft.item.ItemStack pageBook(int n) {
        net.minecraft.item.ItemStack book = new net.minecraft.item.ItemStack(net.minecraft.item.Items.WRITTEN_BOOK);
        net.minecraft.nbt.NbtCompound nbt = book.getOrCreateNbt();
        nbt.putString(net.minecraft.item.WrittenBookItem.TITLE_KEY, "Trang xé (lần " + n + ")");
        nbt.putString(net.minecraft.item.WrittenBookItem.AUTHOR_KEY, "Nam_1998");
        nbt.putInt(net.minecraft.item.WrittenBookItem.GENERATION_KEY, 3);
        net.minecraft.nbt.NbtList pages = new net.minecraft.nbt.NbtList();
        pages.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(Text.translatable("samsara.tang_duoi2.trang_" + n))));
        nbt.put(net.minecraft.item.WrittenBookItem.PAGES_KEY, pages);
        return book;
    }

    /** Chiếc giường người chơi được cầm ở phòng cuối, chỉ đặt được vào một trong hai chỗ trống. */
    public static net.minecraft.item.ItemStack bedItem() {
        net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(net.minecraft.item.Items.RED_BED);
        stack.setCustomName(Text.translatable("samsara.tang_duoi2.giuong_em_item").formatted(net.minecraft.util.Formatting.WHITE));
        stack.getOrCreateNbt().putBoolean(BED_ITEM_KEY, true);
        return stack;
    }

    public static boolean isBedItem(net.minecraft.item.ItemStack stack) {
        return stack.isOf(net.minecraft.item.Items.RED_BED) && stack.hasNbt() && stack.getNbt().getBoolean(BED_ITEM_KEY);
    }

    /** Chỗ người chơi bấm vào thuộc chỗ trống bên trái (-1), bên phải (1) hay không phải (0). */
    public static int bedSideAt(BlockPos pos) {
        int x = pos.getX() - ORIGIN.getX();
        int y = pos.getY() - ORIGIN.getY();
        int z = pos.getZ() - ORIGIN.getZ() - D;
        if (y < 0 || y > 1 || z < -229 || z > -226) return 0;
        if (x >= 2 && x <= 4) return 1;
        if (x >= -4 && x <= -2) return -1;
        return 0;
    }

    /** Đặt sai bên thì giường nhạt lại thành khung dây; khối đánh dấu dưới sàn nhớ là đã đặt rồi. */
    private static BlockPos wrongMarker() {
        return room(-3 * RIGHT_SIDE, -1, -228);
    }

    /** Bên nào người chơi đã đặt giường em; 0 nếu chưa đặt. */
    public static int chosenSide(ServerWorld world) {
        if (world.getBlockState(room(3 * RIGHT_SIDE, 1, -228)).isOf(Blocks.RED_BED)) return RIGHT_SIDE;
        if (world.getBlockState(wrongMarker()).isOf(Blocks.BLACK_WOOL)) return -RIGHT_SIDE;
        return 0;
    }

    /** Giường đặt sai bên từ từ nhạt lại thành khung dây, mảnh nhật ký thêm một dòng. */
    public static void fadeWrongBed(ServerWorld world) {
        int side = -RIGHT_SIDE;
        BlockState draftBed = SamsaraBlocks.KHOI_NHAP.getDefaultState().with(com.samsara.block.KhoiNhapBlock.WATERLOGGED, false);
        for (int z : new int[]{-228, -227}) {
            BlockPos p = room(3 * side, 1, z);
            if (!world.getBlockState(p).isOf(Blocks.RED_BED)) continue;
            world.setBlockState(p, draftBed, FLAGS);
            world.spawnParticles(net.minecraft.particle.ParticleTypes.WHITE_ASH, p.getX() + 0.5, p.getY() + 0.4, p.getZ() + 0.5, 30, 0.4, 0.3, 0.4, 0.01);
        }
        world.playSound(null, room(3 * side, 1, -228), net.minecraft.sound.SoundEvents.BLOCK_BEACON_DEACTIVATE, net.minecraft.sound.SoundCategory.BLOCKS, 1.0f, 0.7f);
        if (world.getBlockEntity(room(5, 1, -219)) instanceof net.minecraft.block.entity.LecternBlockEntity be) {
            be.setBook(journal(-1));
            be.markDirty();
        }
    }

    public static boolean isChosenBed(BlockPos pos, int side) {
        return side != 0 && (pos.equals(room(3 * side, 1, -228)) || pos.equals(room(3 * side, 1, -227)));
    }

    public static BlockPos bedItemSpot() {
        return room(0, 1, -221);
    }

    public static Box finalRoomBox() {
        return new Box(room(-7, 0, -229), room(8, 6, -214));
    }

    /**
     * Đặt giường vào bên {@code side}. Đúng bên (có nắng sớm) thì giường nháp bên kia tan đi và mảnh nhật ký có thêm một
     * trang; sai bên thì giường vẫn nằm đó, không nói gì.
     */
    public static void placeChosenBed(ServerWorld world, int side) {
        if (side != RIGHT_SIDE) world.setBlockState(wrongMarker(), Blocks.BLACK_WOOL.getDefaultState(), FLAGS);
        BlockState bed = Blocks.RED_BED.getDefaultState().with(BedBlock.FACING, Direction.NORTH);
        world.setBlockState(room(3 * side, 1, -228), bed.with(BedBlock.PART, BedPart.HEAD), FLAGS);
        world.setBlockState(room(3 * side, 1, -227), bed.with(BedBlock.PART, BedPart.FOOT), FLAGS);
        world.playSound(null, room(3 * side, 1, -228), net.minecraft.sound.SoundEvents.BLOCK_WOOL_PLACE, net.minecraft.sound.SoundCategory.BLOCKS, 1.0f, 0.8f);
        if (side != RIGHT_SIDE) return;
        for (int z : new int[]{-228, -227}) {
            BlockPos p = room(-3 * side, 1, z);
            world.setBlockState(p, Blocks.AIR.getDefaultState(), FLAGS);
            world.spawnParticles(net.minecraft.particle.ParticleTypes.WHITE_ASH, p.getX() + 0.5, p.getY() + 0.4, p.getZ() + 0.5, 30, 0.4, 0.3, 0.4, 0.01);
        }
        world.playSound(null, room(0, 2, -224), net.minecraft.sound.SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, net.minecraft.sound.SoundCategory.BLOCKS, 1.5f, 0.6f);
        if (world.getBlockEntity(room(5, 1, -219)) instanceof net.minecraft.block.entity.LecternBlockEntity be) {
            be.setBook(journal(1));
            be.markDirty();
        }
    }

    public static BlockPos finalRoom() {
        return at(0, 1, -218 + D);
    }

    /** Toạ độ phòng cuối và đoạn nhiễu viết theo bản cũ, lùi về bắc D block. */
    private static BlockPos room(int x, int y, int z) {
        return at(x, y, z + D);
    }

    public static void restoreErasing(ServerWorld world) {
        restoreSection(world, ERASE_SECTION);
    }

    public static void restoreSection(ServerWorld world, int index) {
        BlockState draft = SamsaraBlocks.KHOI_NHAP.getDefaultState();
        for (BlockPos t : section(index)) {
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
        clearOldEnd(world);
        buildBrokenPipe(world, random);
        buildFloatingLamps(world);
        buildWallFragments(world, random);
        buildFragmentCeilings(world, random);
        buildDraftRoom(world);
        buildPier(world);
        buildSectionOne(world);
        buildIslandOne(world);
        buildLaterSections(world);
        buildFinalRoom(world, random);
        spawnPages(world);
    }

    /** Bản 5 đoạn cũ đặt sàn ván và phòng cuối ở z -190..-229: trả chỗ đó về biển trước khi dựng lại. */
    private static void clearOldEnd(ServerWorld world) {
        BlockState water = Blocks.WATER.getDefaultState();
        BlockState air = Blocks.AIR.getDefaultState();
        for (int x = -12; x <= 12; x++) {
            for (int z = -186; z >= -240; z--) {
                for (int y = -3; y <= 8; y++) world.setBlockState(at(x, y, z), y <= 0 ? water : air, FLAGS);
            }
        }
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

    /**
     * Đèn không còn treo thành hàng giữa trời (trông như lỗi): mỗi mảng tường giữ lại một mẩu trần ống chìa ra,
     * đèn huỳnh quang gắn dưới mẩu trần đó. Xoá hai hàng đèn của bản cũ nếu world đã có.
     */
    private static void buildFloatingLamps(ServerWorld world) {
        BlockState air = Blocks.AIR.getDefaultState();
        for (int z = -18; z >= -140; z -= 13) {
            world.setBlockState(at(-8, 7, z), air, FLAGS);
            world.setBlockState(at(13, 7, z - 5), air, FLAGS);
        }
    }

    /** Mẩu trần chìa ra từ mặt nam của mảng tường, đèn gắn ở giữa. */
    private static void buildFragmentCeilings(ServerWorld world, Random random) {
        BlockState lamp = SamsaraBlocks.DEN_HUYNH_QUANG.getDefaultState().with(DenOngBlock.AXIS, Direction.Axis.X);
        for (int[] w : WALLS) {
            if (w[3] < 5) continue;
            int y = w[3] - 2;
            int reach = 2 + (w[2] % 2);
            for (int dx = 1; dx < w[2] - 1; dx++) {
                int depth = reach - ((dx * 3) & 1);    // mép trần nham nhở
                for (int dz = 1; dz <= depth; dz++) world.setBlockState(at(w[0] + dx, y, w[1] + dz), tile(random), FLAGS);
            }
            world.setBlockState(at(w[0] + w[2] / 2, y, w[1] + 1), lamp, FLAGS);
        }
    }

    /** Mảng tường: x, z (mặt tường), dài, cao. */
    private static final int[][] WALLS = {{-24, -30, 9, 7}, {22, -62, 7, 5}, {-17, -96, 11, 8}, {28, -118, 6, 4},
            {-22, -226, 8, 6}, {20, -266, 7, 5}};

    /** Vài mảng tường gạch đứng trơ giữa biển. */
    private static void buildWallFragments(ServerWorld world, Random random) {
        BlockState code = SamsaraBlocks.WOMB_CODE.getDefaultState();
        List<BlockPos> codes = new ArrayList<>();
        for (int[] w : WALLS) {
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

        // Đảo 4: một chân máy quay đứng trơ, chĩa xuống chân
        island(world, -5, 9, -204, -190);
        world.setBlockState(at(5, 1, -199), Blocks.OAK_FENCE.getDefaultState(), FLAGS);
        world.setBlockState(at(5, 2, -199), Blocks.OAK_FENCE.getDefaultState(), FLAGS);
        world.setBlockState(at(5, 3, -199), Blocks.DISPENSER.getDefaultState()
                .with(net.minecraft.block.DispenserBlock.FACING, Direction.DOWN), FLAGS);
        standingSign(world, at(4, 1, -203), 0, "samsara.tang_duoi2.nhin_cho", "samsara.tang_duoi2.em_buoc",
                "samsara.tang_duoi2.dung_nhin", "samsara.tang_duoi2.cho_khac");

        // Đoạn 5: ô chỉ có thật khi đang được nhìn qua máy quay
        place(world, section(LOOK_SECTION), draft);

        // Đảo 5: hai bản của cùng một căn phòng đặt cạnh nhau, lệch nhau một chút
        island(world, -5, 9, -249, -235);
        BlockState bed = Blocks.RED_BED.getDefaultState().with(BedBlock.FACING, Direction.NORTH);
        for (int v = 0; v < 2; v++) {
            int x0 = -3 + v * 7;
            for (int dx = 0; dx <= 3; dx++) {
                for (int y = 1; y <= 2 + v; y++) world.setBlockState(at(x0 + dx, y, -244 - v), wall, FLAGS);
            }
            world.setBlockState(at(x0 + 1 + v, 1, -243 - v), bed.with(BedBlock.PART, BedPart.HEAD), FLAGS);
            world.setBlockState(at(x0 + 1 + v, 1, -242 - v), bed.with(BedBlock.PART, BedPart.FOOT), FLAGS);
            versionSign(world, at(x0 + 1 + v, 1, -240 - v), v + 1);
        }
        standingSign(world, at(5, 1, -248), 0, "samsara.tang_duoi2.tu_luu", "samsara.tang_duoi2.moi_luc",
                "samsara.tang_duoi2.mot_ban", "samsara.tang_duoi2.cho_no_luu");

        // Đoạn 6: bắt đầu ở bản nháp 0
        place(world, section(SAVE_SECTION), Blocks.WATER.getDefaultState());
        place(world, saveVersion(0), draft);

        // Sàn ván thật trước đoạn cuối
        for (int x = -3; x <= 5; x++) {
            for (int z = -287; z <= -283; z++) world.setBlockState(at(x, 0, z), plank, FLAGS);
        }

        // Đoạn 7: máy quay nhiễu, chỉ còn các tấm bảng
        place(world, section(6), draft);
        standingSign(world, room(2, 1, -197), 0, "samsara.tang_duoi2.khong_xoa_thuong");
        standingSign(world, room(-1, 1, -202), 0, "samsara.tang_duoi2.khong");
        standingSign(world, room(-1, 1, -205), 0, "samsara.tang_duoi2.xoa");
        standingSign(world, room(1, 1, -207), 0, "samsara.tang_duoi2.dung");
        standingSign(world, room(3, 1, -211), 0, "samsara.tang_duoi2.xin_em");
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
                world.setBlockState(room(x, 0, z), plank, FLAGS);
                boolean north = z == -229;
                boolean side = Math.abs(x) == 7;
                if (!north && !side) continue;
                int top = north ? 4 : z < -222 ? 4 : z < -218 ? 2 : 0;
                for (int y = 1; y <= top; y++) world.setBlockState(room(x, y, z), wall, FLAGS);
            }
        }
        BlockState bed = Blocks.RED_BED.getDefaultState().with(BedBlock.FACING, Direction.NORTH);
        world.setBlockState(FINAL_BED, bed.with(BedBlock.PART, BedPart.HEAD), FLAGS);
        world.setBlockState(FINAL_BED.south(), bed.with(BedBlock.PART, BedPart.FOOT), FLAGS);
        DuongOngBuilder.placeSign(world, room(0, 3, -228), Direction.SOUTH, null, "samsara.tang_duoi2.dung_bam_ngu");

        BlockState draftBed = SamsaraBlocks.KHOI_NHAP.getDefaultState().with(com.samsara.block.KhoiNhapBlock.WATERLOGGED, false);
        world.setBlockState(wrongMarker(), Blocks.WATER.getDefaultState(), FLAGS);
        for (int x : new int[]{-3, 3}) {
            world.setBlockState(room(x, 1, -228), draftBed, FLAGS);
            world.setBlockState(room(x, 1, -227), draftBed, FLAGS);
        }

        decorateFinalRoom(world);

        // Cửa sổ phía đông, sát chỗ trống bên phải: nắng sớm chiếu vào; chậu hoa trên thùng
        BlockState pane = Blocks.GLASS_PANE.getDefaultState()
                .with(net.minecraft.block.PaneBlock.NORTH, true).with(net.minecraft.block.PaneBlock.SOUTH, true);
        for (int z = -228; z <= -227; z++) {
            for (int y = 2; y <= 3; y++) world.setBlockState(room(7, y, z), pane, FLAGS);
        }
        world.setBlockState(room(6, 2, -227), Blocks.POTTED_DANDELION.getDefaultState(), FLAGS);

        BlockPos lectern = room(5, 1, -219);
        world.setBlockState(lectern, Blocks.LECTERN.getDefaultState().with(net.minecraft.block.LecternBlock.FACING, Direction.SOUTH)
                .with(net.minecraft.block.LecternBlock.HAS_BOOK, true), FLAGS);
        if (world.getBlockEntity(lectern) instanceof net.minecraft.block.entity.LecternBlockEntity be) {
            be.setBook(journal(0));
            be.markDirty();
        }

        BlockState unloaded = SamsaraBlocks.KHOI_CHUA_TAI.getDefaultState();
        world.setBlockState(room(7, 2, -225), unloaded, FLAGS);
        world.setBlockState(room(7, 3, -225), unloaded, FLAGS);
        world.setBlockState(room(-7, 1, -220), unloaded, FLAGS);
    }

    /** Vẫn là căn phòng ấy, xây lần thứ tư: đồ đã vào gần đúng chỗ, sàn còn lỗ, hai tấm bảng hỏi giường em ở đâu. */
    private static void decorateFinalRoom(ServerWorld world) {
        BlockState tile = SamsaraBlocks.GACH_MEN_O.getDefaultState();
        for (int x = -6; x <= 6; x++) {
            for (int z = -228; z <= -216; z++) {
                if (x >= -4 && x <= 4 && z <= -225) continue;   // quanh giường giữ sàn ván
                if (((x * 5 + z * 3) & 15) == 0) world.setBlockState(room(x, 0, z), tile, FLAGS);
            }
        }
        // Tường bắc chưa xây tới đỉnh
        for (int x = 1; x <= 7; x++) {
            for (int y = 3 + (x > 4 ? 0 : 1); y <= 4; y++) world.setBlockState(room(x, y, -229), Blocks.AIR.getDefaultState(), FLAGS);
        }
        for (int x = -3; x <= 3; x++) {
            for (int z = -224; z <= -221; z++) world.setBlockState(room(x, 1, z), Blocks.BROWN_CARPET.getDefaultState(), FLAGS);
        }
        BlockState shelf = Blocks.BOOKSHELF.getDefaultState();
        for (int z = -226; z <= -223; z++) {
            world.setBlockState(room(-6, 1, z), shelf, FLAGS);
            if (z <= -225) world.setBlockState(room(-6, 2, z), shelf, FLAGS);
        }
        world.setBlockState(room(6, 1, -227), Blocks.BARREL.getDefaultState(), FLAGS);
        world.setBlockState(room(6, 1, -226), Blocks.BARREL.getDefaultState(), FLAGS);
        BlockState seat = Blocks.OAK_STAIRS.getDefaultState().with(net.minecraft.block.StairsBlock.FACING, Direction.WEST);
        world.setBlockState(room(5, 1, -222), seat, FLAGS);
        world.setBlockState(room(-5, 1, -219), seat, FLAGS);
        // Hai tấm bảng chỉ vào chỗ trống hai bên giường; máy quay mới thấy thứ đặt ở đó
        standingSign(world, room(-3, 1, -225), 0, null, "samsara.tang_duoi2.ben_trai");
        standingSign(world, room(3, 1, -225), 0, null, "samsara.tang_duoi2.ben_phai");
    }

    /** Mảnh nhật ký; {@code choice} 1 = đã đặt đúng bên, -1 = đặt sai, 0 = chưa đặt. */
    private static net.minecraft.item.ItemStack journal(int choice) {
        net.minecraft.item.ItemStack book = new net.minecraft.item.ItemStack(net.minecraft.item.Items.WRITTEN_BOOK);
        net.minecraft.nbt.NbtCompound nbt = book.getOrCreateNbt();
        nbt.putString(net.minecraft.item.WrittenBookItem.TITLE_KEY, "Mảnh Nhật Ký: 08/2016");
        nbt.putString(net.minecraft.item.WrittenBookItem.AUTHOR_KEY, "Nam_1998");
        nbt.putInt(net.minecraft.item.WrittenBookItem.GENERATION_KEY, 3);
        net.minecraft.nbt.NbtList pages = new net.minecraft.nbt.NbtList();
        pages.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(Text.translatable("samsara.tang_duoi2.nhat_ky"))));
        if (choice < 0) pages.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(Text.translatable("samsara.tang_duoi2.nhat_ky_sai"))));
        if (choice > 0) pages.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(Text.translatable("samsara.tang_duoi2.nhat_ky_nho"))));
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

    private static void versionSign(ServerWorld world, BlockPos pos, int n) {
        world.setBlockState(pos, Blocks.OAK_SIGN.getDefaultState().with(SignBlock.ROTATION, 0), FLAGS);
        if (world.getBlockEntity(pos) instanceof SignBlockEntity sign) {
            sign.setText(new SignText().withMessage(1, Text.translatable("samsara.tang_duoi2.ban", n)), true);
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
