package com.samsara.system.trontim;

import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.SignBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.WrittenBookItem;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * Tầng 3 "Trốn tìm": căn nhà đứa em tự xây năm 2012, to gấp đôi. Toạ độ tương đối so với ORIGIN, người chơi đi về bắc (z âm).
 *
 * Sân trước (z 0..16, chỗ tỉnh dậy, xích đu) → nhà x -16..16, z -1..-40, hai tầng (trần tầng trệt y 10, mái y 20).
 * Tầng trệt: phòng khách (tây trước), bếp (đông trước), hành lang có cầu thang (đông sau), phòng ngủ khoá (tây sau).
 * Tầng gác: một phòng chơi lớn, tủ quần áo ở góc tây bắc.
 */
public final class TronTimBuilder {
    private TronTimBuilder() {}

    public static final BlockPos ORIGIN = new BlockPos(0, 64, 0);
    private static final int FLAGS = 2 | 16;

    public static BlockPos at(int x, int y, int z) {
        return ORIGIN.add(x, y, z);
    }

    // Mốc âm thanh của căn nhà
    public static final BlockPos SWING = at(10, 3, 8);
    public static final BlockPos CLOCK = at(-8, 4, -12);
    public static final BlockPos DRIP = at(15, 3, -6);
    public static final BlockPos MUSIC = at(-9, 11, -37);
    public static final BlockPos BEDROOM_DOOR = at(-6, 0, -23);

    // Chỗ trốn
    public static final BlockPos SPOT_TABLE = at(-8, 0, -12);
    public static final BlockPos SPOT_KITCHEN_A = at(11, 0, -20);
    public static final BlockPos SPOT_KITCHEN_B = at(5, 0, -3);
    public static final BlockPos SPOT_WARDROBE = at(-13, 11, -37);
    /** Vòng 2: chỗ của "em" thứ hai, góc phòng khách sát tủ sách, quay mặt vào tường. */
    public static final BlockPos SPOT_DECOY = at(-14, 0, -16);

    public static BlockPos arrival() {
        return at(0, 0, 12);
    }

    public static BlockPos bedroom() {
        return at(-7, 0, -30);
    }

    public static BlockPos upstairs() {
        return at(0, 11, -20);
    }

    /** Vùng phòng ngủ (để biết giường nào là giường cuối tầng). */
    public static boolean inBedroom(BlockPos pos) {
        int x = pos.getX() - ORIGIN.getX(), z = pos.getZ() - ORIGIN.getZ();
        return x >= -15 && x <= 2 && z <= -24 && z >= -39;
    }

    public static Box houseBox() {
        return new Box(at(-20, -1, -45), at(20, 24, 18));
    }

    public static boolean isBuilt(ServerWorld world) {
        return world.getBlockState(CLOCK).isOf(Blocks.NOTE_BLOCK);
    }

    public static void build(ServerWorld world) {
        Random random = Random.create(0x2012L);
        TronTimFinale.clear(world);
        clear(world);
        yard(world);
        shell(world, random);
        livingRoom(world);
        kitchen(world);
        hallway(world);
        bedroomRoom(world);
        upstairsRoom(world, random);
        lockBedroom(world);
    }

    private static void clear(ServerWorld world) {
        BlockState air = Blocks.AIR.getDefaultState();
        BlockState stone = Blocks.SMOOTH_STONE.getDefaultState();
        // Chiều này là hư không: tự lát một tấm nền đá dưới nhà và sân (cảnh cuối sẽ gỡ cả tấm này đi)
        for (int x = -26; x <= 26; x++) {
            for (int z = -50; z <= 24; z++) {
                for (int y = -6; y <= -2; y++) world.setBlockState(at(x, y, z), air, FLAGS);
                world.setBlockState(at(x, -1, z), stone, FLAGS);
                for (int y = 0; y <= 24; y++) world.setBlockState(at(x, y, z), air, FLAGS);
            }
        }
    }

    // ==========================================
    // SÂN TRƯỚC
    // ==========================================

