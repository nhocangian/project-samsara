package com.samsara.system.overworld;

import com.samsara.system.cycle.EggCycleState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationPropertyHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Biển do chính mình đặt (Overworld, từ Stage 1): thỉnh thoảng giữa đồng có một tấm biển gỗ trơ trọi, quay mặt về
 * phía người chơi. Dòng đầu là tên người chơi; dòng dưới hoặc là {@code "đã tới đây"}, hoặc là ngày giờ thật của lần
 * chơi trước (lúc người chơi thoát game lần gần nhất). Biển được cắm sau lưng người chơi, ngoài tầm nhìn, nên chỉ
 * bắt gặp khi quay lại. Biển ở lại đó luôn.
 *
 * <p>Đi tiếp ý "dấu vết người cũ" ({@link NamTraces}): từ "có ai đó từng ở đây" sang "người đó là mình".
 * Mỗi người chơi chừng 20-40 phút chơi một tấm, lần đầu sau 10 phút; cả thế giới tối đa {@link #MAX_SIGNS} tấm.
 */
public final class SelfSign extends PersistentState {
    private static final String KEY = "samsara_self_sign";
    private static final int MAX_SIGNS = 6;
    private static final int FIRST_DELAY = 12000;
    private static final int MIN_GAP = 24000;
    private static final int EXTRA_GAP = 24000;

    /** Lúc thoát game gần nhất của mỗi người chơi (giờ thật, ms). */
    private final Map<UUID, Long> lastLogout = new HashMap<>();
    /** Lúc thoát game của lần trước nữa: giờ "lần chơi trước" khi đang chơi lần này. */
    private final Map<UUID, Long> previousSession = new HashMap<>();
    private int placed;

    private static final Map<UUID, Long> NEXT = new HashMap<>();

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            SelfSign s = get(server);
            Long last = s.lastLogout.get(handler.player.getUuid());
            if (last != null) {
                s.previousSession.put(handler.player.getUuid(), last);
                s.markDirty();
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            SelfSign s = get(server);
            s.lastLogout.put(handler.player.getUuid(), System.currentTimeMillis());
            s.markDirty();
            NEXT.remove(handler.player.getUuid());
        });
        ServerTickEvents.END_SERVER_TICK.register(SelfSign::tick);
    }

    private static SelfSign get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(SelfSign::fromNbt, SelfSign::new, KEY);
    }

    private static void tick(MinecraftServer server) {
        if (server.getTicks() % 100 != 0) return;
        if (EggCycleState.get(server).getStage() < 1) return;
        SelfSign state = get(server);
        if (state.placed >= MAX_SIGNS) return;
        ServerWorld world = server.getWorld(World.OVERWORLD);
        if (world == null) return;
        long now = server.getTicks();
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isSpectator()) continue;
            long next = NEXT.computeIfAbsent(player.getUuid(), id -> now + FIRST_DELAY);
            if (now < next) continue;
            if (placeBehind(world, player, state)) {
                NEXT.put(player.getUuid(), now + MIN_GAP + world.random.nextInt(EXTRA_GAP));
            }
        }
    }

    /** Cắm một tấm biển cách 24-40 khối sau lưng người chơi, trên đất cỏ trống. */
    private static boolean placeBehind(ServerWorld world, ServerPlayerEntity player, SelfSign state) {
        Vec3d look = player.getRotationVec(1.0f).multiply(1, 0, 1);
        if (look.lengthSquared() < 1.0e-4) return false;
        look = look.normalize();
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = Math.PI + (world.random.nextDouble() - 0.5) * Math.toRadians(100);
            double dist = 24 + world.random.nextDouble() * 16;
            Vec3d dir = new Vec3d(look.x * Math.cos(angle) - look.z * Math.sin(angle), 0, look.x * Math.sin(angle) + look.z * Math.cos(angle));
            BlockPos at = groundAt(world, MathHelper.floor(player.getX() + dir.x * dist), MathHelper.floor(player.getZ() + dir.z * dist));
            if (at == null) continue;
            place(world, at, player, state);
            return true;
        }
        return false;
    }

    private static BlockPos groundAt(ServerWorld world, int x, int z) {
        if (!world.isChunkLoaded(x >> 4, z >> 4)) return null;
        BlockPos top = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        BlockState ground = world.getBlockState(top.down());
        if (!ground.isIn(BlockTags.DIRT) || !world.getFluidState(top).isEmpty()) return null;
        BlockState here = world.getBlockState(top);
        if (!here.isAir() && !here.isReplaceable()) return null;
        if (!world.isAir(top.up())) return null;
        return top;
    }

    private static void place(ServerWorld world, BlockPos at, ServerPlayerEntity player, SelfSign state) {
        double dx = player.getX() - (at.getX() + 0.5), dz = player.getZ() - (at.getZ() + 0.5);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        // Mặt chữ quay về phía người chơi (vanilla đặt biển với fromYaw(hướng nhìn của người đặt + 180),
        // tức đúng hướng từ biển nhìn về người đặt)
        int rotation = RotationPropertyHelper.fromYaw(yaw);
        world.setBlockState(at, Blocks.OAK_SIGN.getDefaultState().with(SignBlock.ROTATION, rotation), 3);
        if (world.getBlockEntity(at) instanceof SignBlockEntity sign) {
            Text name = Text.literal(player.getGameProfile().getName());
            Long prev = state.previousSession.get(player.getUuid());
            boolean showDate = prev != null && world.random.nextBoolean();
            Text second = showDate ? Text.literal(new SimpleDateFormat("dd/MM  HH:mm").format(new Date(prev)))
                    : Text.translatable("samsara.self_sign.da_toi_day");
            SignText text = sign.getFrontText().withMessage(0, name).withMessage(1, second);
            sign.setText(text, true);
            sign.setWaxed(true);
            sign.markDirty();
        }
        state.placed++;
        state.markDirty();
    }

    /** Lệnh test: cắm ngay một tấm biển 6 khối trước mặt người chơi (bỏ qua stage, giới hạn và thời gian chờ). */
    public static boolean testNow(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        Vec3d look = player.getRotationVec(1.0f).multiply(1, 0, 1).normalize();
        BlockPos at = groundAt(world, MathHelper.floor(player.getX() + look.x * 6), MathHelper.floor(player.getZ() + look.z * 6));
        if (at == null) return false;
        place(world, at, player, get(world.getServer()));
        return true;
    }

    private static SelfSign fromNbt(NbtCompound nbt) {
        SelfSign s = new SelfSign();
        s.placed = nbt.getInt("Placed");
        NbtCompound last = nbt.getCompound("LastLogout");
        for (String k : last.getKeys()) s.lastLogout.put(UUID.fromString(k), last.getLong(k));
        NbtCompound prev = nbt.getCompound("PreviousSession");
        for (String k : prev.getKeys()) s.previousSession.put(UUID.fromString(k), prev.getLong(k));
        return s;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putInt("Placed", placed);
        NbtCompound last = new NbtCompound();
        lastLogout.forEach((k, v) -> last.putLong(k.toString(), v));
        nbt.put("LastLogout", last);
        NbtCompound prev = new NbtCompound();
        previousSession.forEach((k, v) -> prev.putLong(k.toString(), v));
        nbt.put("PreviousSession", prev);
        return nbt;
    }
}
