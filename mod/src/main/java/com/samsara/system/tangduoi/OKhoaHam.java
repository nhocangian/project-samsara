package com.samsara.system.tangduoi;

import com.samsara.block.BanhKhoaBlock;
import com.samsara.block.WombCodeBlock;
import com.samsara.registry.SamsaraBlocks;
import com.samsara.registry.SamsaraSounds;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ổ khoá bốn bánh ở tường bắc căn hầm: mã 0518 (tháng 5/2018). Đúng thì mã nhị phân ăn mòn dần mảng tường từ các bánh số
 * lan ra, chớp dồn, tắt sạch một nhịp, rồi đèn trong ống bật lần lượt chạy vào sâu; vài vết mã sót lại quanh lỗ.
 */
public final class OKhoaHam {
    private OKhoaHam() {}

    public static final int CODE = 518;
    /** Gói S2C (int): số tick màn đen lúc mất điện. */
    public static final Identifier BLACKOUT_PACKET = new Identifier("samsara", "tang_duoi_blackout");
    private static final int FAMILIAR_WRONG = 1998;
    private static final int HOLE_MIN_X = -2;
    private static final int HOLE_MAX_X = 2;
    private static final int HOLE_MAX_Y = 4;

    private static final int RUMBLE_TICKS = 20;
    private static final int CORRODE_STEP = 2;
    private static final int HOLD_TICKS = 12;
    /** Các nhịp chớp của cả mảng mã, nhanh dần (tick tính từ đầu cao trào). */
    private static final int[] SYNC_BLINKS = {4, 8, 11, 14, 16, 18, 19, 20};
    private static final int FLASH_START = 21;
    private static final int BLACKOUT = 24;
    private static final int BLACK_TICKS = 24;
    private static final int IGNITE_START = BLACKOUT + BLACK_TICKS;
    private static final int IGNITE_STEP = 7;
    /** Vài ô mã giữ lại ở rìa trên của lỗ, để lỗ không vuông vức. */
    private static final int[][] KEPT_EDGE = {{-2, 4}, {2, 4}, {2, 3}};
    /** Vết mã còn lại trên tường quanh lỗ sau khi mở (tương đối so với góc tầng). */
    private static final int[][] SCARS = {{-3, 1}, {-3, 3}, {3, 3}, {3, 4}, {0, 5}, {-1, 5}};

    private static int wrong = 0;
    private static int animTick = -1;
    private static int resetIn = 0;
    private static List<BlockPos> order = List.of();
    private static int climaxTick = -1;
    private static final List<Lamp> LAMP_CHAIN = new ArrayList<>();
    private static final List<Lamp> SIDE_LAMPS = new ArrayList<>();
    private static final Map<BlockPos, BlockState> CELLAR = new HashMap<>();
    private static final BlockPos LANTERN = at(4, 1, 43);

    private record Lamp(BlockPos pos, BlockState state) {}

    private static BlockPos at(int x, int y, int z) {
        return DuongOngBuilder.ORIGIN.add(x, y, z);
    }

    public static BlockPos dial(int i) {
        return at(-2 + i, 2, 41);
    }

    public static BlockPos button() {
        return at(3, 2, 42);
    }

    public static BlockPos sign() {
        return at(-4, 3, 42);
    }