    private static void yard(ServerWorld world) {
        BlockState grass = Blocks.GRASS_BLOCK.getDefaultState();
        BlockState fence = Blocks.OAK_FENCE.getDefaultState();
        for (int x = -16; x <= 16; x++) {
            for (int z = 0; z <= 16; z++) world.setBlockState(at(x, -1, z), grass, FLAGS);
        }
        // Hàng rào cao gấp đôi
        for (int x = -16; x <= 16; x++) {
            for (int y = 0; y <= 1; y++) world.setBlockState(at(x, y, 16), fence, FLAGS);
        }
        for (int z = 0; z <= 16; z++) {
            for (int y = 0; y <= 1; y++) {
                world.setBlockState(at(-16, y, z), fence, FLAGS);
                world.setBlockState(at(16, y, z), fence, FLAGS);
            }
        }
        // Xích đu
        for (int y = 0; y <= 5; y++) {
            world.setBlockState(at(8, y, 8), fence, FLAGS);
            world.setBlockState(at(12, y, 8), fence, FLAGS);
        }
        for (int x = 8; x <= 12; x++) world.setBlockState(at(x, 6, 8), Blocks.OAK_PLANKS.getDefaultState(), FLAGS);
        for (int y = 2; y <= 5; y++) {
            world.setBlockState(at(9, y, 8), Blocks.CHAIN.getDefaultState(), FLAGS);
            world.setBlockState(at(11, y, 8), Blocks.CHAIN.getDefaultState(), FLAGS);
        }
        for (int x = 9; x <= 11; x++) world.setBlockState(at(x, 1, 8), Blocks.OAK_SLAB.getDefaultState(), FLAGS);
        sign(world, at(-3, 0, 6), 0, "samsara.tron_tim.bang.nha_cua_em");
    }

    // ==========================================
    // VỎ NHÀ
    // ==========================================

    private static final BlockState[] PLANKS = {
            Blocks.OAK_PLANKS.getDefaultState(), Blocks.BIRCH_PLANKS.getDefaultState(),
            Blocks.SPRUCE_PLANKS.getDefaultState(), Blocks.JUNGLE_PLANKS.getDefaultState()};

    /** Tường ghép đủ loại gỗ, mỗi cột một loại: kiểu đứa trẻ hết gỗ này lấy gỗ khác. */
    private static BlockState plank(int x, int z) {
        return PLANKS[Math.floorMod(x * 7 + z * 13, PLANKS.length)];
    }

