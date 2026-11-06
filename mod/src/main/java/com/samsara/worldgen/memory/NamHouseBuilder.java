package com.samsara.worldgen.memory;

import com.samsara.entity.DimensionalRiftEntity;
import com.samsara.system.arg.PalimpsestJournalItem;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.SignBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

/**
 * Dựng "Nơi ở của Nam - Tháng 5/2018" ở giữa cánh đồng hoa hướng dương:
 *  - Nhà gỗ sồi khung gỗ vân sam, mái dốc hai bên, cửa quay về con đường mòn (+Z).
 *  - Cối xay gió bên phải nhà (phía +X), cánh buồm len trắng.
 *  - Hầm dưới sàn (cửa sập + thang): một chiếc rương đơn độc và vết rách dẫn sang The Womb.
 * Mọi toạ độ tính theo NamMemoryLayout (mặt cỏ ở GROUND_Y).
 * Lệnh test: /samsara test nam_house (dựng lại tại chỗ cũ).
 */
public final class NamHouseBuilder {
    private NamHouseBuilder() {}

    private static final int G = NamMemoryLayout.GROUND_Y;
    private static final int FLAGS = 2;

    /** Độ dời của toạ độ dựng (0 ở chiều Ký Ức; ở world chu kỳ mới là vị trí gần điểm spawn). */
    private static int ox = 0, oy = 0, oz = 0;
    /** Bản ở world chu kỳ mới: không có vết rách sang The Womb, rương đựng nhật ký của Nam. */
    private static boolean cycleVariant = false;

    /** Dựng ở chiều Ký Ức Của Nam (toạ độ theo NamMemoryLayout). */
    public static void build(ServerWorld world) {
        ox = 0; oy = 0; oz = 0;
        cycleVariant = false;
        buildAll(world);
    }

    /**
     * Dựng ở world "Chu Kỳ #N" (Overworld): mặt sàn đặt lên mặt đất ở {@code ground} (khối đất trên cùng),
     * đắp nền dưới chân nhà + cối xay gió, cửa quay về +Z.
     */
    public static void buildForNewCycle(ServerWorld world, BlockPos ground) {
        ox = ground.getX(); oy = ground.getY() - G; oz = ground.getZ();
        cycleVariant = true;
        try {
            buildAll(world);
        } finally {
            ox = 0; oy = 0; oz = 0;
            cycleVariant = false;
        }
    }

    private static void buildAll(ServerWorld world) {
        // Nạp trước các chunk quanh nhà (sinh hoa / địa hình xong rồi mới đè nhà lên)
        for (int cx = -1; cx <= 2; cx++) {
            for (int cz = -1; cz <= 1; cz++) {
                world.getChunk((ox >> 4) + cx, (oz >> 4) + cz);
            }
        }
        clearArea(world);
        if (cycleVariant) buildFoundation(world);
        buildBasement(world);
        buildHouse(world);
        buildRoof(world);
        furnish(world);
        buildWindmill(world);
        placeSign(world);
        if (!cycleVariant) placeRift(world);
    }

    /** Đắp đất dưới khu nhà + cối xay gió cho phẳng (chỉ ở Overworld, địa hình không bằng phẳng). */
    private static void buildFoundation(ServerWorld world) {
        for (int x = -7; x <= 16; x++) {
            for (int z = -6; z <= 7; z++) {
                for (int y = G - 1; y >= G - 16; y--) {
                    BlockState here = world.getBlockState(pos(x, y, z));
                    if (here.isSolidBlock(world, pos(x, y, z)) && !here.isIn(net.minecraft.registry.tag.BlockTags.LEAVES)) break;
                    set(world, x, y, z, Blocks.DIRT.getDefaultState());
                }
                set(world, x, G, z, Blocks.GRASS_BLOCK.getDefaultState());
            }
        }
    }

    // ==========================================
    // MẶT BẰNG
    // ==========================================

