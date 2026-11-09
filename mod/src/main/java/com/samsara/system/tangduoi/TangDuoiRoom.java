package com.samsara.system.tangduoi;

import com.samsara.block.MemorySunflowerBlock;
import com.samsara.registry.SamsaraBlocks;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.LightBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.WrittenBookItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Tầng Dưới, tầng 1: hai đầu của Đường Ống.
 *  - Đầu vào: bản sao căn hầm nhà Nam (rương trống, không còn vết rách); mảng gạch bong ở tường bắc mở vào ống.
 *  - Cuối ống (sau ngã 5): cánh cửa gỗ, phòng ngủ năm 2018 im phăng phắc, ngoài cửa sổ cả cánh đồng quay lưng.
 * Toạ độ phòng tính từ tâm ô cuối (CENTER[5]), toạ độ hầm từ tâm ô đầu (CENTER[0]).
 */
public final class TangDuoiRoom {
    private TangDuoiRoom() {}

    private static final int END = DuongOngBuilder.JUNCTIONS;
    private static final int FLAGS = 2 | 16;

    private static BlockPos end(int x, int y, int z) {
        return DuongOngBuilder.ORIGIN.add(DuongOngBuilder.CENTER[END][0] + x, y, DuongOngBuilder.CENTER[END][1] + z);
    }

    private static BlockPos start(int x, int y, int z) {
        return DuongOngBuilder.ORIGIN.add(x, y, z);
    }

    /** Chỗ người chơi xuất hiện trong hầm, nhìn về phía tường bong. */
    public static BlockPos arrival() {
        return start(0, 1, 46);
    }

    /** Giữa phòng ngủ (lệnh test). */
    public static BlockPos roomCenter() {
        return end(0, 1, -8);
    }

    public static boolean isInRoom(double worldX, double worldZ) {
        double x = worldX - DuongOngBuilder.ORIGIN.getX() - DuongOngBuilder.CENTER[END][0];
        double z = worldZ - DuongOngBuilder.ORIGIN.getZ() - DuongOngBuilder.CENTER[END][1];
        return Math.abs(x) <= 7.5 && z <= -2.0 && z >= -16.5;
    }

    /** Đã vào hẳn trong phòng: cánh cửa sau lưng biến thành tường. */
    public static boolean isDeepInRoom(double worldX, double worldZ) {
        double z = worldZ - DuongOngBuilder.ORIGIN.getZ() - DuongOngBuilder.CENTER[END][1];
        return isInRoom(worldX, worldZ) && z < -5.0;
    }

    public static void sealDoor(ServerWorld world) {
        BlockState wall = SamsaraBlocks.NAM_WALLPAPER.getDefaultState();
        if (world.getBlockState(end(0, 1, -2)).getBlock() instanceof DoorBlock) {
            world.setBlockState(end(0, 1, -2), wall, 2 | 16);
            world.setBlockState(end(0, 2, -2), wall, 2 | 16);
            world.playSound(null, end(0, 1, -2), net.minecraft.sound.SoundEvents.BLOCK_WOODEN_DOOR_CLOSE,
                    net.minecraft.sound.SoundCategory.BLOCKS, 0.7f, 0.8f);
        }
    }

    public static void build(ServerWorld world) {
        buildCellar(world);
        buildRoom(world);
    }

    // ==========================================
    // CĂN HẦM (giống hầm nhà Nam)
    // ==========================================

