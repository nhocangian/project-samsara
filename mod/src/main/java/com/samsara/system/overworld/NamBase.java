package com.samsara.system.overworld;

import com.samsara.system.cycle.EggCycleState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.FurnaceBlock;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.ChestType;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.List;

/**
 * Khu nhà của Nam ở Overworld (kịch bản Video 1: người em vào game là thấy ngay căn nhà gỗ của Nam gần spawn, đồ đạc
 * còn nguyên như người chủ vừa rời đi). Dựng kiểu một cậu bé chơi năm 2011-2013: nhà hộp ván sồi khung gỗ tròn, sàn đá
 * cuội, cửa kính; ruộng lúa có rãnh nước; chuồng rào gỗ có bò, cừu, lợn, gà; lối xuống hầm mỏ bậc thang; góc phù phép
 * với bàn phù phép, kệ sách, đe; cổng địa ngục đã thắp. Đường sỏi nối các khu, đuốc cắm trên cọc rào.
 *
 * <p>Dựng một lần ở world đầu tiên (Chu Kỳ #1) lúc người chơi vào game lần đầu, trên một nền san phẳng 40x34 khối bắt
 * đầu 14 khối phía nam điểm spawn (người chơi xuất hiện là nhìn thấy nhà ngay trước mặt; Bia Mộ ở spawn + (3, 3) không bị
 * đụng). Mép nền được vuốt dốc dần về địa hình cũ. Lán đất "đêm đầu tiên" và các dấu vết khác ({@link NamTraces}) nằm
 * ở rìa phía bắc / tây khu nhà: nơi Nam trú đêm đầu trước khi xây nhà thật.
 */
public final class NamBase extends PersistentState {
    private static final String KEY = "samsara_nam_base";
    private boolean built;
    /** Góc tây bắc của nền (y = mặt đất) lần dựng ở world này, cho lệnh chuẩn bị quay đặt người chơi vào nhà. */
    private BlockPos origin;

    /** Nền: x từ -20 tới +19, z từ +14 tới +47 so với spawn (vành dốc 8 khối dừng ở z +6, trước Bia Mộ ở z +2..4). */
    static final int PAD_X0 = -20, PAD_Z0 = 14, PAD_W = 40, PAD_D = 34;
    private static final int RAMP = 8;
    private static final int FLAGS = 2;