    private static void clearArea(ServerWorld world) {
        // Dọn sạch mọi thứ phía trên mặt cỏ trong khoảng nhà + cối xay gió
        for (int x = -7; x <= 16; x++) {
            for (int z = -6; z <= 6; z++) {
                for (int y = G + 1; y <= G + (cycleVariant ? 24 : 17); y++) {
                    set(world, x, y, z, Blocks.AIR.getDefaultState());
                }
            }
        }
    }

    private static void buildBasement(ServerWorld world) {
        BlockState wall = Blocks.STONE_BRICKS.getDefaultState();
        BlockState mossy = Blocks.MOSSY_STONE_BRICKS.getDefaultState();
        for (int x = -5; x <= 5; x++) {
            for (int z = -3; z <= 4; z++) {
                for (int y = G - 6; y <= G - 1; y++) {
                    boolean shell = x == -5 || x == 5 || z == -3 || z == 4 || y == G - 6 || y == G - 1;
                    BlockState state = shell ? (((x * 7 + y * 3 + z) & 3) == 0 ? mossy : wall) : Blocks.AIR.getDefaultState();
                    set(world, x, y, z, state);
                }
            }
        }
        // Thang lên cửa sập (bám vào tường bắc z = -3)
        BlockState ladder = Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.SOUTH);
        for (int y = G - 5; y <= G - 1; y++) {
            set(world, 3, y, -2, ladder);
        }
        // Mạng nhện ở các góc, đèn linh hồn mờ
        set(world, -4, G - 2, -2, Blocks.COBWEB.getDefaultState());
        set(world, 4, G - 2, 3, Blocks.COBWEB.getDefaultState());
        set(world, -4, G - 5, 3, Blocks.COBWEB.getDefaultState());
        set(world, 4, G - 5, -2, Blocks.SOUL_LANTERN.getDefaultState());