    private static void buildCellar(ServerWorld world) {
        BlockState wall = Blocks.STONE_BRICKS.getDefaultState();
        BlockState mossy = Blocks.MOSSY_STONE_BRICKS.getDefaultState();
        BlockState air = Blocks.AIR.getDefaultState();
        for (int x = -6; x <= 6; x++) {
            for (int z = 41; z <= 50; z++) {
                for (int y = 0; y <= 6; y++) {
                    boolean shell = Math.abs(x) == 6 || z == 41 || z == 50 || y == 0 || y == 6;
                    world.setBlockState(start(x, y, z), shell ? (((x * 7 + y * 3 + z) & 3) == 0 ? mossy : wall) : air, FLAGS);
                }
            }
        }
        // Tường bắc đóng kín: ổ khoá bốn bánh mở lối vào ống
        OKhoaHam.build(world);
        BlockState ladder = Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.NORTH);
        for (int y = 1; y <= 5; y++) world.setBlockState(start(3, y, 49), ladder, FLAGS);
        world.setBlockState(start(-4, 5, 42), Blocks.COBWEB.getDefaultState(), FLAGS);
        world.setBlockState(start(4, 5, 49), Blocks.COBWEB.getDefaultState(), FLAGS);
        world.setBlockState(start(-4, 1, 42), Blocks.COBWEB.getDefaultState(), FLAGS);
        world.setBlockState(start(4, 1, 43), Blocks.SOUL_LANTERN.getDefaultState(), FLAGS);
        // Chiếc rương giống hệt, nhưng trống
        world.setBlockState(start(-3, 1, 48), Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, Direction.NORTH), FLAGS);
    }

    // ==========================================
    // PHÒNG NGỦ 2018
    // ==========================================

    private static void buildRoom(ServerWorld world) {
        BlockState wall = SamsaraBlocks.NAM_WALLPAPER.getDefaultState();
        BlockState floor = Blocks.OAK_PLANKS.getDefaultState();
        BlockState air = Blocks.AIR.getDefaultState();
        for (int x = -7; x <= 7; x++) {
            for (int z = -16; z <= -2; z++) {
                for (int y = 0; y <= 5; y++) {
                    BlockState s;
                    if (y == 0 || y == 5) s = floor;
                    else if (Math.abs(x) == 7 || z == -16 || z == -2) s = wall;
                    else s = air;
                    world.setBlockState(end(x, y, z), s, FLAGS);
                }
            }
        }

        // Bậc lên và cánh cửa gỗ ở cuối ống
        world.setBlockState(end(0, 0, 0), Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH), FLAGS);
        world.setBlockState(end(0, 1, -1), air, FLAGS);
        world.setBlockState(end(0, 2, -1), air, FLAGS);
        BlockState door = Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.SOUTH);
        world.setBlockState(end(0, 1, -2), door.with(DoorBlock.HALF, DoubleBlockHalf.LOWER), FLAGS);
        world.setBlockState(end(0, 2, -2), door.with(DoorBlock.HALF, DoubleBlockHalf.UPPER), FLAGS);

        // Giường đỏ, đầu giường quay về bắc
        BlockState bed = Blocks.RED_BED.getDefaultState().with(BedBlock.FACING, Direction.NORTH);
        world.setBlockState(end(-4, 1, -15), bed.with(BedBlock.PART, BedPart.HEAD), FLAGS);
        world.setBlockState(end(-4, 1, -14), bed.with(BedBlock.PART, BedPart.FOOT), FLAGS);
        DuongOngBuilder.placeSign(world, end(-4, 3, -15), Direction.SOUTH, null, "samsara.tang_duoi.dung_bam_ngu");

        // Đồ đạc
        for (int x = -2; x <= 2; x++) {
            for (int z = -10; z <= -7; z++) world.setBlockState(end(x, 1, z), Blocks.BROWN_CARPET.getDefaultState(), FLAGS);
        }
        world.setBlockState(end(-6, 1, -5), Blocks.BOOKSHELF.getDefaultState(), FLAGS);
        world.setBlockState(end(-6, 2, -5), Blocks.BOOKSHELF.getDefaultState(), FLAGS);
        world.setBlockState(end(-6, 1, -4), Blocks.BARREL.getDefaultState(), FLAGS);

        // Mảnh nhật ký trên giá đọc ở góc học tập
        BlockPos lectern = end(4, 1, -14);
        world.setBlockState(lectern, Blocks.LECTERN.getDefaultState().with(LecternBlock.FACING, Direction.SOUTH).with(LecternBlock.HAS_BOOK, true), FLAGS);
        if (world.getBlockEntity(lectern) instanceof LecternBlockEntity be) {
            be.setBook(journal());
            be.markDirty();
        }

        // Đúng một ô chưa tải
        world.setBlockState(end(7, 2, -13), SamsaraBlocks.KHOI_CHUA_TAI.getDefaultState(), FLAGS);

        // Ánh sáng không có nguồn: căn phòng sáng như một ký ức
        BlockState light = Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, 12);
        for (BlockPos p : new BlockPos[]{end(0, 4, -9), end(-4, 4, -12), end(4, 4, -6), end(4, 4, -13), end(-4, 4, -5)}) {
            world.setBlockState(p, light, FLAGS);
            DuongOngBuilder.RELIGHT.add(p);
        }

        buildWindow(world);
    }

    /** Cửa sổ tường đông; bên ngoài là cánh đồng đứng yên, mọi bông hoa quay lưng lại với căn phòng. */
    private static void buildWindow(ServerWorld world) {
        BlockState sky = Blocks.LIGHT_BLUE_CONCRETE.getDefaultState();
        BlockState grass = Blocks.GRASS_BLOCK.getDefaultState();
        BlockState air = Blocks.AIR.getDefaultState();
        for (int z = -11; z <= -7; z++) {
            for (int y = 2; y <= 3; y++) world.setBlockState(end(7, y, z), Blocks.GLASS.getDefaultState(), FLAGS);
        }
        for (int x = 8; x <= 21; x++) {
            for (int z = -17; z <= -2; z++) {
                for (int y = 0; y <= 8; y++) {
                    boolean edge = x == 21 || z == -17 || z == -2 || y == 8;
                    BlockState s = edge ? sky : y == 0 ? grass : air;
                    world.setBlockState(end(x, y, z), s, FLAGS);
                }
            }
        }
        BlockState flower = SamsaraBlocks.MEMORY_SUNFLOWER.getDefaultState().with(MemorySunflowerBlock.FACING, Direction.EAST);
        for (int x = 10; x <= 19; x += 2) {
            for (int z = -15; z <= -4; z += 2) {
                world.setBlockState(end(x, 1, z), flower.with(TallPlantBlock.HALF, DoubleBlockHalf.LOWER), FLAGS);
                world.setBlockState(end(x, 2, z), flower.with(TallPlantBlock.HALF, DoubleBlockHalf.UPPER), FLAGS);
            }
        }
        BlockState light = Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, 15);
        for (int x = 10; x <= 18; x += 4) {
            for (int z = -14; z <= -5; z += 4) {
                world.setBlockState(end(x, 6, z), light, FLAGS);
                DuongOngBuilder.RELIGHT.add(end(x, 6, z));
            }
        }
    }

    private static ItemStack journal() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        NbtCompound nbt = book.getOrCreateNbt();
        nbt.putString(WrittenBookItem.TITLE_KEY, "Mảnh Nhật Ký: 05/2018");
        nbt.putString(WrittenBookItem.AUTHOR_KEY, "Nam_1998");
        nbt.putInt(WrittenBookItem.GENERATION_KEY, 3);
        NbtList pages = new NbtList();
        pages.add(NbtString.of(Text.Serializer.toJson(Text.translatable("samsara.tang_duoi.nhat_ky_2018"))));
        nbt.put(WrittenBookItem.PAGES_KEY, pages);
        return book;
    }
}