    private static void shell(ServerWorld world, Random random) {
        BlockState floor = Blocks.SPRUCE_PLANKS.getDefaultState();
        BlockState ceiling = Blocks.OAK_PLANKS.getDefaultState();
        BlockState air = Blocks.AIR.getDefaultState();
        for (int x = -16; x <= 16; x++) {
            for (int z = -1; z >= -40; z--) {
                world.setBlockState(at(x, -1, z), floor, FLAGS);
                world.setBlockState(at(x, 10, z), ceiling, FLAGS);
                world.setBlockState(at(x, 20, z), Blocks.DARK_OAK_PLANKS.getDefaultState(), FLAGS);
                boolean outer = Math.abs(x) == 16 || z == -1 || z == -40;
                if (!outer) continue;
                BlockState wall = plank(x, z);
                for (int y = 0; y <= 19; y++) {
                    if (y == 10) continue;
                    world.setBlockState(at(x, y, z), wall, FLAGS);
                }
            }
        }
        // Mái lệch: một bên cao hơn một bên
        for (int x = -16; x <= 16; x++) {
            for (int z = -1; z >= -40; z--) {
                if (x < 0 && (z & 3) != 0) world.setBlockState(at(x, 21, z), Blocks.DARK_OAK_PLANKS.getDefaultState(), FLAGS);
            }
        }
        // Cửa chính to gấp đôi
        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 3; y++) world.setBlockState(at(x, y, -1), air, FLAGS);
        }
        // Cửa sổ chỗ to chỗ nhỏ
        BlockState pane = Blocks.GLASS_PANE.getDefaultState();
        int[][] windows = {{-10, 3, -1, 3, 2}, {8, 4, -1, 2, 3}, {-16, 3, -10, 4, 2}, {16, 2, -12, 2, 2},
                {-6, 13, -1, 4, 3}, {7, 14, -1, 2, 2}, {-16, 13, -25, 3, 3}, {16, 12, -30, 3, 2}};
        for (int[] w : windows) {
            for (int a = 0; a < w[3]; a++) {
                for (int b = 0; b < w[4]; b++) {
                    BlockPos p = Math.abs(w[0]) == 16 ? at(w[0], w[1] + b, w[2] - a) : at(w[0] + a, w[1] + b, w[2]);
                    world.setBlockState(p, pane, FLAGS);
                }
            }
        }
        // Tường trong tầng trệt
        for (int y = 0; y <= 9; y++) {
            for (int z = -2; z >= -39; z--) world.setBlockState(at(3, y, z), plank(3, z), FLAGS);
            for (int x = -15; x <= 15; x++) world.setBlockState(at(x, y, -23), plank(x, -23), FLAGS);
        }
        // Lối phòng khách sang bếp, bếp sang hành lang
        for (int y = 0; y <= 3; y++) {
            for (int z = -8; z >= -9; z--) world.setBlockState(at(3, y, z), air, FLAGS);
            for (int x = 6; x <= 7; x++) world.setBlockState(at(x, y, -23), air, FLAGS);
        }
    }

    // ==========================================
    // PHÒNG KHÁCH
    // ==========================================

    private static void livingRoom(ServerWorld world) {
        BlockState fence = Blocks.OAK_FENCE.getDefaultState();
        BlockState top = Blocks.OAK_PLANKS.getDefaultState();
        // Bàn to: chân rào cao 3, mặt bàn ván
        for (int[] leg : new int[][]{{-11, -9}, {-5, -9}, {-11, -15}, {-5, -15}}) {
            for (int y = 0; y <= 2; y++) world.setBlockState(at(leg[0], y, leg[1]), fence, FLAGS);
        }
        for (int x = -11; x <= -5; x++) {
            for (int z = -9; z >= -15; z--) world.setBlockState(at(x, 3, z), top, FLAGS);
        }
        // Đồng hồ trên bàn
        world.setBlockState(CLOCK, Blocks.NOTE_BLOCK.getDefaultState(), FLAGS);
        // Hai cái ghế to hai bên bàn
        for (int z = -11; z >= -13; z--) {
            chair(world, -14, z, Direction.EAST, -15);
            chair(world, -2, z, Direction.WEST, -1);
        }
        // Tủ sách sát tường tây
        for (int z = -3; z >= -7; z--) {
            for (int y = 0; y <= 5; y++) world.setBlockState(at(-15, y, z), Blocks.BOOKSHELF.getDefaultState(), FLAGS);
        }
        for (int z = -17; z >= -21; z--) {
            for (int y = 0; y <= 5; y++) world.setBlockState(at(-15, y, z), Blocks.BOOKSHELF.getDefaultState(), FLAGS);
        }
        // Thảm trước cửa
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z >= -5; z--) {
                if (x < 3) world.setBlockState(at(x, 0, z), Blocks.RED_CARPET.getDefaultState(), FLAGS);
            }
        }
        sign(world, at(1, 0, -7), 4, "samsara.tron_tim.bang.bep");
    }

    /** Ghế gấp đôi: mặt ngồi là bậc thang đặt trên một khối ván, lưng ghế cao ba khối. */
    private static void chair(ServerWorld world, int x, int z, Direction facing, int backX) {
        world.setBlockState(at(x, 0, z), Blocks.OAK_PLANKS.getDefaultState(), FLAGS);
        world.setBlockState(at(x, 1, z), Blocks.OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, facing.getOpposite()), FLAGS);
        for (int y = 0; y <= 3; y++) world.setBlockState(at(backX, y, z), Blocks.OAK_PLANKS.getDefaultState(), FLAGS);
    }

    // ==========================================
    // BẾP
    // ==========================================

    private static void kitchen(ServerWorld world) {
        BlockState counter = Blocks.SMOOTH_STONE.getDefaultState();
        for (int z = -3; z >= -12; z--) {
            for (int y = 0; y <= 1; y++) {
                world.setBlockState(at(15, y, z), counter, FLAGS);
                world.setBlockState(at(14, y, z), counter, FLAGS);
            }
        }
        world.setBlockState(at(15, 2, -6), Blocks.WATER_CAULDRON.getDefaultState(), FLAGS);
        world.setBlockState(at(15, 6, -6), Blocks.DRIPSTONE_BLOCK.getDefaultState(), FLAGS);
        // Tủ lạnh: khối sắt cao
        for (int y = 0; y <= 5; y++) {
            for (int z = -15; z >= -16; z--) {
                world.setBlockState(at(15, y, z), Blocks.IRON_BLOCK.getDefaultState(), FLAGS);
                world.setBlockState(at(14, y, z), Blocks.IRON_BLOCK.getDefaultState(), FLAGS);
            }
        }
        // Rương to: thùng xếp 2x2x2
        for (int x = 9; x <= 10; x++) {
            for (int z = -18; z >= -19; z--) {
                for (int y = 0; y <= 1; y++) world.setBlockState(at(x, y, z), Blocks.BARREL.getDefaultState(), FLAGS);
            }
        }
        // Bàn ăn nhỏ
        for (int x = 6; x <= 8; x++) {
            for (int z = -6; z >= -8; z--) world.setBlockState(at(x, 2, z), Blocks.BIRCH_PLANKS.getDefaultState(), FLAGS);
        }
        for (int[] leg : new int[][]{{6, -6}, {8, -6}, {6, -8}, {8, -8}}) {
            for (int y = 0; y <= 1; y++) world.setBlockState(at(leg[0], y, leg[1]), Blocks.BIRCH_FENCE.getDefaultState(), FLAGS);
        }
    }

    // ==========================================
    // HÀNH LANG VÀ CẦU THANG
    // ==========================================

    private static void hallway(ServerWorld world) {
        BlockState step = Blocks.OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH);
        BlockState fill = Blocks.OAK_PLANKS.getDefaultState();
        BlockState air = Blocks.AIR.getDefaultState();
        for (int i = 0; i <= 9; i++) {
            for (int x = 13; x <= 14; x++) {
                int z = -25 - i;
                world.setBlockState(at(x, i, z), step, FLAGS);
                for (int y = 0; y < i; y++) world.setBlockState(at(x, y, z), fill, FLAGS);
            }
        }
        // Lỗ trần trên cầu thang
        for (int x = 13; x <= 14; x++) {
            for (int z = -25; z >= -34; z--) world.setBlockState(at(x, 10, z), air, FLAGS);
        }
        // Lan can trên gác quanh lỗ cầu thang
        for (int z = -25; z >= -34; z--) world.setBlockState(at(12, 11, z), Blocks.OAK_FENCE.getDefaultState(), FLAGS);
        for (int x = 12; x <= 15; x++) world.setBlockState(at(x, 11, -24), Blocks.OAK_FENCE.getDefaultState(), FLAGS);
    }

    // ==========================================
    // PHÒNG NGỦ
    // ==========================================

    private static void bedroomRoom(ServerWorld world) {
        BlockState bed = Blocks.RED_BED.getDefaultState().with(BedBlock.FACING, Direction.NORTH);
        BlockState blue = Blocks.BLUE_BED.getDefaultState().with(BedBlock.FACING, Direction.NORTH);
        world.setBlockState(at(-11, 0, -38), bed.with(BedBlock.PART, BedPart.HEAD), FLAGS);
        world.setBlockState(at(-11, 0, -37), bed.with(BedBlock.PART, BedPart.FOOT), FLAGS);
        world.setBlockState(at(-3, 0, -38), blue.with(BedBlock.PART, BedPart.HEAD), FLAGS);
        world.setBlockState(at(-3, 0, -37), blue.with(BedBlock.PART, BedPart.FOOT), FLAGS);
        sign(world, at(-11, 0, -35), 0, "samsara.tron_tim.bang.giuong_anh");
        sign(world, at(-3, 0, -35), 0, "samsara.tron_tim.bang.giuong_em");
        sign(world, at(-7, 0, -38), 0, "samsara.tron_tim.bang.dung_bam_ngu");
        // Giường em bừa: thảm vứt lung tung
        int[][] mess = {{-4, -36}, {-2, -35}, {-1, -37}, {-4, -34}};
        for (int[] m : mess) world.setBlockState(at(m[0], 0, m[1]), Blocks.LIGHT_BLUE_CARPET.getDefaultState(), FLAGS);
        BlockPos lectern = at(-1, 0, -38);
        world.setBlockState(lectern, Blocks.LECTERN.getDefaultState().with(LecternBlock.FACING, Direction.SOUTH)
                .with(LecternBlock.HAS_BOOK, true), FLAGS);
        if (world.getBlockEntity(lectern) instanceof LecternBlockEntity be) {
            be.setBook(journal());
            be.markDirty();
        }
    }

    /** Cửa phòng ngủ: chưa tìm đủ thì bịt ván và có bảng "fong ngu / cam vao". */
    public static void lockBedroom(ServerWorld world) {
        for (int x = -6; x <= -5; x++) {
            for (int y = 0; y <= 3; y++) world.setBlockState(at(x, y, -23), Blocks.OAK_PLANKS.getDefaultState(), FLAGS);
        }
        sign(world, at(-4, 0, -22), 0, "samsara.tron_tim.bang.fong_ngu", "samsara.tron_tim.bang.cam_vao");
    }

    public static void openBedroom(ServerWorld world) {
        for (int x = -6; x <= -5; x++) {
            for (int y = 0; y <= 3; y++) world.setBlockState(at(x, y, -23), Blocks.AIR.getDefaultState(), FLAGS);
        }
    }

    private static ItemStack journal() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        var nbt = book.getOrCreateNbt();
        nbt.putString(WrittenBookItem.TITLE_KEY, "nhat ky cua em");
        nbt.putString(WrittenBookItem.AUTHOR_KEY, "em");
        nbt.putInt(WrittenBookItem.GENERATION_KEY, 3);
        NbtList pages = new NbtList();
        pages.add(NbtString.of(Text.Serializer.toJson(Text.translatable("samsara.tron_tim.nhat_ky"))));
        nbt.put(WrittenBookItem.PAGES_KEY, pages);
        return book;
    }

    // ==========================================
    // TẦNG GÁC: PHÒNG CHƠI VÀ TỦ QUẦN ÁO
    // ==========================================

    private static void upstairsRoom(ServerWorld world, Random random) {
        BlockState wall = Blocks.SPRUCE_PLANKS.getDefaultState();
        BlockState air = Blocks.AIR.getDefaultState();
        // Tủ quần áo to ở góc tây bắc: hộp ván, ruột rỗng, mặt nam có hai cánh cửa
        for (int x = -15; x <= -11; x++) {
            for (int z = -35; z >= -39; z--) {
                for (int y = 11; y <= 17; y++) {
                    boolean shellBlock = x == -15 || x == -11 || z == -35 || z == -39 || y == 17;
                    world.setBlockState(at(x, y, z), shellBlock ? wall : air, FLAGS);
                }
            }
        }
        BlockState door = Blocks.SPRUCE_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.SOUTH);
        world.setBlockState(at(-14, 11, -35), door.with(DoorBlock.HALF, DoubleBlockHalf.LOWER).with(DoorBlock.HINGE, net.minecraft.block.enums.DoorHinge.LEFT), FLAGS);
        world.setBlockState(at(-14, 12, -35), door.with(DoorBlock.HALF, DoubleBlockHalf.UPPER).with(DoorBlock.HINGE, net.minecraft.block.enums.DoorHinge.LEFT), FLAGS);
        world.setBlockState(at(-13, 11, -35), door.with(DoorBlock.HALF, DoubleBlockHalf.LOWER).with(DoorBlock.HINGE, net.minecraft.block.enums.DoorHinge.RIGHT), FLAGS);
        world.setBlockState(at(-13, 12, -35), door.with(DoorBlock.HALF, DoubleBlockHalf.UPPER).with(DoorBlock.HINGE, net.minecraft.block.enums.DoorHinge.RIGHT), FLAGS);
        // Hộp nhạc cạnh tủ
        world.setBlockState(MUSIC, Blocks.NOTE_BLOCK.getDefaultState(), FLAGS);
        // Đồ chơi: thùng chồng, khối len, một cái giường búp bê nhỏ (bậc thang)
        world.setBlockState(at(5, 11, -10), Blocks.BARREL.getDefaultState(), FLAGS);
        world.setBlockState(at(6, 11, -10), Blocks.BARREL.getDefaultState(), FLAGS);
        world.setBlockState(at(5, 12, -10), Blocks.BARREL.getDefaultState(), FLAGS);
        world.setBlockState(at(-4, 11, -14), Blocks.YELLOW_WOOL.getDefaultState(), FLAGS);
        world.setBlockState(at(-3, 11, -15), Blocks.LIME_WOOL.getDefaultState(), FLAGS);
        world.setBlockState(at(-4, 12, -14), Blocks.RED_WOOL.getDefaultState(), FLAGS);
        world.setBlockState(at(2, 11, -28), Blocks.OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.EAST), FLAGS);
        world.setBlockState(at(3, 11, -28), Blocks.OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.WEST), FLAGS);
        sign(world, at(0, 11, -6), 0, "samsara.tron_tim.bang.do_choi");
    }

    // ==========================================
    // BẢNG
    // ==========================================

    private static void sign(ServerWorld world, BlockPos pos, int rotation, String... keys) {
        world.setBlockState(pos, Blocks.OAK_SIGN.getDefaultState().with(SignBlock.ROTATION, rotation), FLAGS);
        if (world.getBlockEntity(pos) instanceof SignBlockEntity be) {
            SignText text = new SignText();
            for (int i = 0; i < keys.length && i < 4; i++) text = text.withMessage(i, Text.translatable(keys[i]));
            be.setText(text, true);
            be.markDirty();
        }
    }
}