    public static void build(ServerWorld world) {
        wrong = 0;
        animTick = -1;
        resetIn = 0;
        climaxTick = -1;
        SIDE_LAMPS.clear();
        LAMP_CHAIN.clear();
        CELLAR.clear();
        for (int i = 0; i < 4; i++) world.setBlockState(dial(i), SamsaraBlocks.BANH_KHOA.getDefaultState(), 2 | 16);
        world.setBlockState(button(), Blocks.STONE_BUTTON.getDefaultState()
                .with(net.minecraft.block.WallMountedBlock.FACE, net.minecraft.block.enums.WallMountLocation.WALL)
                .with(net.minecraft.block.HorizontalFacingBlock.FACING, Direction.SOUTH), 2 | 16);
        DuongOngBuilder.placeSign(world, sign(), Direction.SOUTH);
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!world.isClient && world instanceof ServerWorld sw && hit.getBlockPos().equals(button())
                    && sw.getRegistryKey() == com.samsara.registry.SamsaraDimensions.TANG_DUOI_WORLD_KEY
                    && player instanceof ServerPlayerEntity sp) {
                submit(sw, sp);
            }
            return ActionResult.PASS;
        });
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getRegistryKey() == com.samsara.registry.SamsaraDimensions.TANG_DUOI_WORLD_KEY) tick(world);
        });
    }

    /** Lệnh test: đặt bốn bánh rồi xác nhận. */
    public static void forceCode(ServerWorld world, ServerPlayerEntity player, int code) {
        int c = code;
        for (int i = 3; i >= 0; i--) {
            BlockState s = world.getBlockState(dial(i));
            if (s.getBlock() instanceof BanhKhoaBlock) world.setBlockState(dial(i), s.with(BanhKhoaBlock.DIGIT, c % 10), 2 | 16);
            c /= 10;
        }
        submit(world, player);
    }

    private static int read(ServerWorld world) {
        int code = 0;
        for (int i = 0; i < 4; i++) {
            BlockState s = world.getBlockState(dial(i));
            if (!(s.getBlock() instanceof BanhKhoaBlock)) return -1;
            code = code * 10 + s.get(BanhKhoaBlock.DIGIT);
        }
        return code;
    }

    private static void submit(ServerWorld world, ServerPlayerEntity player) {
        if (animTick >= 0 || resetIn > 0) return;
        int code = read(world);
        if (code < 0) return;
        if (code == CODE) {
            animTick = 0;
            prepareOrder(world.getRandom());
            world.playSound(null, dial(1), SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 0.5f, 0.45f);
            return;
        }
        wrong++;
        resetIn = 8;
        world.playSound(null, dial(1).add(0, 0, 8), SoundEvents.BLOCK_STONE_FALL, SoundCategory.BLOCKS, 1.2f, 0.5f);
        if (code == FAMILIAR_WRONG) {
            DuongOngBuilder.placeSign(world, sign(), Direction.SOUTH, null, "samsara.tang_duoi.khong_phai_anh");
            world.playSound(null, dial(1), SoundEvents.BLOCK_DEEPSLATE_BREAK, SoundCategory.BLOCKS, 0.9f, 0.5f);
        }
        if (wrong == 5) player.sendMessage(Text.translatable("samsara.tang_duoi.khac_khi_con_nho"), true);
        if (wrong >= 8) {
            for (int i = 0; i < 4; i++) {
                BlockState s = world.getBlockState(dial(i));
                if (s.getBlock() instanceof BanhKhoaBlock) world.setBlockState(dial(i), s.with(BanhKhoaBlock.HINT, true), 2 | 16);
            }
        }
    }

    private static void tick(ServerWorld world) {
        if (resetIn > 0 && --resetIn == 0) {
            for (int i = 0; i < 4; i++) {
                BlockState s = world.getBlockState(dial(i));
                if (s.getBlock() instanceof BanhKhoaBlock) world.setBlockState(dial(i), s.with(BanhKhoaBlock.DIGIT, 0), 2 | 16);
            }
        }
        if (animTick < 0) return;
        animTick++;

        if (animTick == 2) world.playSound(null, dial(1).north(4), SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.0f, 0.4f);

        int corrodeEnd = RUMBLE_TICKS + order.size() * CORRODE_STEP;
        if (animTick > RUMBLE_TICKS && animTick <= corrodeEnd && (animTick - RUMBLE_TICKS) % CORRODE_STEP == 0) {
            corrode(world, order.get((animTick - RUMBLE_TICKS) / CORRODE_STEP - 1));
        }
        if (animTick == corrodeEnd + HOLD_TICKS) startClimax(world);
        if (climaxTick >= 0) tickClimax(world);
    }

    /** Thứ tự ăn mòn: từ các bánh số lan ra, lệch ngẫu nhiên để viền ăn mòn lởm chởm. */
    private static void prepareOrder(Random random) {
        List<double[]> scored = new ArrayList<>();
        List<BlockPos> cells = new ArrayList<>();
        for (int x = HOLE_MIN_X; x <= HOLE_MAX_X; x++) {
            for (int y = 1; y <= HOLE_MAX_Y; y++) {
                cells.add(at(x, y, 41));
                scored.add(new double[]{Math.hypot(x + 0.5, y - 2) + random.nextDouble() * 1.4});
            }
        }
        List<Integer> index = new ArrayList<>();
        for (int i = 0; i < cells.size(); i++) index.add(i);
        index.sort(Comparator.comparingDouble(i -> scored.get(i)[0]));
        List<BlockPos> result = new ArrayList<>();
        for (int i : index) result.add(cells.get(i));
        order = result;
    }

    private static void placeCode(ServerWorld world, BlockPos pos) {
        world.setBlockState(pos, WombCodeBlock.at(SamsaraBlocks.WOMB_CODE.getDefaultState(), pos), 2 | 16);
        for (Direction d : Direction.values()) WombCodeBlock.refresh(world, pos.offset(d));
        WombCodeBlock.refresh(world, pos);
    }

    private static void corrode(ServerWorld world, BlockPos pos) {
        placeCode(world, pos);
        world.playSound(null, pos, SamsaraSounds.DEN_CHOP_TICK, SoundCategory.BLOCKS, 0.7f, 0.5f + world.random.nextFloat() * 0.5f);
        // Một ít vết mã lan ra tường xung quanh, ở lại sau khi cửa đã mở
        int i = order.indexOf(pos);
        if (i % 3 == 2 && i / 3 < SCARS.length) {
            int[] s = SCARS[i / 3];
            placeCode(world, at(s[0], s[1], 41));
        }
    }

    /** Bắt đầu cao trào: đèn trong ống tắt sẵn (người chơi chưa thấy), cả mảng mã chớp dồn. */
    private static void startClimax(ServerWorld world) {
        climaxTick = 0;
        LAMP_CHAIN.clear();
        CELLAR.clear();
        List<BlockPos> lamps = new ArrayList<>();
        for (int z = 40; z >= -64; z--) lamps.add(at(0, 6, z));
        lamps.add(at(0, 8, 0));
        lamps.add(at(0, 8, -70));
        lamps.sort(Comparator.comparingInt((BlockPos b) -> b.getZ()).reversed());
        BlockState off = SamsaraBlocks.GACH_MEN_O.getDefaultState();
        for (BlockPos l : lamps) {
            BlockState st = world.getBlockState(l);
            if (!st.isOf(SamsaraBlocks.DEN_HUYNH_QUANG) && !st.isOf(SamsaraBlocks.DEN_CHOP)) continue;
            LAMP_CHAIN.add(new Lamp(l, st));
            DuongOngBuilder.RELIGHT.remove(l);
            world.setBlockState(l, off, 3);
        }
        SIDE_LAMPS.clear();
        for (int x = -32; x <= 32; x++) {
            if (x == 0) continue;
            BlockPos l = at(x, 6, 0);
            BlockState st = world.getBlockState(l);
            if (!st.isOf(SamsaraBlocks.DEN_HUYNH_QUANG) && !st.isOf(SamsaraBlocks.DEN_CHOP)) continue;
            SIDE_LAMPS.add(new Lamp(l, st));
            DuongOngBuilder.RELIGHT.remove(l);
            world.setBlockState(l, off, 3);
        }
        world.playSound(null, dial(1), SamsaraSounds.SYSTEM_STATIC, SoundCategory.BLOCKS, 1.0f, 0.6f);
    }

    private static boolean isKeptEdge(int x, int y) {
        for (int[] k : KEPT_EDGE) {
            if (k[0] == x && k[1] == y) return true;
        }
        return false;
    }

    private static void setHole(ServerWorld world, boolean code) {
        BlockState c = SamsaraBlocks.WOMB_CODE.getDefaultState();
        for (int x = HOLE_MIN_X; x <= HOLE_MAX_X; x++) {
            for (int y = 1; y <= HOLE_MAX_Y; y++) {
                if (isKeptEdge(x, y)) continue;
                BlockPos pos = at(x, y, 41);
                world.setBlockState(pos, code ? WombCodeBlock.at(c, pos) : Blocks.AIR.getDefaultState(), 2 | 16);
            }
        }
    }

    private static void tickClimax(ServerWorld world) {
        climaxTick++;
        // Chớp dồn, nhanh dần
        for (int t : SYNC_BLINKS) {
            if (climaxTick == t) setHole(world, (java.util.Arrays.binarySearch(SYNC_BLINKS, t) & 1) == 1);
        }
        if (climaxTick == FLASH_START) flashCellar(world, true);
        if (climaxTick == BLACKOUT) {
            flashCellar(world, false);
            setHole(world, false);
            world.setBlockState(LANTERN, Blocks.AIR.getDefaultState(), 3);
            // Tắt sạch: mắt tối hẳn, mọi âm thanh của đoạn trước dừng lại
            for (ServerPlayerEntity p : world.getPlayers()) {
                if (p.squaredDistanceTo(LANTERN.getX(), LANTERN.getY(), LANTERN.getZ()) < 400.0) {
                    PacketByteBuf buf = PacketByteBufs.create();
                    buf.writeInt(BLACK_TICKS);
                    ServerPlayNetworking.send(p, BLACKOUT_PACKET, buf);
                    p.networkHandler.sendPacket(new StopSoundS2CPacket(null, SoundCategory.BLOCKS));
                }
            }
        }
        int ignite = climaxTick - IGNITE_START;
        if (ignite >= 0 && ignite % IGNITE_STEP == 0) {
            int i = ignite / IGNITE_STEP;
            if (i < LAMP_CHAIN.size()) {
                Lamp l = LAMP_CHAIN.get(i);
                world.setBlockState(l.pos, l.state, 3);
                if (l.state.isOf(SamsaraBlocks.DEN_CHOP)) world.scheduleBlockTick(l.pos, l.state.getBlock(), 40);
                world.playSound(null, l.pos, SamsaraSounds.DEN_CHOP_TICK, SoundCategory.BLOCKS, 2.5f, 0.85f);
            } else {
                world.setBlockState(LANTERN, Blocks.SOUL_LANTERN.getDefaultState(), 3);
                for (Lamp l : SIDE_LAMPS) {
                    world.setBlockState(l.pos, l.state, 3);
                    if (l.state.isOf(SamsaraBlocks.DEN_CHOP)) world.scheduleBlockTick(l.pos, l.state.getBlock(), 40);
                }
                for (int x = HOLE_MIN_X - 1; x <= HOLE_MAX_X + 1; x++) {
                    for (int y = 0; y <= HOLE_MAX_Y + 1; y++) WombCodeBlock.refresh(world, at(x, y, 41));
                }
                climaxTick = -1;
                animTick = -1;
            }
        }
    }

    /** Một cái nháy rất ngắn: cả tường hầm thành mã rồi trở lại. */
    private static void flashCellar(ServerWorld world, boolean on) {
        if (on) {
            BlockState c = SamsaraBlocks.WOMB_CODE.getDefaultState();
            for (int x = -6; x <= 6; x++) {
                for (int z = 41; z <= 50; z++) {
                    for (int y = 0; y <= 6; y++) {
                        BlockPos pos = at(x, y, z);
                        BlockState st = world.getBlockState(pos);
                        if (st.isOf(Blocks.STONE_BRICKS) || st.isOf(Blocks.MOSSY_STONE_BRICKS)) {
                            CELLAR.put(pos, st);
                            world.setBlockState(pos, WombCodeBlock.at(c, pos), 2 | 16);
                        }
                    }
                }
            }
        } else {
            CELLAR.forEach((pos, st) -> world.setBlockState(pos, st, 2 | 16));
            CELLAR.clear();
        }
    }
}