    private final List<BlockPos> fixups = new ArrayList<>();
    private ServerWorld world;
    private BlockPos o;

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerWorld world = server.getOverworld();
            NamBase state = world.getPersistentStateManager().getOrCreate(NamBase::fromNbt, NamBase::new, KEY);
            if (state.built) return;
            state.built = true;
            state.markDirty();
            if (EggCycleState.get(server).getCycle() > 1) return;   // chu kỳ mới có nhà Ký Ức riêng, không có khu này
            state.origin = build(world, world.getSpawnPos());
        });
    }

    /** Dựng khu nhà coi {@code spawn} là điểm spawn (lệnh test dựng quanh người chơi). Trả về góc nền. */
    public static BlockPos build(ServerWorld world, BlockPos spawn) {
        NamBase b = new NamBase();
        b.world = world;
        b.run(spawn);
        // Lần dựng gần nhất (kể cả lệnh test) là khu nhà mà lệnh chuẩn bị quay dùng
        NamBase state = world.getPersistentStateManager().getOrCreate(NamBase::fromNbt, NamBase::new, KEY);
        state.origin = b.o;
        state.markDirty();
        return b.o;
    }

    /** Ô đứng trong nhà (giữa nhà, nhìn ra cửa) hoặc trước cửa nhà; null nếu world này chưa có khu nhà. */
    public static BlockPos houseSpot(ServerWorld world, boolean inside) {
        NamBase state = world.getPersistentStateManager().getOrCreate(NamBase::fromNbt, NamBase::new, KEY);
        if (state.origin == null) return null;
        return inside ? state.origin.add(19, 0, 4) : state.origin.add(19, 0, -2);
    }

    private void run(BlockPos spawn) {
        int baseY = groundY(spawn.getX() + PAD_X0 + PAD_W / 2, spawn.getZ() + PAD_Z0 + PAD_D / 2);
        o = new BlockPos(spawn.getX() + PAD_X0, baseY, spawn.getZ() + PAD_Z0);
        for (int x = -RAMP; x < PAD_W + RAMP; x++) {
            for (int z = -RAMP; z < PAD_D + RAMP; z++) world.getChunk(o.add(x, 0, z));
        }
        level();
        house(15, 2);
        farm(2, 3);
        pen(28, 2);
        mine(4, 22);
        enchanting(15, 20);
        portal(30, 22);
        paths();
        for (BlockPos p : fixups) {
            BlockState s = world.getBlockState(p);
            world.setBlockState(p, Block.postProcessState(s, world, p), FLAGS);
        }
    }

    // ==========================================
    // NỀN
    // ==========================================

    private int groundY(int x, int z) {
        BlockPos.Mutable p = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z)).mutableCopy();
        while (p.getY() > world.getBottomY() + 1) {
            BlockState below = world.getBlockState(p.down());
            if (!below.getFluidState().isEmpty()) break;   // mặt nước: không san xuống tận đáy
            if (!below.isIn(BlockTags.LOGS) && !below.isIn(BlockTags.LEAVES) && !below.isReplaceable()) break;
            p.move(Direction.DOWN);
        }
        return p.getY();
    }

    /** San phẳng nền ở độ cao o.y (mặt cỏ ở o.y - 1), vành RAMP khối vuốt dần về địa hình cũ. */
    private void level() {
        for (int x = -RAMP; x < PAD_W + RAMP; x++) {
            for (int z = -RAMP; z < PAD_D + RAMP; z++) {
                int dx = x < 0 ? -x : x >= PAD_W ? x - PAD_W + 1 : 0;
                int dz = z < 0 ? -z : z >= PAD_D ? z - PAD_D + 1 : 0;
                int d = Math.max(dx, dz);
                int wx = o.getX() + x, wz = o.getZ() + z;
                int orig = groundY(wx, wz);
                int h = d == 0 ? o.getY() : Math.round(o.getY() + (orig - o.getY()) * (d / (float) (RAMP + 1)));
                // Đắp đất tới mặt mới, phạt hết phía trên (cây cối trên nền thành tán lá lơ lửng rồi tự rụng)
                for (int y = Math.min(h, orig) - 4; y < h - 1; y++) {
                    BlockPos p = new BlockPos(wx, y, wz);
                    if (world.getBlockState(p).isReplaceable() || y >= orig - 1) world.setBlockState(p, Blocks.DIRT.getDefaultState(), FLAGS);
                }
                world.setBlockState(new BlockPos(wx, h - 1, wz), Blocks.GRASS_BLOCK.getDefaultState(), FLAGS);
                int top = Math.max(orig, h) + (d == 0 ? 14 : 6);
                for (int y = h; y < top; y++) world.setBlockState(new BlockPos(wx, y, wz), Blocks.AIR.getDefaultState(), FLAGS);
            }
        }
    }

    // ==========================================
    // TIỆN ÍCH ĐẶT KHỐI (toạ độ tương đối với góc nền o; y = 0 là mặt đất)
    // ==========================================

    private BlockPos at(int x, int y, int z) {
        return o.add(x, y, z);
    }

    private void set(int x, int y, int z, BlockState s) {
        BlockPos p = at(x, y, z);
        // Dựng lại chồng lên khu cũ (lệnh test): dọn đồ trong rương / lò cũ trước, kẻo đồ văng ra sàn
        if (world.getBlockEntity(p) instanceof net.minecraft.util.Clearable c) c.clear();
        world.setBlockState(p, s, FLAGS);
    }

    private void setFix(int x, int y, int z, BlockState s) {
        set(x, y, z, s);
        fixups.add(at(x, y, z));
    }

    private void torchOnWall(int x, int y, int z, Direction facing) {
        set(x, y, z, Blocks.WALL_TORCH.getDefaultState().with(WallTorchBlock.FACING, facing));
    }

    // ==========================================
    // NHÀ GỖ: 9x7, cửa phía bắc (quay về spawn), cao 4, mái ván bậc
    // ==========================================

    private void house(int x0, int z0) {
        int w = 9, d = 7, h = 4;
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                set(x0 + x, -1, z0 + z, Blocks.COBBLESTONE.getDefaultState());
                boolean edgeX = x == 0 || x == w - 1, edgeZ = z == 0 || z == d - 1;
                for (int y = 0; y < h; y++) {
                    BlockState s = Blocks.AIR.getDefaultState();
                    if (edgeX && edgeZ) s = Blocks.OAK_LOG.getDefaultState();
                    else if (edgeX || edgeZ) s = Blocks.OAK_PLANKS.getDefaultState();
                    set(x0 + x, y, z0 + z, s);
                }
            }
        }
        // Cửa sổ kính (tấm kính) ở giữa mỗi bức tường, cao 1-2
        for (int y = 1; y <= 2; y++) {
            for (int x : new int[]{2, 3, 5, 6}) {
                setFix(x0 + x, y, z0, Blocks.GLASS_PANE.getDefaultState());
                setFix(x0 + x, y, z0 + d - 1, Blocks.GLASS_PANE.getDefaultState());
            }
            for (int z : new int[]{2, 4}) {
                setFix(x0, y, z0 + z, Blocks.GLASS_PANE.getDefaultState());
                setFix(x0 + w - 1, y, z0 + z, Blocks.GLASS_PANE.getDefaultState());
            }
        }
        // Cửa gỗ giữa tường bắc
        int dx = x0 + 4;
        set(dx, 0, z0, Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.NORTH).with(DoorBlock.HALF, DoubleBlockHalf.LOWER).with(DoorBlock.HINGE, DoorHinge.LEFT));
        set(dx, 1, z0, Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.NORTH).with(DoorBlock.HALF, DoubleBlockHalf.UPPER).with(DoorBlock.HINGE, DoorHinge.LEFT));
        torchOnWall(dx - 1, 2, z0 - 1, Direction.NORTH);
        torchOnWall(dx + 1, 2, z0 - 1, Direction.NORTH);
        // Mái: viền bậc ván hai bên bắc / nam, giữa là phiến ván, kiểu mái bằng của trẻ con
        for (int x = -1; x <= w; x++) {
            setFix(x0 + x, h, z0 - 1, Blocks.OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
            setFix(x0 + x, h, z0 + d, Blocks.OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
            for (int z = 0; z < d; z++) {
                set(x0 + x, h, z0 + z, (x == -1 || x == w) ? Blocks.OAK_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.BOTTOM) : Blocks.OAK_PLANKS.getDefaultState());
            }
        }
        // Nội thất: giường góc tây nam, bàn chế tạo, hai lò, hai rương đôi, đuốc trên tường
        set(x0 + 1, 0, z0 + 5, Blocks.RED_BED.getDefaultState().with(BedBlock.FACING, Direction.SOUTH).with(BedBlock.PART, BedPart.HEAD));
        set(x0 + 1, 0, z0 + 4, Blocks.RED_BED.getDefaultState().with(BedBlock.FACING, Direction.SOUTH).with(BedBlock.PART, BedPart.FOOT));
        set(x0 + 7, 0, z0 + 1, Blocks.CRAFTING_TABLE.getDefaultState());
        set(x0 + 7, 0, z0 + 2, Blocks.FURNACE.getDefaultState().with(FurnaceBlock.FACING, Direction.WEST));
        set(x0 + 7, 0, z0 + 3, Blocks.FURNACE.getDefaultState().with(FurnaceBlock.FACING, Direction.WEST));
        chestPair(x0 + 4, 0, z0 + 5, Direction.NORTH, true);
        chestPair(x0 + 6, 0, z0 + 5, Direction.NORTH, false);
        torchOnWall(x0 + 4, 2, z0 + 1, Direction.SOUTH);
        torchOnWall(x0 + 1, 2, z0 + 3, Direction.EAST);
        torchOnWall(x0 + 7, 2, z0 + 4, Direction.WEST);
        // Hai chậu hoa trên bậu cửa sổ: đứa trẻ hái được bông nào thì cắm bông đó
        set(x0 + 2, 0, z0 + 1, Blocks.POTTED_POPPY.getDefaultState());
        set(x0 + 3, 0, z0 + 1, Blocks.POTTED_DANDELION.getDefaultState());
    }

    private void chestPair(int x, int y, int z, Direction facing, boolean food) {
        // Rương quay mặt về bắc: nửa LEFT ở x, nửa kia ở phía đông (ChestBlock.getFacing: LEFT -> rotateYClockwise)
        set(x, y, z, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, facing).with(ChestBlock.CHEST_TYPE, ChestType.LEFT));
        set(x + 1, y, z, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, facing).with(ChestBlock.CHEST_TYPE, ChestType.RIGHT));
        if (!(world.getBlockEntity(at(x, y, z)) instanceof ChestBlockEntity a)) return;
        ItemStack[] items = food
                ? new ItemStack[]{new ItemStack(Items.WHEAT, 23), new ItemStack(Items.WHEAT_SEEDS, 41), new ItemStack(Items.BREAD, 6),
                new ItemStack(Items.COOKED_PORKCHOP, 4), new ItemStack(Items.APPLE, 3), new ItemStack(Items.ROTTEN_FLESH, 17),
                new ItemStack(Items.STRING, 9), new ItemStack(Items.BONE, 5), new ItemStack(Items.WHITE_WOOL, 7), new ItemStack(Items.EGG, 4)}
                : new ItemStack[]{new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.DIRT, 38),
                new ItemStack(Items.COAL, 27), new ItemStack(Items.IRON_INGOT, 11), new ItemStack(Items.GOLD_INGOT, 2),
                new ItemStack(Items.DIAMOND, 3), new ItemStack(Items.REDSTONE, 14), new ItemStack(Items.OAK_LOG, 19),
                new ItemStack(Items.TORCH, 30), new ItemStack(Items.BUCKET), new ItemStack(Items.SADDLE), damaged(Items.STONE_SWORD, 40)};
        for (int i = 0; i < items.length; i++) a.setStack(i * 2 % 27, items[i]);
    }

    private static ItemStack damaged(net.minecraft.item.Item item, int left) {
        ItemStack s = new ItemStack(item);
        s.setDamage(Math.max(0, s.getMaxDamage() - left));
        return s;
    }

    // ==========================================
    // RUỘNG LÚA 9x9, rãnh nước ở hàng giữa, viền gỗ tròn
    // ==========================================

    private void farm(int x0, int z0) {
        for (int x = -1; x <= 9; x++) {
            for (int z = -1; z <= 9; z++) {
                boolean edge = x == -1 || x == 9 || z == -1 || z == 9;
                if (edge) {
                    set(x0 + x, -1, z0 + z, Blocks.OAK_LOG.getDefaultState().with(net.minecraft.block.PillarBlock.AXIS,
                            (x == -1 || x == 9) ? Direction.Axis.Z : Direction.Axis.X));
                    continue;
                }
                if (z == 4) {
                    set(x0 + x, -1, z0 + z, Blocks.WATER.getDefaultState());
                    continue;
                }
                set(x0 + x, -1, z0 + z, Blocks.FARMLAND.getDefaultState().with(FarmlandBlock.MOISTURE, 7));
                // Lúa lớn không đều: chỗ vừa gặt, chỗ sắp chín
                int age = (x * 7 + z * 3) % 11 < 2 ? 0 : Math.min(7, 3 + (x * 5 + z) % 5);
                set(x0 + x, 0, z0 + z, Blocks.WHEAT.getDefaultState().with(CropBlock.AGE, age));
            }
        }
        torchOnFence(x0 - 1, z0 - 1);
        torchOnFence(x0 + 9, z0 - 1);
    }

    private void torchOnFence(int x, int z) {
        setFix(x, 0, z, Blocks.OAK_FENCE.getDefaultState());
        set(x, 1, z, Blocks.TORCH.getDefaultState());
    }

    // ==========================================
    // CHUỒNG: rào gỗ 9x8, cổng phía tây, bò / cừu / lợn / gà
    // ==========================================

    private void pen(int x0, int z0) {
        int w = 9, d = 8;
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                if (x == 0 || x == w - 1 || z == 0 || z == d - 1) setFix(x0 + x, 0, z0 + z, Blocks.OAK_FENCE.getDefaultState());
            }
        }
        setFix(x0, 0, z0 + 3, Blocks.OAK_FENCE_GATE.getDefaultState().with(FenceGateBlock.FACING, Direction.WEST));
        set(x0, 1, z0, Blocks.TORCH.getDefaultState());
        set(x0 + w - 1, 1, z0 + d - 1, Blocks.TORCH.getDefaultState());
        // Máng nước nhỏ và cỏ khô
        set(x0 + 6, -1, z0 + 5, Blocks.WATER.getDefaultState());
        set(x0 + 2, 0, z0 + 5, Blocks.HAY_BLOCK.getDefaultState());
        animal(EntityType.COW, x0 + 2, z0 + 2);
        animal(EntityType.COW, x0 + 4, z0 + 3);
        animal(EntityType.SHEEP, x0 + 6, z0 + 2);
        animal(EntityType.SHEEP, x0 + 5, z0 + 5);
        animal(EntityType.SHEEP, x0 + 3, z0 + 5);
        animal(EntityType.PIG, x0 + 2, z0 + 4);
        animal(EntityType.PIG, x0 + 6, z0 + 4);
        animal(EntityType.CHICKEN, x0 + 4, z0 + 1);
        animal(EntityType.CHICKEN, x0 + 7, z0 + 3);
        animal(EntityType.CHICKEN, x0 + 3, z0 + 3);
    }

    private void animal(EntityType<? extends MobEntity> type, int x, int z) {
        MobEntity mob = type.create(world);
        if (mob == null) return;
        BlockPos p = at(x, 0, z);
        mob.refreshPositionAndAngles(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, world.random.nextFloat() * 360f, 0f);
        mob.setPersistent();
        world.spawnEntity(mob);
    }

    // ==========================================
    // HẦM MỎ: cổng đá cuội, cầu thang xoắn quanh cột đá cuội 2x2 đi thẳng xuống Y -50 (vùng deepslate có kim cương),
    // đáy là phòng mỏ (rương, lò, bàn chế tạo) và ba hành lang đào nhánh
    // ==========================================

    /** Độ cao tuyệt đối của sàn phòng mỏ dưới đáy. */
    private static final int MINE_BOTTOM_Y = -50;
    /** Vòng xoắn: 12 ô quanh cột 2x2, theo chiều kim đồng hồ nhìn từ trên xuống, bắt đầu ngay sau cổng. */
    private static final int[][] SPIRAL = {{1, 0}, {2, 0}, {3, 0}, {3, 1}, {3, 2}, {3, 3}, {2, 3}, {1, 3}, {0, 3}, {0, 2}, {0, 1}, {0, 0}};

    private void mine(int x0, int z0) {
        // Mái che lối xuống: khung đá cuội 3 rộng phía bắc vòng xoắn
        for (int y = 0; y < 3; y++) {
            set(x0 - 1, y, z0, Blocks.COBBLESTONE.getDefaultState());
            set(x0 + 1, y, z0, Blocks.COBBLESTONE.getDefaultState());
        }
        for (int x = -1; x <= 1; x++) set(x0 + x, 3, z0, Blocks.COBBLESTONE_SLAB.getDefaultState());
        torchOnWall(x0 - 1, 2, z0 - 1, Direction.NORTH);
        set(x0, -1, z0, Blocks.COBBLESTONE.getDefaultState());

        // Góc tây bắc của khuôn 4x4: ô đầu tiên của vòng xoắn (1, 0) nằm ngay sau cổng
        int rx = x0 - 1, rz = z0 + 1;
        int bottom = MINE_BOTTOM_Y - o.getY();   // toạ độ tương đối của sàn đáy
        int i = 0;
        for (; -1 - i > bottom; i++) {
            int[] c = SPIRAL[i % SPIRAL.length];
            int x = rx + c[0], z = rz + c[1], y = -1 - i;
            set(x, y, z, Blocks.COBBLESTONE.getDefaultState());
            for (int k = 1; k <= 3; k++) carve(x, y + k, z);
            // Vách ngoài và cột giữa luôn kín (hang tự nhiên, nước, dung nham cắt ngang thì bịt bằng đá cuội)
            for (Direction d : Direction.Type.HORIZONTAL) {
                int nx = x + d.getOffsetX(), nz = z + d.getOffsetZ();
                boolean inRing = nx >= rx && nx <= rx + 3 && nz >= rz && nz <= rz + 3;
                boolean pillar = nx >= rx + 1 && nx <= rx + 2 && nz >= rz + 1 && nz <= rz + 2;
                if (inRing && !pillar) continue;
                for (int k = 1; k <= 3; k++) seal(nx, y + k, nz);
            }
            if (i % 6 == 3) {
                // Đuốc trên vách ngoài, quay vào trong
                Direction out = c[0] == 3 ? Direction.EAST : c[0] == 0 ? Direction.WEST : c[1] == 0 ? Direction.NORTH : Direction.SOUTH;
                torchOnWall(x, y + 2, z, out.getOpposite());
            }
        }

        // Phòng mỏ dưới đáy: 8x8, cao 3, bao quanh chân vòng xoắn
        int fy = bottom;
        for (int x = rx - 2; x <= rx + 5; x++) {
            for (int z = rz - 2; z <= rz + 5; z++) {
                set(x, fy, z, Blocks.COBBLESTONE.getDefaultState());
                for (int k = 1; k <= 3; k++) carve(x, fy + k, z);
                boolean edge = x == rx - 2 || x == rx + 5 || z == rz - 2 || z == rz + 5;
                if (edge) for (int k = 1; k <= 3; k++) { /* tường phòng giữ nguyên đá; chỉ bịt chất lỏng */ sealAround(x, fy + k, z); }
            }
        }
        set(rx - 1, fy + 1, rz + 4, Blocks.CRAFTING_TABLE.getDefaultState());
        set(rx - 1, fy + 1, rz + 3, Blocks.FURNACE.getDefaultState().with(FurnaceBlock.FACING, Direction.EAST));
        set(rx + 4, fy + 1, rz + 4, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, Direction.WEST));
        if (world.getBlockEntity(at(rx + 4, fy + 1, rz + 4)) instanceof ChestBlockEntity c) {
            ItemStack[] loot = {new ItemStack(Items.COBBLED_DEEPSLATE, 64), new ItemStack(Items.COBBLED_DEEPSLATE, 41),
                    new ItemStack(Items.RAW_IRON, 9), new ItemStack(Items.COAL, 12), new ItemStack(Items.REDSTONE, 23),
                    new ItemStack(Items.LAPIS_LAZULI, 8), new ItemStack(Items.DIAMOND, 1), new ItemStack(Items.TORCH, 17),
                    damaged(Items.IRON_PICKAXE, 31)};
            for (int k = 0; k < loot.length; k++) c.setStack(k * 3 % 27, loot[k]);
        }
        set(rx - 1, fy + 1, rz - 1, Blocks.TORCH.getDefaultState());
        set(rx + 4, fy + 1, rz - 1, Blocks.TORCH.getDefaultState());
        set(rx + 4, fy + 1, rz + 2, Blocks.TORCH.getDefaultState());

        // Ba hành lang đào nhánh 1x2 tỏa ra đông, tây, nam; đuốc mỗi 8 khối
        branch(rx + 6, fy, rz + 1, Direction.EAST, 26);
        branch(rx - 3, fy, rz + 2, Direction.WEST, 22);
        branch(rx + 2, fy, rz + 6, Direction.SOUTH, 30);
    }

    /** Hành lang 1x2 bắt đầu ở (x, z), sàn ở fy, đi {@code len} khối theo {@code dir}. */
    private void branch(int x, int fy, int z, Direction dir, int len) {
        Direction side = dir.rotateYClockwise();
        for (int i = 0; i < len; i++) {
            int px = x + dir.getOffsetX() * i, pz = z + dir.getOffsetZ() * i;
            if (!world.getBlockState(at(px, fy, pz)).isOpaqueFullCube(world, at(px, fy, pz))) set(px, fy, pz, Blocks.COBBLESTONE.getDefaultState());
            carve(px, fy + 1, pz);
            carve(px, fy + 2, pz);
            sealAround(px, fy + 1, pz);
            sealAround(px, fy + 2, pz);
            if (i % 8 == 4) {
                int wx = px + side.getOffsetX(), wz = pz + side.getOffsetZ();
                seal(wx, fy + 2, wz);
                torchOnWall(px, fy + 2, pz, side.getOpposite());
            }
        }
    }

    /** Đào thành không khí. */
    private void carve(int x, int y, int z) {
        set(x, y, z, Blocks.AIR.getDefaultState());
    }

    /** Ô này hổng hoặc là chất lỏng thì bịt bằng đá cuội (để hầm kín, không tràn nước / dung nham). */
    private void seal(int x, int y, int z) {
        BlockState s = world.getBlockState(at(x, y, z));
        if (s.isReplaceable() || !s.getFluidState().isEmpty()) set(x, y, z, Blocks.COBBLESTONE.getDefaultState());
    }

    /** Bịt chất lỏng ở bốn bên và trên đầu ô (x, y, z). */
    private void sealAround(int x, int y, int z) {
        for (Direction d : Direction.values()) {
            if (d == Direction.DOWN) continue;
            BlockPos n = at(x, y, z).offset(d);
            if (!world.getFluidState(n).isEmpty()) world.setBlockState(n, Blocks.COBBLESTONE.getDefaultState(), FLAGS);
        }
    }

    // ==========================================
    // GÓC PHÙ PHÉP: nền đá cuội 7x7, bàn phù phép giữa, kệ sách vòng ngoài (chưa đủ 15 cái), một cái đe
    // ==========================================

    private void enchanting(int x0, int z0) {
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) set(x0 + x, -1, z0 + z, Blocks.COBBLESTONE.getDefaultState());
        }
        set(x0 + 3, 0, z0 + 3, Blocks.ENCHANTING_TABLE.getDefaultState());
        int[][] shelves = {{1, 1}, {2, 1}, {3, 1}, {4, 1}, {5, 1}, {1, 2}, {1, 3}, {5, 2}, {5, 3}};
        for (int[] s : shelves) {
            set(x0 + s[0], 0, z0 + s[1], Blocks.BOOKSHELF.getDefaultState());
            if (s[1] == 1) set(x0 + s[0], 1, z0 + s[1], Blocks.BOOKSHELF.getDefaultState());
        }
        set(x0 + 5, 0, z0 + 5, Blocks.ANVIL.getDefaultState());
        set(x0 + 1, 0, z0 + 5, Blocks.TORCH.getDefaultState());
        set(x0 + 5, 2, z0 + 1, Blocks.TORCH.getDefaultState());
    }

    // ==========================================
    // CỔNG ĐỊA NGỤC: khung hắc diện thạch 4x5 đã thắp, vài khối đá địa ngục rơi vãi quanh chân
    // ==========================================

    private void portal(int x0, int z0) {
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 5; y++) {
                boolean frame = x == 0 || x == 3 || y == 0 || y == 4;
                if (frame) set(x0 + x, y - 1 + 1, z0, Blocks.OBSIDIAN.getDefaultState());
            }
        }
        for (int x = 1; x <= 2; x++) {
            for (int y = 1; y <= 3; y++) set(x0 + x, y, z0, Blocks.NETHER_PORTAL.getDefaultState().with(NetherPortalBlock.AXIS, Direction.Axis.X));
        }
        int[][] rack = {{-1, 1}, {4, -1}, {1, 2}, {3, 1}, {-1, -1}};
        for (int[] r : rack) set(x0 + r[0], -1, z0 + r[1], Blocks.NETHERRACK.getDefaultState());
        set(x0 + 4, 0, z0 - 1, Blocks.NETHERRACK.getDefaultState());
    }

    // ==========================================
    // ĐƯỜNG SỎI: từ cửa nhà ra phía spawn và tới từng khu, đuốc trên cọc rào
    // ==========================================

    private void paths() {
        line(19, 1, 19, -RAMP + 1);   // từ cửa nhà ra phía spawn
        line(0, 0, 34, 0);            // đường phía bắc, trước nhà, ruộng và chuồng
        line(0, 0, 0, 21);            // men phía tây ruộng xuống hầm mỏ
        line(0, 21, 4, 21);
        line(26, 0, 26, 14);          // giữa nhà và chuồng xuống đường phía nam
        line(0, 14, 34, 14);          // đường phía nam
        line(18, 14, 18, 19);         // tới góc phù phép
        line(31, 14, 31, 21);         // tới cổng địa ngục
        torchOnFence(1, 1);
        torchOnFence(25, 1);
        torchOnFence(27, 15);
        torchOnFence(17, 15);
        torchOnFence(1, 15);
    }

    /** Đường sỏi thẳng (ngang hoặc dọc) trên mặt đất, chỉ thay khối cỏ / đất (sỏi: năm 2012 chưa có khối lối mòn). */
    private void line(int x1, int z1, int x2, int z2) {
        int sx = Integer.signum(x2 - x1), sz = Integer.signum(z2 - z1);
        int x = x1, z = z1;
        while (true) {
            BlockPos g = at(x, -1, z);
            BlockState s = world.getBlockState(g);
            if (s.isOf(Blocks.GRASS_BLOCK) || s.isOf(Blocks.DIRT)) {
                world.setBlockState(g, Blocks.GRAVEL.getDefaultState(), FLAGS);
                if (!world.getBlockState(g.up()).isAir() && world.getBlockState(g.up()).isReplaceable()) world.setBlockState(g.up(), Blocks.AIR.getDefaultState(), FLAGS);
            }
            if (x == x2 && z == z2) break;
            if (x != x2) x += sx;
            else z += sz;
        }
    }

    private static NamBase fromNbt(NbtCompound nbt) {
        NamBase s = new NamBase();
        s.built = nbt.getBoolean("Built");
        if (nbt.contains("Origin")) s.origin = BlockPos.fromLong(nbt.getLong("Origin"));
        return s;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putBoolean("Built", built);
        if (origin != null) nbt.putLong("Origin", origin.asLong());
        return nbt;
    }
}