        // Chiếc rương đơn độc
        BlockPos chestPos = pos(-3, G - 5, 3);
        world.setBlockState(chestPos, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, Direction.NORTH), FLAGS);
        if (world.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            // Chu kỳ mới: chỉ còn cuốn nhật ký ("Nếu một ngày em mở file này ra, đừng bấm ngủ.")
            chest.setStack(13, cycleVariant ? PalimpsestJournalItem.createDecodedBook() : PalimpsestJournalItem.createLastPage());
            ItemStack rose = new ItemStack(Items.WITHER_ROSE);
            rose.setCustomName(Text.translatable("samsara.nam_house_builder.doa_hoa_ep_trong"));
            chest.setStack(22, rose);
        }
    }

    // ==========================================
    // NHÀ GỖ
    // ==========================================

    private static void buildHouse(ServerWorld world) {
        BlockState planks = Blocks.OAK_PLANKS.getDefaultState();
        BlockState post = Blocks.SPRUCE_LOG.getDefaultState();
        BlockState beam = Blocks.STRIPPED_SPRUCE_LOG.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.X);
        BlockState beamZ = Blocks.STRIPPED_SPRUCE_LOG.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Z);
        BlockState glass = Blocks.GLASS_PANE.getDefaultState();

        // Móng đá + sàn gỗ
        for (int x = -5; x <= 5; x++) {
            for (int z = -4; z <= 4; z++) {
                boolean edge = x == -5 || x == 5 || z == -4 || z == 4;
                set(world, x, G, z, edge ? Blocks.COBBLESTONE.getDefaultState() : Blocks.SPRUCE_PLANKS.getDefaultState());
            }
        }

        // Tường
        for (int y = G + 1; y <= G + 4; y++) {
            for (int x = -5; x <= 5; x++) {
                for (int z = -4; z <= 4; z++) {
                    boolean wallX = x == -5 || x == 5;
                    boolean wallZ = z == -4 || z == 4;
                    if (!wallX && !wallZ) continue;
                    BlockState state = planks;
                    if (wallX && wallZ) state = post;
                    else if (y == G + 4) state = wallZ ? beam : beamZ;
                    set(world, x, y, z, state);
                }
            }
        }

        // Cửa sổ (kính đơn): trước, sau, hai bên
        for (int y = G + 2; y <= G + 3; y++) {
            set(world, -3, y, 4, glass);
            set(world, 3, y, 4, glass);
            set(world, -2, y, -4, glass);
            set(world, 2, y, -4, glass);
            set(world, -5, y, 0, glass);
            set(world, 5, y, 0, glass);
        }

        // Cửa chính quay về con đường mòn
        BlockState door = Blocks.SPRUCE_DOOR.getDefaultState()
                .with(DoorBlock.FACING, Direction.SOUTH)
                .with(DoorBlock.HINGE, DoorHinge.LEFT);
        set(world, 0, G + 1, 4, door.with(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(world, 0, G + 2, 4, door.with(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        // Bậc thềm
        set(world, 0, G, 5, Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
    }

    private static void buildRoof(ServerWorld world) {
        BlockState stairN = Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH);
        BlockState stairS = Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH);
        BlockState planks = Blocks.OAK_PLANKS.getDefaultState();

        for (int i = 0; i <= 4; i++) {
            int y = G + 5 + i;
            for (int x = -6; x <= 6; x++) {
                set(world, x, y, -5 + i, stairN);
                set(world, x, y, 5 - i, stairS);
            }
            // Đầu hồi tam giác hai bên
            for (int z = -4 + i; z <= 4 - i; z++) {
                set(world, -5, y, z, planks);
                set(world, 5, y, z, planks);
            }
        }
        for (int x = -6; x <= 6; x++) {
            set(world, x, G + 9, 0, Blocks.DARK_OAK_PLANKS.getDefaultState());
        }
        // Ô cửa sổ nhỏ trên đầu hồi
        set(world, -5, G + 6, 0, Blocks.GLASS_PANE.getDefaultState());
        set(world, 5, G + 6, 0, Blocks.GLASS_PANE.getDefaultState());
    }

    private static void furnish(ServerWorld world) {
        // Giường (đầu giường quay về phía bắc), vẫn còn như vừa có người nằm
        BlockState bed = Blocks.RED_BED.getDefaultState().with(BedBlock.FACING, Direction.NORTH);
        set(world, -3, G + 1, -2, bed.with(BedBlock.PART, BedPart.HEAD));
        set(world, -3, G + 1, -1, bed.with(BedBlock.PART, BedPart.FOOT));

        set(world, -4, G + 1, 1, Blocks.CRAFTING_TABLE.getDefaultState());
        set(world, -4, G + 1, 2, Blocks.FURNACE.getDefaultState().with(net.minecraft.block.FurnaceBlock.FACING, Direction.EAST));
        set(world, -4, G + 1, 3, Blocks.BOOKSHELF.getDefaultState());
        set(world, -4, G + 2, 3, Blocks.BOOKSHELF.getDefaultState());
        set(world, 4, G + 1, 3, Blocks.BARREL.getDefaultState());
        set(world, 1, G + 1, -3, Blocks.POTTED_DEAD_BUSH.getDefaultState());
        set(world, -1, G + 1, -3, Blocks.LANTERN.getDefaultState());
        set(world, 0, G + 3, -3, Blocks.WALL_TORCH.getDefaultState().with(WallTorchBlock.FACING, Direction.SOUTH));
        for (int x = -1; x <= 1; x++) {
            for (int z = 0; z <= 2; z++) {
                set(world, x, G + 1, z, Blocks.BROWN_CARPET.getDefaultState());
            }
        }

        // Cửa sập xuống hầm (nằm sát mặt sàn)
        set(world, 3, G, -2, Blocks.SPRUCE_TRAPDOOR.getDefaultState()
                .with(TrapdoorBlock.FACING, Direction.SOUTH)
                .with(TrapdoorBlock.HALF, BlockHalf.TOP));
    }

    // ==========================================
    // CỐI XAY GIÓ
    // ==========================================

    private static void buildWindmill(ServerWorld world) {
        BlockState stone = Blocks.COBBLESTONE.getDefaultState();
        BlockState wood = Blocks.SPRUCE_PLANKS.getDefaultState();
        BlockState corner = Blocks.STRIPPED_SPRUCE_LOG.getDefaultState();

        // Tháp ở x 9..11; trục cao hơn đỉnh mái nhà (G + 9) để cánh buồm không cắt vào mái
        int top = G + 13;
        for (int y = G; y <= top; y++) {
            for (int x = 9; x <= 11; x++) {
                for (int z = -2; z <= 0; z++) {
                    boolean isCorner = (x == 9 || x == 11) && (z == -2 || z == 0);
                    BlockState state = y <= G + 2 ? stone : (isCorner ? corner : wood);
                    set(world, x, y, z, state);
                }
            }
        }
        // Mái chóp
        for (int x = 8; x <= 12; x++) {
            set(world, x, top + 1, -3, Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
            set(world, x, top + 1, 1, Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
            for (int z = -2; z <= 0; z++) {
                set(world, x, top + 1, z, Blocks.DARK_OAK_PLANKS.getDefaultState());
            }
        }
        for (int x = 9; x <= 11; x++) set(world, x, top + 2, -1, Blocks.DARK_OAK_SLAB.getDefaultState());

        // Trục + 4 cánh buồm trên mặt nam (z = 1)
        int hx = 10;
        int hy = G + 11;
        int hz = 1;
        set(world, hx, hy, hz, Blocks.SPRUCE_LOG.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Z));
        BlockState spine = Blocks.OAK_FENCE.getDefaultState();
        BlockState sail = Blocks.WHITE_WOOL.getDefaultState();
        for (int i = 1; i <= 5; i++) {
            // Lên
            set(world, hx, hy + i, hz, spine);
            if (i >= 2) set(world, hx + 1, hy + i, hz, sail);
            // Xuống
            set(world, hx, hy - i, hz, spine);
            if (i >= 2) set(world, hx - 1, hy - i, hz, sail);
            // Trái
            set(world, hx - i, hy, hz, spine);
            if (i >= 2) set(world, hx - i, hy + 1, hz, sail);
            // Phải
            set(world, hx + i, hy, hz, spine);
            if (i >= 2) set(world, hx + i, hy - 1, hz, sail);
        }
    }

    // ==========================================
    // BIỂN BÁO & VẾT RÁCH
    // ==========================================

    private static void placeSign(ServerWorld world) {
        BlockPos signPos = pos(2, G + 1, 6);
        world.setBlockState(signPos, Blocks.OAK_SIGN.getDefaultState().with(SignBlock.ROTATION, 0), FLAGS);
        if (world.getBlockEntity(signPos) instanceof SignBlockEntity sign) {
            SignText text = sign.getFrontText()
                    .withMessage(0, Text.translatable("samsara.nam_house_builder.noi_o_cua_nam"))
                    .withMessage(1, Text.translatable("samsara.nam_house_builder.thang_5_2018"))
                    .withMessage(2, Text.literal(""))
                    .withMessage(3, Text.translatable("samsara.nam_house_builder.dung_bam_ngu"));
            sign.setText(text, true);
        }
    }

    private static void placeRift(ServerWorld world) {
        Box basement = new Box(-5, G - 6, -3, 6, G, 5);
        for (Entity old : world.getEntitiesByClass(DimensionalRiftEntity.class, basement, e -> true)) {
            old.discard();
        }
        DimensionalRiftEntity rift = new DimensionalRiftEntity(world, 0.5, G - 5, 0.5, 0.0f,
                "womb_" + DimensionalRiftEntity.defaultWombStyle);
        world.spawnEntity(rift);
    }

    private static void set(ServerWorld world, int x, int y, int z, BlockState state) {
        world.setBlockState(pos(x, y, z), state, FLAGS);
    }

    private static BlockPos pos(int x, int y, int z) {
        return new BlockPos(x + ox, y + oy, z + oz);
    }
}
